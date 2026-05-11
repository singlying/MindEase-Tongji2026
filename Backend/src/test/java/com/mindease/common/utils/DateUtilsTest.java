package com.mindease.common.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日期时间工具类单元测试
 * <p>
 * 提供日期解析、格式化、区间计算、年龄推算等通用能力，
 * 服务于预约管理、心情日志归档、报表统计等模块。
 * </p>
 */
@DisplayName("DateUtils 日期时间工具单元测试")
class DateUtilsTest {

    private static final DateTimeFormatter DTF_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DTF_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // --------------------------------------------------------
    // 被测目标方法集
    // --------------------------------------------------------

    private String formatDate(LocalDate date) {
        if (date == null) return null;
        return date.format(DTF_DATE);
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(DTF_DATETIME);
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        return LocalDate.parse(text, DTF_DATE);
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) return null;
        return LocalDateTime.parse(text, DTF_DATETIME);
    }

    private int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return -1;
        return (int) ChronoUnit.YEARS.between(birthDate, LocalDate.now());
    }

    private List<LocalDate> getDateRange(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    private boolean isWeekend(LocalDate date) {
        int dow = date.getDayOfWeek().getValue();  // Mon=1 ... Sun=7
        return dow >= 6;
    }

    private boolean isWorkingDay(LocalDate date) {
        return !isWeekend(date);
    }

    private String getTimeGreeting(LocalDateTime now) {
        int hour = now.getHour();
        if (hour >= 5 && hour < 12) return "早上好";
        if (hour >= 12 && hour < 14) return "中午好";
        if (hour >= 14 && hour < 18) return "下午好";
        if (hour >= 18 && hour < 22) return "晚上好";
        return "夜深了";
    }

    private LocalDateTime getStartOfDay(LocalDate date) {
        return date.atTime(LocalTime.MIN);
    }

    private LocalDateTime getEndOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    private long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    // ==================== 日期格式化测试 ====================

    @Nested
    @DisplayName("日期格式化")
    class DateFormattingTests {

        @Test
        @DisplayName("标准 yyyy-MM-dd 格式输出")
        void standardDateFormat() {
            LocalDate date = LocalDate.of(2025, 6, 15);
            assertEquals("2025-06-15", formatDate(date));
        }

        @Test
        @DisplayName("月份和日补零对齐")
        void zeroPadding() {
            assertEquals("2025-01-05", formatDate(LocalDate.of(2025, 1, 5)));
            assertEquals("2025-12-31", formatDate(LocalDate.of(2025, 12, 31)));
        }

        @Test
        @DisplayName("日期时间完整格式化")
        void dateTimeFormat() {
            LocalDateTime dt = LocalDateTime.of(2025, 3, 20, 14, 30, 0);
            assertEquals("2025-03-20 14:30:00", formatDateTime(dt));
        }

        @Test
        @DisplayName("午夜零点时刻格式化")
        void midnightFormat() {
            LocalDateTime dt = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
            assertEquals("2025-01-01 00:00:00", formatDateTime(dt));
        }

        @Test
        @DisplayName("null 输入安全返回 null")
        void nullSafety() {
            assertNull(formatDate(null));
            assertNull(formatDateTime(null));
        }
    }

    // ==================== 日期解析测试 ====================

    @Nested
    @DisplayName("日期字符串解析")
    class DateParsingTests {

        @Test
        @DisplayName("正确格式的日期字符串可解析")
        void validDateString() {
            LocalDate parsed = parseDate("2025-07-01");
            assertEquals(2025, parsed.getYear());
            assertEquals(7, parsed.getMonthValue());
            assertEquals(1, parsed.getDayOfMonth());
        }

        @Test
        @DisplayName("正确格式的日期时间字符串可解析")
        void validDateTimeString() {
            LocalDateTime dt = parseDateTime("2024-12-25 23:59:59");
            assertEquals(24, dt.getYear());
            assertEquals(12, dt.getMonthValue());
            assertEquals(25, dt.getDayOfMonth());
            assertEquals(23, dt.getHour());
            assertEquals(59, dt.getMinute());
            assertEquals(59, dt.getSecond());
        }

        @Test
        @DisplayName("非法格式字符串抛出异常")
        void invalidFormatThrows() {
            assertThrows(DateTimeParseException.class, () -> parseDate("2025/06/15"));
            assertThrows(DateTimeParseException.class, () -> parseDateTime("2025-06-15T10:00:00"));
        }

        @Test
        @DisplayName("空字符串和 null 返回 null")
        void blankOrNullReturnsNull() {
            assertNull(parseDate(""));
            assertNull(parseDate("  "));
            assertNull(parseDate(null));
            assertNull(parseDateTime(null));
        }
    }

    // ==================== 年龄计算测试 ====================

    @Nested
    @DisplayName("年龄计算")
    class AgeCalculationTests {

        @Test
        @DisplayName("已知出生日期计算年龄合理范围")
        void reasonableAgeRange() {
            // 大约 30 年前的生日
            LocalDate birth = LocalDate.now().minusYears(30);
            int age = calculateAge(birth);
            assertTrue(age >= 29 && age <= 31, "预期约 30 岁, 实际: " + age);
        }

        @Test
        @DisplayName("刚出生婴儿年龄为 0")
        void newbornAgeZero() {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            assertEquals(0, calculateAge(yesterday));
        }

        @Test
        @DisplayName("未来日期作为生日返回负值表示无效")
        void futureBirthDateNegativeAge() {
            LocalDate future = LocalDate.now().plusDays(365);
            assertTrue(calculateAge(future) < 0);
        }

        @Test
        @DisplayName("null 生日返回 -1 错误码")
        void nullBirthReturnsErrorCode() {
            assertEquals(-1, calculateAge(null));
        }
    }

    // ==================== 日期区间测试 ====================

    @Nested
    @DisplayName("日期区间枚举")
    class DateRangeTests {

        @Test
        @DisplayName("同一天区间仅包含当天")
        void singleDayRange() {
            LocalDate d = LocalDate.of(2025, 6, 1);
            List<LocalDate> range = getDateRange(d, d);
            assertEquals(1, range.size());
            assertEquals(d, range.get(0));
        }

        @Test
        @DisplayName("跨周区间天数正确")
        void weekRange() {
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = LocalDate.of(2025, 6, 7);
            List<LocalDate> range = getDateRange(start, end);
            assertEquals(7, range.size());
            assertEquals(start, range.getFirst());
            assertEquals(end, range.getLast());
        }

        @Test
        @DisplayName("起止顺序颠倒时区间为空")
        void reversedRangeIsEmpty() {
            LocalDate later = LocalDate.of(2025, 6, 30);
            LocalDate earlier = LocalDate.of(2025, 6, 1);
            List<LocalDate> range = getDateRange(later, earlier);
            assertTrue(range.isEmpty(), "结束早于开始时应为空集合");
        }

        @Test
        @DisplayName("跨月区间正确跨越月份边界")
        void crossMonthBoundary() {
            LocalDate start = LocalDate.of(2025, 5, 30);
            LocalDate end = LocalDate.of(2025, 6, 2);
            List<LocalDate> range = getDateRange(start, end);
            assertEquals(4, range.size());
        }
    }

    // ==================== 工作日/周末判断 ====================

    @Nested
    @DisplayName("工作日与周末判断")
    class WeekdayJudgmentTests {

        @Test
        @DisplayName("周六周日判定为周末")
        void saturdayAndSundayAreWeekends() {
            // 2025-06-14 是 Saturday, 2025-06-15 是 Sunday
            assertTrue(isWeekend(LocalDate.of(2025, 6, 14)));
            assertTrue(isWeekend(LocalDate.of(2025, 6, 15)));
        }

        @Test
        @DisplayName("周一至周五判定为工作日")
        void mondayToFridayAreWorkdays() {
            // 2025-06-09 ~ 2025-06-13 是 Mon~Fri
            for (int d = 9; d <= 13; d++) {
                LocalDate date = LocalDate.of(2025, 6, d);
                assertTrue(isWorkingDay(date), "6月" + d + "日应为工作日");
            }
        }

        @Test
        @DisplayName("工作日与周末判断互斥")
        void mutualExclusivity() {
            LocalDate sat = LocalDate.of(2025, 6, 14);
            LocalDate mon = LocalDate.of(2025, 6, 9);
            assertTrue(isWeekend(sat) ^ isWorkingDay(sat));  // XOR
            assertTrue(isWeekend(mon) ^ isWorkingDay(mon));
        }
    }

    // ==================== 时间问候语测试 ====================

    @Nested
    @DisplayName("时间段问候语")
    class TimeGreetingTests {

        @Test
        @DisplayName("早晨时段问候语")
        void morningGreeting() {
            assertEquals("早上好", getTimeGreeting(LocalDateTime.of(2025, 6, 15, 8, 0)));
            assertEquals("早上好", getTimeGreeting(LocalDateTime.of(2025, 6, 15, 11, 59)));
        }

        @Test
        @DisplayName("中午时段问候语")
        void noonGreeting() {
            assertEquals("中午好", getTimeGreeting(LocalDateTime.of(2025, 6, 15, 12, 0)));
            assertEquals("中午好", getTimeGreeting(LocalDateTime.of(2025, 6, 15, 13, 59)));
        }

        @Test
        @DisplayName("下午时段问候语")
        void afternoonGreeting() {
            assertEquals("下午好", getTimeGreeting(LocalDateTime.of(2025, 6, 15, 15, 30)));
        }

        @Test
        @DisplayName("晚上时段问候语")
        void eveningGreeting() {
            assertEquals("晚上好", getTimeGreeting(LocalDateTime.of(2025, 6, 15, 19, 0)));
        }

        @Test
        @DisplayName("深夜时段问候语")
        void lateNightGreeting() {
            assertEquals("夜深了", getTimeGreeting(LocalDateTime.of(2025, 6, 15, 2, 0)));
            assertEquals("夜深了", getTimeGreeting(LocalDateTime.of(2025, 6, 15, 23, 0)));
        }
    }

    // ==================== 当天起止时间测试 ====================

    @Nested
    @DisplayName("当日起止时刻")
    class DayBoundaryTests {

        @Test
        @DisplayName("当天起始时间为 00:00:00")
        void startOfDayIsMidnight() {
            LocalDateTime start = getStartOfDay(LocalDate.of(2025, 7, 1));
            assertEquals(0, start.getHour());
            assertEquals(0, start.getMinute());
            assertEquals(0, start.getSecond());
        }

        @Test
        @DisplayName("当天结束时间为 23:59:59")
        void endOfDayIsLastMoment() {
            LocalDateTime end = getEndOfDay(LocalDate.of(2025, 7, 1));
            assertEquals(23, end.getHour());
            assertEquals(59, end.getMinute());
            assertEquals(59, end.getSecond());
        }

        @Test
        @DisplayName("同一天的起止间隔接近 24 小时")
        void daySpanApproximately24Hours() {
            LocalDate date = LocalDate.of(2025, 8, 15);
            long seconds =ChronoUnit.SECONDS.between(getStartOfDay(date), getEndOfDay(date));
            assertEquals(24 * 3600 - 1, seconds);  // 86399 秒
        }
    }

    // ==================== 天数差计算 ====================

    @Nested
    @DisplayName("日期间隔天数计算")
    class DaysBetweenTests {

        @Test
        @DisplayName("相邻两天相差 1 天")
        void adjacentDaysDifference() {
            assertEquals(1, daysBetween(
                    LocalDate.of(2025, 6, 1),
                    LocalDate.of(2025, 6, 2)
            ));
        }

        @Test
        @DisplayName("同一天相差 0 天")
        void sameDayZeroDiff() {
            assertEquals(0, daysBetween(
                    LocalDate.of(2025, 6, 15),
                    LocalDate.of(2025, 6, 15)
            ));
        }

        @Test
        @DisplayName("跨年大跨度天数计算")
        void crossYearSpan() {
            long diff = daysBetween(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2025, 1, 1)
            );
            assertEquals(366, diff, "2024 是闰年");
        }
    }
}
