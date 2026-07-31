package com.corwin.system.file.interfaces.web;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.file.application.command.StorageQueryCommand;
import com.corwin.system.file.application.command.StorageSortBy;
import com.corwin.system.file.application.command.StorageSortOrder;
import com.corwin.system.file.application.command.UploadFileCommand;
import com.corwin.system.file.application.service.FileQueryService;
import com.corwin.system.file.application.service.FileService;
import com.corwin.system.file.application.view.LogicalPhysicalFileView;
import com.corwin.system.file.application.view.StorageNodeView;
import com.corwin.system.file.config.SystemFileConfigSpecs;
import com.corwin.system.file.domain.model.LogicalFile;
import com.corwin.system.file.domain.model.PhysicalFile;
import com.corwin.system.file.infrastructure.storage.LocalStorageProvider;
import com.corwin.system.file.interfaces.web.req.StorageListReq;
import com.corwin.system.file.interfaces.web.res.PhysicalFileDetailRes;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.OwnerType;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST controller for system file operations including upload, preview, download,
 * and administrative queries.
 *
 * @author Corwin 2026/2/23
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/sys/files")
@RequiredArgsConstructor
public class SystemFileController {

    private final FileService fileService;
    private final FileQueryService fileQueryService;
    private final LocalStorageProvider storageProvider;

    @PostMapping("/upload")
    @Authorize(userType = UserType.ADMIN, permissions = {"sys.file.upload"})
    public ApiResponse<String> upload(@RequestParam FilePurpose purpose, @RequestParam(required = false) String ownerId,
            @RequestParam("file") MultipartFile file) throws Exception {
        String effectiveOwnerId = (ownerId == null || ownerId.isBlank()) ? "system" : ownerId;
        UploadFileCommand cmd = new UploadFileCommand(OwnerType.APPLICATION, effectiveOwnerId, null,
                file.getOriginalFilename(), file.getContentType());
        try (InputStream is = file.getInputStream()) {
            return ApiResponse.ok(fileService.uploadFile(cmd, is, purpose));
        }
    }

    @GetMapping("/view/{fileId}")
    @Authorize(userType = UserType.ADMIN, permissions = {"sfl.preview"})
    public void view(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        LogicalPhysicalFileView fileView = fileQueryService.getLogicalPhysicalFile(fileId);
        LogicalFile logicalFile = fileView.logicalFile();
        PhysicalFile pf = fileView.physicalFile();

        long previewLimit = Configs.snapshot(SystemFileConfigSpecs.PREVIEW).value().maxSizeBytes();
        if (previewLimit > 0 && pf.getFileSize() != null && pf.getFileSize() > previewLimit) {
            throw new BizException("文件大小超过预览限制", BaseError.ILLEGAL_ARGUMENT);
        }

        String contentType = fileQueryService.resolveContentType(logicalFile.getFileName(), pf.getContentType());
        response.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        try (InputStream is = storageProvider.read(pf.getRelativePath(), pf.getFileName());
                OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
        }
    }

    @GetMapping("/meta/{fileId}")
    @Authorize(userType = UserType.ADMIN, permissions = {"sfl.preview"})
    public ApiResponse<StorageNodeView> meta(@PathVariable String fileId) {
        return ApiResponse.ok(fileQueryService.getFileMetadata(fileId));
    }

    @GetMapping("/download/{fileId}")
    @Authorize(userType = UserType.ADMIN, permissions = {"sfl.download"})
    public void download(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        LogicalPhysicalFileView fileView = fileQueryService.getLogicalPhysicalFile(fileId);
        LogicalFile logicalFile = fileView.logicalFile();
        PhysicalFile pf = fileView.physicalFile();

        String originalFileName = logicalFile.getFileName();
        String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8).replace("+", "%20");
        String asciiFallback = originalFileName.replaceAll("[^\\x20-\\x7E]", "_");
        response.setContentType(
                pf.getContentType() != null ? pf.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodedFileName);
        response.setContentLengthLong(pf.getFileSize());
        try (InputStream is = storageProvider.read(pf.getRelativePath(), pf.getFileName());
                OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
        }
    }

    @PostMapping("/metadata/batch")
    @Authorize(userType = UserType.ADMIN, permissions = {"sys.file.metadata.batch"})
    public ApiResponse<List<StorageNodeView>> getMetadataBatch(@RequestBody List<String> ids) {
        return ApiResponse.ok(fileQueryService.getMetadataBatch(ids));
    }

    @GetMapping("/admin/list")
    @Authorize(userType = UserType.ADMIN, permissions = {"sfl.view"})
    public ApiResponse<List<StorageNodeView>> adminList() {
        return ApiResponse.ok(fileQueryService.listAllFileMetadata());
    }

    @PostMapping("/admin/nodes")
    @Authorize(userType = UserType.ADMIN, permissions = {"sfl.view"})
    public ApiResponse<List<StorageNodeView>> adminNodes(@RequestBody StorageListReq req) {
        String keyword = req.keyword();
        boolean recursive = Boolean.TRUE.equals(req.recursive());
        StorageSortBy sortBy = req.sortBy() == null ? StorageSortBy.NAME : req.sortBy();
        StorageSortOrder sortOrder = req.sortOrder() == null ? StorageSortOrder.ASC : req.sortOrder();
        boolean effectiveRecursive = recursive || (keyword != null && !keyword.isBlank());
        StorageQueryCommand query = new StorageQueryCommand(req.parentId(), keyword, effectiveRecursive, sortBy,
                sortOrder);
        return ApiResponse.ok(fileQueryService.listAdminContent(query));
    }

    @GetMapping("/admin/logical-files/{logicalFileId}/physical")
    @Authorize(userType = UserType.ADMIN, permissions = {"sfl.phys.view"})
    public ApiResponse<PhysicalFileDetailRes> physicalDetail(@PathVariable String logicalFileId) {
        LogicalPhysicalFileView fileView = fileQueryService.getLogicalPhysicalFile(logicalFileId);
        LogicalFile logicalFile = fileView.logicalFile();
        PhysicalFile physicalFile = fileView.physicalFile();
        String absolutePath = storageProvider.getBasePath().resolve(physicalFile.getRelativePath())
                .resolve(physicalFile.getFileName()).toAbsolutePath().normalize().toString();
        PhysicalFileDetailRes detail = new PhysicalFileDetailRes(logicalFile.getId(), logicalFile.getFileName(),
                logicalFile.getOwnerType(), logicalFile.getOwnerId(), logicalFile.getParentId(), physicalFile.getId(),
                physicalFile.getHash(), physicalFile.getRelativePath(), absolutePath, physicalFile.getFileName(),
                physicalFile.getFileSize(), physicalFile.getContentType(), physicalFile.getRefCount(),
                physicalFile.getCreatedAt());
        return ApiResponse.ok(detail);
    }

    @PostMapping("/admin/physical/{physicalFileId}/logical-refs")
    @Authorize(userType = UserType.ADMIN, permissions = {"sfl.ref.view"})
    public ApiResponse<List<StorageNodeView>> logicalRefs(@PathVariable String physicalFileId,
            @RequestBody StorageListReq req) {
        StorageSortBy sortBy = req.sortBy() == null ? StorageSortBy.NAME : req.sortBy();
        StorageSortOrder sortOrder = req.sortOrder() == null ? StorageSortOrder.ASC : req.sortOrder();
        return ApiResponse.ok(fileQueryService.listLogicalFileRefs(physicalFileId, req.keyword(), sortBy, sortOrder));
    }
}
