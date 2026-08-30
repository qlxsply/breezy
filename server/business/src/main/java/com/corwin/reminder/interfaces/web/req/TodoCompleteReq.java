package com.corwin.reminder.interfaces.web.req;

import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
public record TodoCompleteReq(String completionNote, List<String> completionAttachmentFileIds) {}
