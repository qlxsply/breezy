package com.corwin.reminder.interfaces.web.res;

import java.time.LocalDate;
import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
public record TodoDailyDetailRes(
        LocalDate date,
        List<TodoRes> createdItems,
        List<TodoRes> completedItems
) {
}
