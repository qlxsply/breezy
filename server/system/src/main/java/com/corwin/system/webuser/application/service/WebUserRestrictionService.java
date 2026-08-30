package com.corwin.system.webuser.application.service;

import com.corwin.framework.util.HighDate;
import com.corwin.system.webuser.domain.model.WebUserRestrictionScope;
import com.corwin.system.webuser.domain.repo.WebUserRestrictionRepository;
import org.springframework.stereotype.Service;

/**
 * Service for checking login restrictions (e.g. temporary bans) on web users.
 *
 * @author Corwin 2026/5/11
 */
@Service
public class WebUserRestrictionService {

  private final WebUserRestrictionRepository repository;

  public WebUserRestrictionService(WebUserRestrictionRepository repository) {
    this.repository = repository;
  }

  /**
   * Check if the given user has any active login-level restriction.
   *
   * @param userId the user ID
   * @return true if a login restriction is currently active
   */
  public boolean hasLoginRestriction(Long userId) {
    return repository.findByUserId(userId).stream()
        .filter(
            restriction ->
                restriction.getRestrictionScope() == WebUserRestrictionScope.LOGIN
                    || restriction.getRestrictionScope() == WebUserRestrictionScope.ALL)
        .anyMatch(restriction -> restriction.activeAt(HighDate.realInstant()));
  }
}
