package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.model.WebUserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Corwin 2026/5/11
 */
public interface WebUserJpaRepository extends JpaRepository<WebUser, Long> {

    Page<WebUser> findByStatus(WebUserStatus status, Pageable pageable);

    Page<WebUser> findByDisplayNameContainingIgnoreCaseOrNicknameContainingIgnoreCase(String displayName,
            String nickname, Pageable pageable);

    Page<WebUser> findByStatusAndDisplayNameContainingIgnoreCaseOrStatusAndNicknameContainingIgnoreCase(
            WebUserStatus leftStatus, String displayName, WebUserStatus rightStatus, String nickname,
            Pageable pageable);
}
