package com.corwin.bootstrap.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.system.audit.published.Audit;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.resource.domain.model.ApiAccessType;
import com.corwin.system.resource.domain.model.ApiMethod;
import com.corwin.system.resource.domain.model.ApiProtocol;
import com.corwin.system.resource.domain.model.PermissionUserScope;
import com.corwin.system.resource.published.ApiMeta;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/5/5
 */
@Component
public class BootstrapControllerApiScanner {

    private static final List<String> BASE_PACKAGES = new ArrayList<>();

    static {
        BASE_PACKAGES.add("com.corwin.system");
        BASE_PACKAGES.add("com.corwin.datasource");
        BASE_PACKAGES.add("com.corwin.storage");
        BASE_PACKAGES.add("com.corwin.jsonfmt");
        BASE_PACKAGES.add("com.corwin.schemaforge");
        BASE_PACKAGES.add("com.corwin.reminder");
        BASE_PACKAGES.add("com.corwin.web");

    }

    public ScanResult scan() {
        List<Class<?>> controllers = scanControllerClasses();
        List<ApiSeed> apis = new ArrayList<>();
        for (Class<?> controllerClass : controllers) {
            apis.addAll(parseController(controllerClass));
        }
        apis.sort(Comparator.comparing(ApiSeed::module).thenComparing(api -> api.httpMethod().name())
                .thenComparing(ApiSeed::pathPattern).thenComparing(ApiSeed::handlerClass)
                .thenComparing(ApiSeed::handlerMethod));
        validateNoDuplicateApis(apis);
        List<PermissionSeed> permissions = collectPermissions(apis);
        return new ScanResult(List.copyOf(apis), permissions);
    }

