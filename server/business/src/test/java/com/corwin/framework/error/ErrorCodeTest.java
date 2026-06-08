package com.corwin.framework.error;

import com.corwin.datasource.domain.error.DatabaseSourceError;
import com.corwin.jsonfmt.domain.error.JsonFmtError;
import com.corwin.reminder.domain.error.ReminderError;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局错误码校验测试。
 *
 * @author wl0180 2026/3/23
 */
public class ErrorCodeTest {

    @Test
    void should_not_have_duplicate_codes_and_all_codes_should_be_in_range() {
        Class[] errorEnums = new Class[]{BaseError.class, DatabaseSourceError.class, JsonFmtError.class,
                ReminderError.class};

        Map<String, String> codeIndex = new LinkedHashMap<>();

        for (Class<? extends ErrorCode> errorEnumClass : errorEnums) {
            assertTrue(errorEnumClass.isEnum(), errorEnumClass.getName() + " must be enum");

            ErrorCode[] values = errorEnumClass.getEnumConstants();
            assertNotNull(values, errorEnumClass.getName() + " enum constants must not be null");

            for (ErrorCode errorCode : values) {
                String code = errorCode.getCode();
                String owner = errorEnumClass.getSimpleName() + "." + ((Enum<?>) errorCode).name();

                assertNotNull(errorCode.getRange(), owner + " range must not be null");
                assertTrue(errorCode.getRange().contains(code),
                        owner + " code [" + code + "] is out of range [" + errorCode.getRange()
                                .getStart() + "-" + errorCode.getRange().getEnd() + "]");

                String existed = codeIndex.putIfAbsent(code, owner);
                assertNull(existed,
                        "Duplicate error code [" + code + "] found in [" + existed + "] and [" + owner + "]");
            }
        }
    }

    @Test
    void should_not_have_overlapped_ranges() {
        ErrorCodeRanges[] ranges = ErrorCodeRanges.values();

        for (int i = 0; i < ranges.length; i++) {
            for (int j = i + 1; j < ranges.length; j++) {
                ErrorCodeRanges a = ranges[i];
                ErrorCodeRanges b = ranges[j];

                boolean overlapped = a.getStart().compareTo(b.getEnd()) <= 0 && b.getStart().compareTo(a.getEnd()) <= 0;

                assertFalse(overlapped,
                        "Error code range overlapped: [" + a.getName() + "] " + a.getStart() + "-" + a.getEnd() + " and [" + b.getName() + "] " + b.getStart() + "-" + b.getEnd());
            }
        }
    }

}
