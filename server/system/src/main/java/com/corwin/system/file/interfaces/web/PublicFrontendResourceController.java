package com.corwin.system.file.interfaces.web;

import com.corwin.framework.util.Defaults;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.file.application.service.FrontendResourceQueryService;
import com.corwin.system.file.application.view.FrontendResourceFileView;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public controller for serving frontend static resources (JS, CSS, images, etc.). All endpoints
 * are unauthenticated ({@link PermitAll}) and serve files from the app-resources directory with
 * caching headers.
 *
 * @author Corwin 2026/6/15
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/public/frontend-resources")
@RequiredArgsConstructor
public class PublicFrontendResourceController {

  private final FrontendResourceQueryService frontendResourceQueryService;

  /**
   * Serves a frontend resource file. The path is resolved and streamed to the HTTP response with
   * appropriate content type and caching headers.
   *
   * @param resourcePath the wildcard resource path (may include subdirectories)
   * @param response the HTTP response to write to
   */
  @GetMapping("/{*resourcePath}")
  @PermitAll
  public void view(@PathVariable String resourcePath, HttpServletResponse response)
      throws Exception {
    write(frontendResourceQueryService.getResource(resourcePath), response);
  }

  private void write(FrontendResourceFileView fileView, HttpServletResponse response)
      throws Exception {
    String contentType =
        Defaults.or(fileView.contentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    response.setContentType(contentType);
    response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
    response.setContentLengthLong(fileView.fileSize());
    try (InputStream is = Files.newInputStream(fileView.filePath());
        OutputStream os = response.getOutputStream()) {
      is.transferTo(os);
    }
  }
}
