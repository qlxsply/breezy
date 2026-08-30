package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserRestriction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link WebUserRestriction} entity.
 *
 * @author Corwin 2026/5/11
 */
public interface WebUserRestrictionJpaRepository extends JpaRepository<WebUserRestriction, Long> {

  /**
   * Find all restrictions associated with the given user ID.
   *
   * @param userId the user ID
   * @return list of restrictions
   */
  List<WebUserRestriction> findByUserId(Long userId);
}
