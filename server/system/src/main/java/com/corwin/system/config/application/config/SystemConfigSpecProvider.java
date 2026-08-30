package com.corwin.system.config.application.config;

import com.corwin.framework.config.definition.ConfigSpec;
import com.corwin.framework.config.definition.ConfigSpecProvider;
import com.corwin.system.audit.config.SystemAuditConfigSpecs;
import com.corwin.system.auth.config.SystemAuthConfigSpecs;
import com.corwin.system.file.config.SystemFileConfigSpecs;
import com.corwin.system.notify.config.SystemNotifyConfigSpecs;
import com.corwin.system.user.config.SystemUserConfigSpecs;
import java.util.Collection;
import java.util.List;

/**
 * @author Corwin 2026/7/31
 */
public final class SystemConfigSpecProvider implements ConfigSpecProvider {

  @Override
  public Collection<ConfigSpec<?>> getConfigSpecs() {
    return List.of(
        SystemAuthConfigSpecs.AUTHENTICATION,
        SystemAuthConfigSpecs.PASSWORD_POLICY,
        SystemAuditConfigSpecs.AUDIT_POLICY,
        SystemFileConfigSpecs.STORAGE,
        SystemFileConfigSpecs.PREVIEW,
        SystemNotifyConfigSpecs.SSE,
        SystemNotifyConfigSpecs.WEB_PUSH,
        SystemNotifyConfigSpecs.MESSAGE_TYPES,
        SystemUserConfigSpecs.USER_PREFERENCE_DEFAULTS);
  }
}
