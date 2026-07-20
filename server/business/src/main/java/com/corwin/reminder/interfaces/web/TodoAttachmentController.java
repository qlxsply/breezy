package com.corwin.reminder.interfaces.web;

import com.corwin.reminder.application.service.TodoAttachmentAppService;
import com.corwin.reminder.application.view.TodoAttachmentFileView;
import com.corwin.reminder.application.view.TodoAttachmentMetaView;
import com.corwin.reminder.interfaces.web.res.TodoAttachmentMetaRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
@ApiMeta(module = ApiModuleCode.REMINDER)
@Authorize(userType = UserType.USER, permissions = {"tdo.use"})
@RestController
@RequestMapping("/api/todo/attachments")
@RequiredArgsConstructor
public class TodoAttachmentController {

    private final TodoAttachmentAppService service;

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.upload(file));
    }

    @PostMapping("/metadata")
    public ApiResponse<List<TodoAttachmentMetaRes>> metadata(@RequestBody List<String> ids) {
        return ApiResponse.ok(service.metadata(ids).stream().map(TodoAttachmentController::toRes).toList());
    }

    @GetMapping("/{fileId}/view")
    public void view(@PathVariable String fileId, HttpServletResponse response) throws IOException {
        TodoAttachmentFileView fileView = service.view(fileId);
        response.setContentType(fileView.contentType() != null ? fileView.contentType() : MediaType.IMAGE_PNG_VALUE);
        try (OutputStream os = response.getOutputStream()) {
            os.write(fileView.content());
        }
    }

    private static TodoAttachmentMetaRes toRes(TodoAttachmentMetaView view) {
        return new TodoAttachmentMetaRes(view.id(), view.name(), view.size(), view.contentType(), view.createdAt(),
                view.updatedAt());
    }
}
