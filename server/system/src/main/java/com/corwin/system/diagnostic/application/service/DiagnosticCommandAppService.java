package com.corwin.system.diagnostic.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.HighDate;
import com.corwin.system.diagnostic.application.command.StartDiagnosticCommand;
import com.corwin.system.diagnostic.application.command.UpdateDiagnosticConfigCommand;
import com.corwin.system.diagnostic.application.view.DiagnosticSessionView;
import com.corwin.system.diagnostic.domain.model.DiagnosticConfig;
import com.corwin.system.diagnostic.domain.model.DiagnosticSession;
import com.corwin.system.diagnostic.infrastructure.runtime.DiagnosticRuntimeManager;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * @author Corwin 2026/4/16
 */
@Service
public class DiagnosticCommandAppService {

    private final DiagnosticRuntimeManager runtimeManager;

    public DiagnosticCommandAppService(DiagnosticRuntimeManager runtimeManager) {
        this.runtimeManager = runtimeManager;
    }

    public DiagnosticSessionView start(StartDiagnosticCommand command) {
        Instant now = HighDate.realInstant();
        DiagnosticSession current = runtimeManager.currentSession().orElse(null);
        if (current != null) {
            return toView(current, now);
        }
        DiagnosticConfig config = buildConfig(command);
        DiagnosticSession session = DiagnosticSession.active(config, now);
        runtimeManager.start(session);
        return toView(session, now);
    }

    public DiagnosticSessionView updateConfig(UpdateDiagnosticConfigCommand command) {
        Instant now = HighDate.realInstant();
        DiagnosticSession current = runtimeManager.currentSession()
                .orElseThrow(() -> new BizException("运行时诊断未开启", BaseError.CONFLICT));
        DiagnosticConfig updatedConfig = mergeConfig(current.config(), command);
        DiagnosticSession updatedSession = current.withConfig(updatedConfig, now);
        runtimeManager.update(updatedSession);
        return toView(updatedSession, now);
    }

    public boolean stop() {
        return runtimeManager.stop();
    }

    private DiagnosticConfig buildConfig(StartDiagnosticCommand command) {
        DiagnosticConfig defaults = DiagnosticConfig.defaultConfig();
        if (command == null) {
            return defaults;
        }
        return new DiagnosticConfig(command.intervalMs() == null ? defaults.intervalMs() : command.intervalMs(),
                command.historyCapacity() == null ? defaults.historyCapacity() : command.historyCapacity(),
                command.eventCapacity() == null ? defaults.eventCapacity() : command.eventCapacity(),
                command.items() == null ? defaults.items() : command.items(),
                command.deepMode() == null ? defaults.deepMode() : command.deepMode(),
                command.slowRequestThresholdMs() == null ? defaults.slowRequestThresholdMs() :
                        command.slowRequestThresholdMs(),
                command.slowSqlThresholdMs() == null ? defaults.slowSqlThresholdMs() : command.slowSqlThresholdMs(),
                command.ttlSeconds() == null ? defaults.ttlSeconds() : command.ttlSeconds());
    }

    private DiagnosticConfig mergeConfig(DiagnosticConfig current, UpdateDiagnosticConfigCommand command) {
        if (command == null) {
            return current;
        }
        return new DiagnosticConfig(command.intervalMs() == null ? current.intervalMs() : command.intervalMs(),
                command.historyCapacity() == null ? current.historyCapacity() : command.historyCapacity(),
                command.eventCapacity() == null ? current.eventCapacity() : command.eventCapacity(),
                command.items() == null ? current.items() : command.items(),
                command.deepMode() == null ? current.deepMode() : command.deepMode(),
                command.slowRequestThresholdMs() == null ? current.slowRequestThresholdMs() :
                        command.slowRequestThresholdMs(),
                command.slowSqlThresholdMs() == null ? current.slowSqlThresholdMs() : command.slowSqlThresholdMs(),
                command.ttlSeconds() == null ? current.ttlSeconds() : command.ttlSeconds());
    }

    private DiagnosticSessionView toView(DiagnosticSession session, Instant now) {
        return new DiagnosticSessionView(session.status(), session.config(), session.startedAt(), session.expireAt(),
                session.remainingTtlSeconds(now));
    }
}
