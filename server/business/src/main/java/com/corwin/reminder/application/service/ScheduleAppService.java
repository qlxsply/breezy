package com.corwin.reminder.application.service;

import com.corwin.framework.config.runtime.Configs;
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
import com.corwin.system.user.config.SystemUserConfigSpecs;
import jakarta.transaction.Transactional;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
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
      tz = Configs.get(SystemUserConfigSpecs.USER_PREFERENCE_DEFAULTS).timeZone().zoneId();
    }

    ScheduleEvent event;
    if (cmd.id() == null) {
      event =
          ScheduleEvent.create(
              cmd.title(),
              cmd.startTime(),
              cmd.endTime(),
              tz,
              rule,
              cmd.advanceSecondsList(),
              cmd.note());
      event.assignOwner(currentUserId());
    } else {
      event =
          repo.findById(cmd.id())
              .orElseThrow(() -> new IllegalArgumentException("schedule not found: " + cmd.id()));
      event.update(
          cmd.title(),
          cmd.startTime(),
          cmd.endTime(),
          tz,
          rule,
          cmd.advanceSecondsList(),
          cmd.note());
    }
    repo.save(event);
    eventPublisher.publishEvent(ScheduleReminderChangedEvent.updated(event.getId()));
    return event.getId();
  }

  public ScheduleEvent get(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("schedule not found: " + id));
  }

  public PageData<ScheduleEvent> page(ScheduleEventStatus status, String titleLike, PageSpec spec) {
    return repo.page(status, titleLike, PageSpecSorts.apply(spec));
  }

  @Transactional
  public void pause(Long id) {
    ScheduleEvent event = get(id);
    event.pause();
    repo.save(event);
    eventPublisher.publishEvent(ScheduleReminderChangedEvent.removed(id));
  }

  @Transactional
  public void activate(Long id) {
    ScheduleEvent event = get(id);
    event.activate();
    repo.save(event);
    eventPublisher.publishEvent(ScheduleReminderChangedEvent.updated(id));
  }

  @Transactional
  public void cancel(Long id) {
    ScheduleEvent event = get(id);
    event.cancel();
    repo.save(event);
    eventPublisher.publishEvent(ScheduleReminderChangedEvent.removed(id));
  }

  private RecurrenceRule toRule(UpsertScheduleCommand.RuleCommand rule) {
    Objects.requireNonNull(rule, "rule required");
    RecurrenceRuleFrequency frequency =
        Objects.requireNonNull(rule.frequency(), "frequency required");
    return switch (frequency) {
      case ONCE -> RecurrenceRule.once();
      case DAILY -> RecurrenceRule.daily(nvl(rule.interval()), rule.until());
      case WEEKLY ->
          RecurrenceRule.weekly(
              nvl(rule.interval()),
              Objects.requireNonNull(rule.daysOfWeek(), "daysOfWeek required"),
              rule.until());
      case MONTHLY ->
          RecurrenceRule.monthly(
              nvl(rule.interval()),
              Objects.requireNonNull(rule.dayOfMonth(), "dayOfMonth required"),
              rule.until());
      case YEARLY ->
          RecurrenceRule.yearly(
              nvl(rule.interval()),
              Objects.requireNonNull(rule.monthOfYear(), "monthOfYear required"),
              Objects.requireNonNull(rule.dayOfMonth(), "dayOfMonth required"),
              rule.until());
    };
  }

  private int nvl(Integer interval) {
    return interval == null || interval <= 0 ? 1 : interval;
  }

  private Long currentUserId() {
    AuthPrincipal principal = CtxUtil.getPrincipal();
    Long userId = principal == null ? null : principal.userId();
    BizAssert.notNull(userId, BaseError.FORBIDDEN);
    return userId;
  }
}
