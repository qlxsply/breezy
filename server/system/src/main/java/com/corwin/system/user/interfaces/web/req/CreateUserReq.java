package com.corwin.system.user.interfaces.web.req;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Corwin 2026/1/22
 */
@Getter
@Setter
public class CreateUserReq {
    private String username;
    private String nickname;
    private String password;
    private List<Long> roleIds;
}
