package com.corwin.system.user.interfaces.web.req;

import com.corwin.system.user.domain.model.UserStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Corwin 2026/1/22
 */
@Getter
@Setter
public class CreateUserReq {
    private String username;
    private String nickname;
    private String password;
}
