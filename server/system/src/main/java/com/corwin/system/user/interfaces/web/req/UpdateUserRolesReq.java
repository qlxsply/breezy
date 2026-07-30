package com.corwin.system.user.interfaces.web.req;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request object for replacing all role assignments for a user.
 *
 * @author Corwin 2026/1/23
 */
@Getter
@Setter
public class UpdateUserRolesReq {
    private List<Long> roleIds;
}
