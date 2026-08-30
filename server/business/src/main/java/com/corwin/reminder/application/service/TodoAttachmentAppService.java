package com.corwin.reminder.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.reminder.application.view.TodoAttachmentFileView;
import com.corwin.reminder.application.view.TodoAttachmentMetaView;
import com.corwin.reminder.domain.error.ReminderError;
import com.corwin.system.file.application.command.UploadFileCommand;
import com.corwin.system.file.application.port.FileCommandPort;
import com.corwin.system.file.application.port.FileQueryPort;
import com.corwin.system.file.application.view.StorageNodeView;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.OwnerType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Corwin 2026/3/12
 */
@Service
@RequiredArgsConstructor
public class TodoAttachmentAppService {

  private final FileCommandPort fileCommandPort;
  private final FileQueryPort fileQueryPort;

  @Transactional
  public String upload(MultipartFile file) {
    BizAssert.notNull(file, BaseError.MISSING_PARAMETER);
    BizAssert.notBlank(file.getOriginalFilename(), BaseError.MISSING_PARAMETER);
    BizAssert.state(isImage(file.getContentType()), ReminderError.TODO_ATTACHMENT_IMAGE_ONLY);

    UploadFileCommand command =
        new UploadFileCommand(
            OwnerType.APPLICATION, "todo", null, file.getOriginalFilename(), file.getContentType());
    try (InputStream inputStream = file.getInputStream()) {
      return fileCommandPort.uploadFile(command, inputStream, FilePurpose.ATTACHMENT);
    } catch (IOException ex) {
      throw new BizException("待办附件上传失败", BaseError.SERVICE_ERROR);
    }
  }

  public List<TodoAttachmentMetaView> metadata(List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return fileQueryPort.getMetadataBatch(ids).stream().map(this::toMetaView).toList();
  }

  public TodoAttachmentFileView view(String fileId) {
    BizAssert.notBlank(fileId, BaseError.MISSING_PARAMETER);
    StorageNodeView view = fileQueryPort.getFileMetadata(fileId);
    return new TodoAttachmentFileView(
        view.name(), view.contentType(), fileCommandPort.readFileContent(fileId));
  }

  private TodoAttachmentMetaView toMetaView(StorageNodeView view) {
    return new TodoAttachmentMetaView(
        view.id(),
        view.name(),
        view.size(),
        view.contentType(),
        view.createdAt(),
        view.updatedAt());
  }

  private boolean isImage(String contentType) {
    return contentType != null && contentType.toLowerCase().startsWith("image/");
  }
}
