package com.corwin.system.user.interfaces.web.req;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Request object for creating a new admin user.
 *
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
