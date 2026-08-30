package com.corwin.system.resource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * API metadata entity.
 *
 * <p>Stores runtime information about registered API endpoints, including their module, protocol,
 * HTTP method, path pattern, access control attributes, audit configuration, and sort options.
 *
 * @author Corwin 2026/1/23
 */
@Getter
@Entity
@Table(
    name = "sys_api",
    indexes = {
      @Index(
          name = "idx_sys_api_unique",
          columnList = "module,protocol,http_method,path_pattern",
          unique = true),
      @Index(name = "idx_sys_api_module", columnList = "module"),
      @Index(name = "idx_sys_api_path", columnList = "module,http_method,path_pattern"),
      @Index(name = "idx_sys_api_access_type", columnList = "access_type")
    })
public class Api {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Module code that this API belongs to (e.g. "system", "datasource"). */
  @Column(name = "module", nullable = false, length = 128)
  private String module;

  /** Communication protocol (e.g. HTTP). */
  @Enumerated(EnumType.STRING)
  @Column(name = "protocol", nullable = false, length = 16)
  private ApiProtocol protocol;

  /** HTTP method (GET, POST, PUT, DELETE, etc.). */
  @Enumerated(EnumType.STRING)
  @Column(name = "http_method", nullable = false, length = 16)
  private ApiMethod httpMethod;

  /** Request path pattern (e.g. "/api/users/{id}"). */
  @Column(name = "path_pattern", nullable = false, length = 512)
  private String pathPattern;

  /** Fully qualified handler class name. */
  @Column(name = "handler_class", length = 512)
  private String handlerClass;

  /** Handler method name. */
  @Column(name = "handler_method", length = 128)
  private String handlerMethod;

  /** Whether a permission declaration is present on this API endpoint. */
  @Column(name = "permission_declared", nullable = false)
  private Boolean permissionDeclared;

  /** Access control type (PERMIT_ALL, AUTHENTICATED, AUTHORIZED, or DENY). */
  @Enumerated(EnumType.STRING)
  @Column(name = "access_type", nullable = false, length = 32)
  private ApiAccessType accessType;

  /** Allowed user types as a JSON array string (e.g. {@code ["ADMIN","USER"]}). */
  @Lob
  @Column(name = "user_type", length = 16)
  private String userType;

  /** Whether audit logging is declared for this API. */
  @Column(name = "audit_declared", nullable = false)
  private Boolean auditDeclared;

  /** Audit resource name. */
  @Column(name = "audit_resource", length = 128)
  private String auditResource;

  /** Audit action name. */
  @Column(name = "audit_action", length = 128)
  private String auditAction;

  /** Audit description. */
  @Column(name = "audit_description", length = 512)
  private String auditDescription;

  /** Sort options JSON configuration for this API. */
  @Lob
  @Column(name = "sort_options_json")
  private String sortOptionsJson;

  /** Whether this API is enabled. */
  @Column(name = "enabled", nullable = false)
  private Boolean enabled;

  /** Whether this API record is system-defined. */
  @Column(name = "system_builtin", nullable = false)
  private Boolean systemBuiltin;

  /** Creation timestamp. */
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Last update timestamp. */
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Api() {}

  /**
   * Creates a new API metadata record.
   *
   * @param module the module this API belongs to
   * @param protocol the communication protocol
   * @param httpMethod the HTTP method
   * @param pathPattern the request path pattern
   * @param handlerClass the fully qualified handler class name
   * @param handlerMethod the handler method name
   * @param permissionDeclared whether a permission is declared for this API
   * @param accessType the access control type
   * @param userType the allowed user types (JSON array string)
   * @param auditDeclared whether audit logging is enabled
   * @param auditResource the audit resource name
   * @param auditAction the audit action name
   * @param auditDescription the audit description
   * @param systemBuiltin whether this API is system-defined
   */
  public Api(
      String module,
      ApiProtocol protocol,
      ApiMethod httpMethod,
      String pathPattern,
      String handlerClass,
      String handlerMethod,
      Boolean permissionDeclared,
      ApiAccessType accessType,
      String userType,
      Boolean auditDeclared,
      String auditResource,
      String auditAction,
      String auditDescription,
      Boolean systemBuiltin) {
    Instant now = HighDate.mockInstant();
    this.module = module;
    this.protocol = protocol;
    this.httpMethod = httpMethod;
    this.pathPattern = pathPattern;
    this.handlerClass = handlerClass;
    this.handlerMethod = handlerMethod;
    this.permissionDeclared = permissionDeclared;
    this.accessType = accessType;
    this.userType = userType;
    this.auditDeclared = auditDeclared;
    this.auditResource = auditResource;
    this.auditAction = auditAction;
    this.auditDescription = auditDescription;
    this.sortOptionsJson = null;
    this.enabled = true;
    this.systemBuiltin = systemBuiltin;
    this.createdAt = now;
    this.updatedAt = now;
  }

  /**
   * Updates all fields of this API metadata record.
   *
   * @param module the module
   * @param protocol the protocol
   * @param httpMethod the HTTP method
   * @param pathPattern the path pattern
   * @param handlerClass the handler class
   * @param handlerMethod the handler method
   * @param permissionDeclared whether permission is declared
   * @param accessType the access type
   * @param userType the allowed user types
   * @param auditDeclared whether audit is enabled
   * @param auditResource the audit resource
   * @param auditAction the audit action
   * @param auditDescription the audit description
   * @param systemBuiltin whether system-defined
   * @param enabled whether the API is enabled
   */
  public void update(
      String module,
      ApiProtocol protocol,
      ApiMethod httpMethod,
      String pathPattern,
      String handlerClass,
      String handlerMethod,
      Boolean permissionDeclared,
      ApiAccessType accessType,
      String userType,
      Boolean auditDeclared,
      String auditResource,
      String auditAction,
      String auditDescription,
      Boolean systemBuiltin,
      Boolean enabled) {
    this.module = module;
    this.protocol = protocol;
    this.httpMethod = httpMethod;
    this.pathPattern = pathPattern;
    this.handlerClass = handlerClass;
    this.handlerMethod = handlerMethod;
    this.permissionDeclared = permissionDeclared;
    this.accessType = accessType;
    this.userType = userType;
    this.auditDeclared = auditDeclared;
    this.auditResource = auditResource;
    this.auditAction = auditAction;
    this.auditDescription = auditDescription;
    this.systemBuiltin = systemBuiltin;
    this.enabled = enabled;
    touch();
  }

  /**
   * Updates the sort options JSON configuration.
   *
   * @param sortOptionsJson the sort options JSON string
   */
  public void updateSortOptions(String sortOptionsJson) {
    this.sortOptionsJson = sortOptionsJson;
    touch();
  }

  /** Enables this API. */
  public void enable() {
    this.enabled = true;
    touch();
  }

  /** Disables this API. */
  public void disable() {
    this.enabled = false;
    touch();
  }

  private void touch() {
    this.updatedAt = HighDate.mockInstant();
  }
}
