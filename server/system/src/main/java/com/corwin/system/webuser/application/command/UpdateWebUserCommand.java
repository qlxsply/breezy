package com.corwin.system.webuser.application.command;

/**
 * Command for updating a web user's status.
 *
 * @param status the target status value
 * @author Corwin 2026/5/11
 */
public record UpdateWebUserCommand(
        String status
) {
}