    private List<Class<?>> scanControllerClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<Class<?>> result = new ArrayList<>();
        for (String basePackage : BASE_PACKAGES) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                String className = candidate.getBeanClassName();
                if (className == null || className.isBlank()) {
                    continue;
                }
                try {
                    result.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Load controller class failed: " + className, e);
                }
            }
        }
        result.sort(Comparator.comparing(Class::getName));
        return result;
    }

    private List<ApiSeed> parseController(Class<?> controllerClass) {
        ApiMeta apiMeta = AnnotatedElementUtils.findMergedAnnotation(controllerClass, ApiMeta.class);
        if (apiMeta == null) {
            throw new IllegalStateException("@ApiMeta missing on controller: " + controllerClass.getName());
        }

        List<String> typePaths = resolveTypePaths(controllerClass);
        SecurityMeta typeSecurity = resolveTypeSecurity(controllerClass);
        Audit typeAudit = AnnotatedElementUtils.findMergedAnnotation(controllerClass, Audit.class);
        String module = apiMeta.module().code();

        List<ApiSeed> result = new ArrayList<>();
        List<Method> methods = Arrays.stream(controllerClass.getDeclaredMethods()).filter(this::isHandlerMethod)
                .sorted(Comparator.comparing(Method::getName)
                        .thenComparing(method -> Arrays.toString(method.getParameterTypes()))).toList();
        for (Method method : methods) {
            RequestMeta requestMeta = parseRequestMeta(method);
            if (requestMeta == null) {
                continue;
            }
            SecurityMeta securityMeta = resolveSecurity(method, controllerClass, typeSecurity);
            Audit audit = AnnotatedElementUtils.findMergedAnnotation(method, Audit.class);
            AuditMeta auditMeta = audit == null ? AuditMeta.none() : AuditMeta.of(audit, controllerClass, method);
            if (audit == null && typeAudit != null) {
                auditMeta = AuditMeta.of(typeAudit, controllerClass, method);
            }

            for (String typePath : typePaths) {
                for (String methodPath : requestMeta.paths()) {
                    String fullPath = combinePath(typePath, methodPath);
                    for (ApiMethod httpMethod : requestMeta.httpMethods()) {
                        result.add(
                                new ApiSeed(module, ApiProtocol.HTTP, httpMethod, fullPath, controllerClass.getName(),
                                        method.getName(), securityMeta.permissionDeclared(), securityMeta.accessType(),
                                        securityMeta.userTypesJson(), securityMeta.permissionCodes(),
                                        auditMeta.declared(), auditMeta.resource(), auditMeta.action(),
                                        auditMeta.description()));
                    }
                }
            }
        }
        return result;
    }

    private boolean isHandlerMethod(Method method) {
        int modifiers = method.getModifiers();
        return Modifier.isPublic(modifiers) && !method.isSynthetic() && !method.isBridge();
    }

    private void validateNoDuplicateApis(List<ApiSeed> apis) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ApiSeed api : apis) {
            String uniqueKey = api.module() + "|" + api.protocol().name() + "|" + api.httpMethod()
                    .name() + "|" + api.pathPattern();
            if (!seen.add(uniqueKey)) {
                throw new IllegalStateException("Duplicate api mapping found: " + uniqueKey);
            }
        }
    }

    private List<String> resolveTypePaths(Class<?> controllerClass) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(controllerClass,
                RequestMapping.class);
        if (requestMapping == null) {
            return List.of("");
        }
        return resolvePaths(requestMapping.value(), requestMapping.path());
    }

    private RequestMeta parseRequestMeta(Method method) {
        GetMapping getMapping = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class);
        if (getMapping != null) {
            return new RequestMeta(resolvePaths(getMapping.value(), getMapping.path()), List.of(ApiMethod.GET));
        }
        PostMapping postMapping = AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class);
        if (postMapping != null) {
            return new RequestMeta(resolvePaths(postMapping.value(), postMapping.path()), List.of(ApiMethod.POST));
        }
        PutMapping putMapping = AnnotatedElementUtils.findMergedAnnotation(method, PutMapping.class);
        if (putMapping != null) {
            return new RequestMeta(resolvePaths(putMapping.value(), putMapping.path()), List.of(ApiMethod.PUT));
        }
        DeleteMapping deleteMapping = AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class);
        if (deleteMapping != null) {
            return new RequestMeta(resolvePaths(deleteMapping.value(), deleteMapping.path()),
                    List.of(ApiMethod.DELETE));
        }
        PatchMapping patchMapping = AnnotatedElementUtils.findMergedAnnotation(method, PatchMapping.class);
        if (patchMapping != null) {
            return new RequestMeta(resolvePaths(patchMapping.value(), patchMapping.path()), List.of(ApiMethod.PATCH));
        }

        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (requestMapping == null) {
            return null;
        }
        return new RequestMeta(resolvePaths(requestMapping.value(), requestMapping.path()),
                resolveHttpMethods(requestMapping.method()));
    }

    private List<String> resolvePaths(String[] values, String[] paths) {
        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        if (values != null) {
            Arrays.stream(values).map(this::normalizeSegment).forEach(resolved::add);
        }
        if (paths != null) {
            Arrays.stream(paths).map(this::normalizeSegment).forEach(resolved::add);
        }
        if (resolved.isEmpty()) {
            resolved.add("");
        }
        return List.copyOf(resolved);
    }

    private List<ApiMethod> resolveHttpMethods(RequestMethod[] methods) {
        if (methods == null || methods.length == 0) {
            return List.of(ApiMethod.ANY);
        }
        LinkedHashSet<ApiMethod> result = new LinkedHashSet<>();
        for (RequestMethod method : methods) {
            if (method == null) {
                continue;
            }
            result.add(switch (method) {
                case GET -> ApiMethod.GET;
                case POST -> ApiMethod.POST;
                case PUT -> ApiMethod.PUT;
                case DELETE -> ApiMethod.DELETE;
                case PATCH -> ApiMethod.PATCH;
                case OPTIONS -> ApiMethod.OPTIONS;
                case HEAD -> ApiMethod.HEAD;
                default -> ApiMethod.ANY;
            });
        }
        return List.copyOf(result);
    }

    private SecurityMeta resolveTypeSecurity(Class<?> controllerClass) {
        if (AnnotatedElementUtils.findMergedAnnotation(controllerClass, PermitAll.class) != null) {
            return SecurityMeta.permitAll();
        }
        Authorize authorize = AnnotatedElementUtils.findMergedAnnotation(controllerClass, Authorize.class);
        if (authorize != null) {
            return SecurityMeta.authorize(authorize);
        }
        Authenticated authenticated = AnnotatedElementUtils.findMergedAnnotation(controllerClass, Authenticated.class);
        if (authenticated != null) {
            return SecurityMeta.authenticated(authenticated);
        }
        return null;
    }

    private SecurityMeta resolveSecurity(Method method, Class<?> controllerClass, SecurityMeta typeSecurity) {
        if (AnnotatedElementUtils.findMergedAnnotation(method, PermitAll.class) != null) {
            return SecurityMeta.permitAll();
        }
        Authorize authorize = AnnotatedElementUtils.findMergedAnnotation(method, Authorize.class);
        if (authorize != null) {
            return SecurityMeta.authorize(authorize);
        }
        Authenticated authenticated = AnnotatedElementUtils.findMergedAnnotation(method, Authenticated.class);
        if (authenticated != null) {
            return SecurityMeta.authenticated(authenticated);
        }
        if (typeSecurity != null) {
            return typeSecurity;
        }
        throw new IllegalStateException(
                "Security declaration missing for handler: " + controllerClass.getName() + "#" + method.getName());
    }

    private List<PermissionSeed> collectPermissions(List<ApiSeed> apis) {
        LinkedHashMap<String, PermissionSeed> result = new LinkedHashMap<>();
        for (ApiSeed api : apis) {
            if (!api.permissionDeclared()) {
                continue;
            }
            PermissionUserScope userScope = resolveUserScope(api.userTypesJson());
            for (String permissionCode : api.permissionCodes()) {
                result.merge(permissionCode,
                        new PermissionSeed(permissionCode, permissionCode, userScope, "Generated from @Authorize"),
                        this::mergePermission);
            }
        }
        return result.values().stream().sorted(Comparator.comparing(PermissionSeed::code)).toList();
    }

    private PermissionSeed mergePermission(PermissionSeed left, PermissionSeed right) {
        PermissionUserScope mergedScope = mergeScope(left.userScope(), right.userScope());
        return new PermissionSeed(left.code(), left.name(), mergedScope, left.description());
    }

    private PermissionUserScope mergeScope(PermissionUserScope left, PermissionUserScope right) {
        if (left == right) {
            return left;
        }
        if (left == PermissionUserScope.COMMON || right == PermissionUserScope.COMMON) {
            return PermissionUserScope.COMMON;
        }
        return PermissionUserScope.COMMON;
    }

    private PermissionUserScope resolveUserScope(String userTypesJson) {
        if (userTypesJson == null || userTypesJson.isBlank() || "[]".equals(userTypesJson)) {
            return PermissionUserScope.COMMON;
        }
        boolean internal = userTypesJson.contains("\"INTERNAL\"");
        boolean external = userTypesJson.contains("\"EXTERNAL\"");
        if (internal && external) {
            return PermissionUserScope.COMMON;
        }
        if (internal) {
            return PermissionUserScope.INTERNAL;
        }
        if (external) {
            return PermissionUserScope.EXTERNAL;
        }
        return PermissionUserScope.COMMON;
    }

    private String normalizeSegment(String value) {
        if (value == null || value.isBlank() || "/".equals(value.trim())) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String combinePath(String typePath, String methodPath) {
        String combined = normalizeSegment(typePath) + normalizeSegment(methodPath);
        if (combined.isBlank()) {
            return "/";
        }
        if (!combined.startsWith("/")) {
            combined = "/" + combined;
        }
        while (combined.contains("//")) {
            combined = combined.replace("//", "/");
        }
        return combined;
    }

    public record ScanResult(
            List<ApiSeed> apis,
            List<PermissionSeed> permissions
    ) {
    }

    public record ApiSeed(
            String module,
            ApiProtocol protocol,
            ApiMethod httpMethod,
            String pathPattern,
            String handlerClass,
            String handlerMethod,
            boolean permissionDeclared,
            ApiAccessType accessType,
            String userTypesJson,
            List<String> permissionCodes,
            boolean auditDeclared,
            String auditResource,
            String auditAction,
            String auditDescription
    ) {
    }

    public record PermissionSeed(
            String code,
            String name,
            PermissionUserScope userScope,
            String description
    ) {
    }

    private record RequestMeta(
            List<String> paths,
            List<ApiMethod> httpMethods
    ) {
    }

    private record SecurityMeta(
            boolean permissionDeclared,
            ApiAccessType accessType,
            String userTypesJson,
            List<String> permissionCodes
    ) {
        static SecurityMeta permitAll() {
            return new SecurityMeta(false, ApiAccessType.PERMIT_ALL, "[]", List.of());
        }

        static SecurityMeta authenticated(Authenticated authenticated) {
            return new SecurityMeta(false, ApiAccessType.AUTHENTICATED, userTypesJson(authenticated.userTypes()),
                    List.of());
        }

        static SecurityMeta authorize(Authorize authorize) {
            List<String> permissionCodes = Arrays.stream(authorize.permissions()).map(String::trim)
                    .filter(code -> !code.isBlank()).distinct().toList();
            return new SecurityMeta(true, ApiAccessType.AUTHORIZED, userTypesJson(authorize.userTypes()),
                    permissionCodes);
        }

        private static String userTypesJson(UserType[] userTypes) {
            if (userTypes == null || userTypes.length == 0) {
                return "[]";
            }
            return Arrays.stream(userTypes).map(UserType::name).distinct().map(type -> "\"" + type + "\"")
                    .collect(Collectors.joining(",", "[", "]"));
        }
    }

    private record AuditMeta(
            boolean declared,
            String resource,
            String action,
            String description
    ) {
        static AuditMeta none() {
            return new AuditMeta(false, null, null, null);
        }

        static AuditMeta of(Audit audit, Class<?> controllerClass, Method method) {
            String resource = audit.resource().itemCode();
            String action = audit.action().itemCode();
            String description = audit.description().isBlank()
                    ? audit.resource().label() + audit.action().label()
                    : audit.description().trim();
            return new AuditMeta(true, resource, action, description);
        }
    }
}
