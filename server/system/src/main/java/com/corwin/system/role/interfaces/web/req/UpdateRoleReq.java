package com.corwin.system.role.interfaces.web.req;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Corwin 2026/1/23
 */
@Getter
@Setter
public class UpdateRoleReq {
    private String code;
    private String name;
    private Boolean enabled;
}
