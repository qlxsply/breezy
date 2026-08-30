package com.corwin.reminder.application.view;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
public record TodoDailyDetailView(
    LocalDate date, List<TodoView> createdItems, List<TodoView> completedItems) {}
