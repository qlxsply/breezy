package com.corwin.system.role.interfaces.web.req;

import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating a new role.
 *
 * @author Corwin 2026/1/23
 */
@Getter
@Setter
public class CreateRoleReq {
    private String code;
    private String name;
    private Boolean enabled;
}
