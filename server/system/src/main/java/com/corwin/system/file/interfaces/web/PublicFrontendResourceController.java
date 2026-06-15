package com.corwin.system.file.interfaces.web;

import com.corwin.framework.util.Defaults;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.file.application.service.FrontendResourceQueryService;
import com.corwin.system.file.application.view.FrontendResourceFileView;
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
import java.nio.file.Files;

/**
 * @author Corwin 2026/6/15
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/public/frontend-resources")
@RequiredArgsConstructor
public class PublicFrontendResourceController {

    private final FrontendResourceQueryService frontendResourceQueryService;

    @GetMapping("/{*resourcePath}")
    @PermitAll
    public void view(@PathVariable String resourcePath, HttpServletResponse response) throws Exception {
        write(frontendResourceQueryService.getResource(resourcePath), response);
    }

    private void write(FrontendResourceFileView fileView, HttpServletResponse response) throws Exception {
        String contentType = Defaults.or(fileView.contentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
        response.setContentLengthLong(fileView.fileSize());
        try (InputStream is = Files.newInputStream(fileView.filePath()); OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
        }
    }
}
