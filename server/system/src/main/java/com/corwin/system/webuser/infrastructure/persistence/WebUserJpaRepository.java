package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.model.WebUserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link WebUser} entity providing basic query methods.
 *
 * @author Corwin 2026/5/11
 */
public interface WebUserJpaRepository extends JpaRepository<WebUser, Long> {

  /** Find users by status with pagination. */
  Page<WebUser> findByStatus(WebUserStatus status, Pageable pageable);

  /** Find users by display name or nickname with case-insensitive contains matching. */
  Page<WebUser> findByDisplayNameContainingIgnoreCaseOrNicknameContainingIgnoreCase(
      String displayName, String nickname, Pageable pageable);

  /** Find users by status combined with display name or nickname search. */
  Page<WebUser>
      findByStatusAndDisplayNameContainingIgnoreCaseOrStatusAndNicknameContainingIgnoreCase(
          WebUserStatus leftStatus,
          String displayName,
          WebUserStatus rightStatus,
          String nickname,
          Pageable pageable);
}
