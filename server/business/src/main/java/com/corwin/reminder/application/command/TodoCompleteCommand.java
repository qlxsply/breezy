package com.corwin.reminder.application.command;

import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
public record TodoCompleteCommand(
        String completionNote,
        List<String> completionAttachmentFileIds
) {
}
