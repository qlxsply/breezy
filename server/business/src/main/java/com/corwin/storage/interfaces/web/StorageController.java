package com.corwin.storage.interfaces.web;

import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.storage.interfaces.web.req.StorageFolderCreateReq;
import com.corwin.storage.interfaces.web.req.StorageFolderRenameReq;
import com.corwin.storage.interfaces.web.req.StorageListReq;
import com.corwin.storage.interfaces.web.req.StorageMoveReq;
import com.corwin.storage.interfaces.web.res.StorageItemRes;
import com.corwin.system.file.application.command.StorageQueryCommand;
import com.corwin.system.file.application.command.StorageSortBy;
import com.corwin.system.file.application.command.StorageSortOrder;
import com.corwin.system.file.application.command.UploadFileCommand;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.OwnerType;
import com.corwin.system.file.application.port.FileCommandPort;
import com.corwin.system.file.application.port.FileQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/2/23
 */
@ApiMeta(module = ApiModuleCode.STORAGE)
@Authorize(userType = UserType.USER, permissions = {"stg.use"})
@RestController
@RequestMapping("/api/sys/storage")
@RequiredArgsConstructor
public class StorageController {

    private final FileCommandPort fileService;
    private final FileQueryPort fileQueryService;

    @PostMapping("/list")
    public ApiResponse<List<StorageItemRes>> list(@RequestBody(required = false) StorageListReq req) {
        StorageListReq resolved = req == null ? new StorageListReq(null, null, null, null, null) : req;
        String ownerId = currentUserId().toString();
        String keyword = resolved.keyword();
        boolean recursive = Boolean.TRUE.equals(resolved.recursive());
        StorageSortBy sortBy = resolved.sortBy() == null ? StorageSortBy.NAME : resolved.sortBy();
        StorageSortOrder sortOrder = resolved.sortOrder() == null ? StorageSortOrder.ASC : resolved.sortOrder();
        boolean effectiveRecursive = recursive || (keyword != null && !keyword.isBlank());
        StorageQueryCommand query = new StorageQueryCommand(resolved.parentId(), keyword, effectiveRecursive, sortBy,
                sortOrder);
        List<StorageItemRes> items = fileQueryService.listContent(OwnerType.USER, ownerId, query).stream()
                .map(v -> new StorageItemRes(v.id(), v.type(), v.name(), v.parentId(), v.size(), v.contentType(),
                        v.createdAt(), v.updatedAt()))
                .collect(Collectors.toList());
        return ApiResponse.ok(items);
    }

    @PostMapping("/folders")
    public ApiResponse<String> createFolder(@RequestBody StorageFolderCreateReq req) {
        String ownerId = currentUserId().toString();
        return ApiResponse.ok(fileService.createFolder(OwnerType.USER, ownerId, req.parentId(), req.name()));
    }

    @PutMapping("/folders/{folderId}")
    public ApiResponse<Void> renameFolder(@PathVariable String folderId, @RequestBody StorageFolderRenameReq req) {
        String ownerId = currentUserId().toString();
        fileService.renameFolder(folderId, req.newName(), OwnerType.USER, ownerId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/files/{fileId}")
    public ApiResponse<Void> renameFile(@PathVariable String fileId, @RequestBody StorageFolderRenameReq req) {
        String ownerId = currentUserId().toString();
        fileService.renameFile(fileId, req.newName(), OwnerType.USER, ownerId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/folders/{folderId}/move")
    public ApiResponse<Void> moveFolder(@PathVariable String folderId, @RequestBody StorageMoveReq req) {
        String ownerId = currentUserId().toString();
        fileService.moveFolder(folderId, req.targetParentId(), OwnerType.USER, ownerId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/files/{fileId}/move")
    public ApiResponse<Void> moveFile(@PathVariable String fileId, @RequestBody StorageMoveReq req) {
        String ownerId = currentUserId().toString();
        fileService.moveFile(fileId, req.targetParentId(), OwnerType.USER, ownerId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/folders/{folderId}")
    public ApiResponse<Void> deleteFolder(@PathVariable String folderId,
            @RequestParam(defaultValue = "false") boolean recursive) {
        String ownerId = currentUserId().toString();
        fileService.deleteFolder(folderId, recursive, OwnerType.USER, ownerId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam(required = false) String parentId,
            @RequestParam("file") MultipartFile file) throws Exception {
        String ownerId = currentUserId().toString();
        UploadFileCommand cmd = new UploadFileCommand(OwnerType.USER, ownerId, parentId, file.getOriginalFilename(),
                file.getContentType());
        try (InputStream is = file.getInputStream()) {
            return ApiResponse.ok(fileService.uploadFile(cmd, is, FilePurpose.DRIVE));
        }
    }

    @DeleteMapping("/files/{fileId}")
    public ApiResponse<Boolean> deleteFile(@PathVariable String fileId) {
        String ownerId = currentUserId().toString();
        fileService.deleteFile(fileId, OwnerType.USER, ownerId);
        return ApiResponse.ok(true);
    }

    private Long currentUserId() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        Long userId = principal == null ? null : principal.userId();
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        return userId;
    }
}
