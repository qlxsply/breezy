package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserRestriction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/5/11
 */
public interface WebUserRestrictionJpaRepository extends JpaRepository<WebUserRestriction, Long> {

    List<WebUserRestriction> findByUserId(Long userId);
}
