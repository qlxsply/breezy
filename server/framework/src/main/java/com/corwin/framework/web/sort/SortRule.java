package com.corwin.framework.web.sort;

import com.corwin.framework.domain.page.SortSpec;
import java.util.List;

/**
 * Sort rules for the current endpoint.
 *
 * @param enabled whether sorting is enabled for this endpoint
 * @param allowed list of sortable fields (frontend field -> SQL column mapping)
 * @param defaults default sort specifications applied when the request has no sort
 * @author Corwin 2026/7/29
 */
public record SortRule(boolean enabled, List<SortableField> allowed, List<SortSpec> defaults) {

  public SortRule {
    allowed = allowed == null ? List.of() : List.copyOf(allowed);
    defaults = defaults == null ? List.of() : List.copyOf(defaults);
  }

  public static SortRule disabled() {
    return new SortRule(false, List.of(), List.of());
  }
}
