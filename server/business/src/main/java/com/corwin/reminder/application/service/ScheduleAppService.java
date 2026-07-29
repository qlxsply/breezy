package com.corwin.reminder.application.service;

import com.corwin.framework.config.DefaultConfigWrapper;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.sort.PageSpecSorts;
import com.corwin.reminder.application.command.UpsertScheduleCommand;
import com.corwin.reminder.application.event.ScheduleReminderChangedEvent;
import com.corwin.reminder.domain.model.RecurrenceRule;
import com.corwin.reminder.domain.model.RecurrenceRuleFrequency;
import com.corwin.reminder.domain.model.ScheduleEvent;
import com.corwin.reminder.domain.model.ScheduleEventStatus;
import com.corwin.reminder.domain.repo.ScheduleEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 *
 * @author Corwin 2026/1/12
 */
@Service
@RequiredArgsConstructor
public class ScheduleAppService {

    private final ScheduleEventRepository repo;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long upsert(UpsertScheduleCommand cmd) {
        Objects.requireNonNull(cmd, "cmd required");

        RecurrenceRule rule = toRule(cmd.rule());

        String tz = cmd.eventTimeZoneId();
        if (tz == null || tz.isBlank()) {
            tz = DefaultConfigWrapper.timeZone().getID();
        }

        ScheduleEvent e;
        if (cmd.id() == null) {
            e = ScheduleEvent.create(cmd.title(), cmd.startTime(), cmd.endTime(), tz, rule, cmd.advanceSecondsList(),
                    cmd.note());
            e.assignOwner(currentUserId());
        } else {
            e = repo.findById(cmd.id())
                    .orElseThrow(() -> new IllegalArgumentException("schedule not found: " + cmd.id()));
            e.update(cmd.title(), cmd.startTime(), cmd.endTime(), tz, rule, cmd.advanceSecondsList(), cmd.note());
        }
        repo.save(e);
        eventPublisher.publishEvent(ScheduleReminderChangedEvent.updated(e.getId()));
        return e.getId();
    }

    public ScheduleEvent get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("schedule not found: " + id));
    }

    public PageData<ScheduleEvent> page(ScheduleEventStatus status, String titleLike, PageSpec spec) {
        return repo.page(status, titleLike, PageSpecSorts.apply(spec));
    }

    @Transactional
    public void pause(Long id) {
        ScheduleEvent e = get(id);
        e.pause();
        repo.save(e);
        eventPublisher.publishEvent(ScheduleReminderChangedEvent.removed(id));
    }

    @Transactional
    public void activate(Long id) {
        ScheduleEvent e = get(id);
        e.activate();
        repo.save(e);
        eventPublisher.publishEvent(ScheduleReminderChangedEvent.updated(id));
    }

    @Transactional
    public void cancel(Long id) {
        ScheduleEvent e = get(id);
        e.cancel();
        repo.save(e);
        eventPublisher.publishEvent(ScheduleReminderChangedEvent.removed(id));
    }

    private RecurrenceRule toRule(UpsertScheduleCommand.RuleCommand r) {
        Objects.requireNonNull(r, "rule required");
        RecurrenceRuleFrequency f = Objects.requireNonNull(r.frequency(), "frequency required");

        return switch (f) {
            case ONCE -> RecurrenceRule.once();
            case DAILY -> RecurrenceRule.daily(nvl(r.interval()), r.until());
            case WEEKLY -> RecurrenceRule.weekly(nvl(r.interval()),
                    Objects.requireNonNull(r.daysOfWeek(), "daysOfWeek required"), r.until());
            case MONTHLY -> RecurrenceRule.monthly(nvl(r.interval()),
                    Objects.requireNonNull(r.dayOfMonth(), "dayOfMonth required"), r.until());
            case YEARLY -> RecurrenceRule.yearly(nvl(r.interval()),
                    Objects.requireNonNull(r.monthOfYear(), "monthOfYear required"),
                    Objects.requireNonNull(r.dayOfMonth(), "dayOfMonth required"), r.until());
        };
    }

    private int nvl(Integer interval) {
        return (interval == null || interval <= 0) ? 1 : interval;
    }

    private Long currentUserId() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        Long userId = principal == null ? null : principal.userId();
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        return userId;
    }
}
