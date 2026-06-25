package com.corwin.system.file.interfaces.web;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.file.application.service.FileQueryService;
import com.corwin.system.file.application.service.StaticAssetQueryService;
import com.corwin.system.file.application.view.LogicalPhysicalFileView;
import com.corwin.system.file.infrastructure.storage.LocalStorageProvider;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Corwin 2026/5/26
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/public/static-files")
@RequiredArgsConstructor
public class PublicStaticAssetController {

    private final StaticAssetQueryService staticAssetQueryService;
    private final FileQueryService fileQueryService;
    private final LocalStorageProvider storageProvider;

    @GetMapping("/{fileId}")
    @PermitAll
    public void view(@PathVariable String fileId, HttpServletResponse response) throws Exception {
        write(staticAssetQueryService.getStaticAssetByFileId(fileId), response);
    }

    @GetMapping("/code/{code}")
    @PermitAll
    public void viewByCode(@PathVariable String code, HttpServletResponse response) throws Exception {
        String fileId = staticAssetQueryService.resolveFileIdByCode(code)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        write(staticAssetQueryService.getStaticAssetByFileId(fileId), response);
    }

    private void write(LogicalPhysicalFileView fileView, HttpServletResponse response) throws Exception {
        String contentType = fileQueryService.resolveContentType(fileView.logicalFile().getFileName(),
                fileView.physicalFile().getContentType());
        response.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
        response.setContentLengthLong(fileView.physicalFile().getFileSize());
        try (InputStream is = storageProvider.read(fileView.physicalFile().getRelativePath(),
                fileView.physicalFile().getFileName()); OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
        }
    }
}
