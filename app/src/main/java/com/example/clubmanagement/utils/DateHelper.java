package com.example.clubmanagement.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 날짜/시간 관련 공통 유틸리티 클래스
 * 일관된 날짜 포맷팅과 파싱을 제공합니다.
 */
public class DateHelper {

    // ======================== 포맷 상수 ========================

    public static final String FORMAT_DATE = "yyyy.MM.dd";
    public static final String FORMAT_DATE_TIME = "yyyy.MM.dd HH:mm";
    public static final String FORMAT_DATE_TIME_SECONDS = "yyyy.MM.dd HH:mm:ss";
    public static final String FORMAT_TIME = "HH:mm";
    public static final String FORMAT_MONTH_DAY = "MM.dd";
    public static final String FORMAT_YEAR_MONTH = "yyyy.MM";
    public static final String FORMAT_FULL_DATE = "yyyy년 MM월 dd일";
    public static final String FORMAT_FULL_DATE_TIME = "yyyy년 MM월 dd일 HH시 mm분";
    public static final String FORMAT_SHORT_DATE = "M/d";
    public static final String FORMAT_DAY_OF_WEEK = "E";

    private static final Locale LOCALE_KOREA = Locale.KOREA;

    // ======================== Date 포맷팅 ========================

    /**
     * Date를 기본 포맷(yyyy.MM.dd)으로 변환
     */
    @NonNull
    public static String formatDate(@Nullable Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(FORMAT_DATE, LOCALE_KOREA).format(date);
    }

    /**
     * Date를 날짜+시간 포맷(yyyy.MM.dd HH:mm)으로 변환
     */
    @NonNull
    public static String formatDateTime(@Nullable Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(FORMAT_DATE_TIME, LOCALE_KOREA).format(date);
    }

    /**
     * Date를 시간만 포맷(HH:mm)으로 변환
     */
    @NonNull
    public static String formatTime(@Nullable Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(FORMAT_TIME, LOCALE_KOREA).format(date);
    }

    /**
     * Date를 월/일 포맷(MM.dd)으로 변환
     */
    @NonNull
    public static String formatMonthDay(@Nullable Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(FORMAT_MONTH_DAY, LOCALE_KOREA).format(date);
    }

    /**
     * Date를 한글 전체 날짜 포맷(yyyy년 MM월 dd일)으로 변환
     */
    @NonNull
    public static String formatFullDate(@Nullable Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(FORMAT_FULL_DATE, LOCALE_KOREA).format(date);
    }

    /**
     * Date를 한글 전체 날짜+시간 포맷(yyyy년 MM월 dd일 HH시 mm분)으로 변환
     */
    @NonNull
    public static String formatFullDateTime(@Nullable Date date) {
        if (date == null) return "";
        return new SimpleDateFormat(FORMAT_FULL_DATE_TIME, LOCALE_KOREA).format(date);
    }

    /**
     * Date를 커스텀 포맷으로 변환
     */
    @NonNull
    public static String format(@Nullable Date date, @NonNull String pattern) {
        if (date == null) return "";
        return new SimpleDateFormat(pattern, LOCALE_KOREA).format(date);
    }

    // ======================== Timestamp 포맷팅 ========================

    /**
     * Timestamp를 기본 포맷(yyyy.MM.dd)으로 변환
     */
    @NonNull
    public static String formatDate(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "";
        return formatDate(timestamp.toDate());
    }

    /**
     * Timestamp를 날짜+시간 포맷(yyyy.MM.dd HH:mm)으로 변환
     */
    @NonNull
    public static String formatDateTime(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "";
        return formatDateTime(timestamp.toDate());
    }

    /**
     * Timestamp를 시간만 포맷(HH:mm)으로 변환
     */
    @NonNull
    public static String formatTime(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "";
        return formatTime(timestamp.toDate());
    }

    /**
     * Timestamp를 한글 전체 날짜 포맷(yyyy년 MM월 dd일)으로 변환
     */
    @NonNull
    public static String formatFullDate(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "";
        return formatFullDate(timestamp.toDate());
    }

    /**
     * Timestamp를 커스텀 포맷으로 변환
     */
    @NonNull
    public static String format(@Nullable Timestamp timestamp, @NonNull String pattern) {
        if (timestamp == null) return "";
        return format(timestamp.toDate(), pattern);
    }

    // ======================== 상대 시간 ========================

    /**
     * 상대 시간 표시 (예: "방금 전", "5분 전", "3일 전")
     */
    @NonNull
    public static String getTimeAgo(@Nullable Date date) {
        if (date == null) return "";

        long diffMillis = System.currentTimeMillis() - date.getTime();
        long diffSeconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis);
        long diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);

        if (diffSeconds < 60) {
            return "방금 전";
        } else if (diffMinutes < 60) {
            return diffMinutes + "분 전";
        } else if (diffHours < 24) {
            return diffHours + "시간 전";
        } else if (diffDays < 7) {
            return diffDays + "일 전";
        } else if (diffDays < 30) {
            return (diffDays / 7) + "주 전";
        } else if (diffDays < 365) {
            return (diffDays / 30) + "개월 전";
        } else {
            return (diffDays / 365) + "년 전";
        }
    }

    /**
     * Timestamp로 상대 시간 표시
     */
    @NonNull
    public static String getTimeAgo(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "";
        return getTimeAgo(timestamp.toDate());
    }

    /**
     * 스마트 날짜 표시 (오늘이면 시간만, 올해면 월/일, 그 외엔 전체 날짜)
     */
    @NonNull
    public static String getSmartDate(@Nullable Date date) {
        if (date == null) return "";

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTime(date);

        if (isSameDay(now, target)) {
            // 오늘이면 시간만 표시
            return formatTime(date);
        } else if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR)) {
            // 같은 해면 월/일만 표시
            return formatMonthDay(date);
        } else {
            // 다른 해면 전체 날짜 표시
            return formatDate(date);
        }
    }

    /**
     * Timestamp로 스마트 날짜 표시
     */
    @NonNull
    public static String getSmartDate(@Nullable Timestamp timestamp) {
        if (timestamp == null) return "";
        return getSmartDate(timestamp.toDate());
    }

    // ======================== 날짜 비교 ========================

    /**
     * 두 날짜가 같은 날인지 확인
     */
    public static boolean isSameDay(@NonNull Date date1, @NonNull Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }

    /**
     * 두 Calendar가 같은 날인지 확인
     */
    public static boolean isSameDay(@NonNull Calendar cal1, @NonNull Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * 오늘인지 확인
     */
    public static boolean isToday(@Nullable Date date) {
        if (date == null) return false;
        return isSameDay(date, new Date());
    }

    /**
     * 어제인지 확인
     */
    public static boolean isYesterday(@Nullable Date date) {
        if (date == null) return false;
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        return isSameDay(date, yesterday.getTime());
    }

    /**
     * 날짜가 기간 내에 있는지 확인
     */
    public static boolean isDateInRange(@NonNull Date date, @Nullable Date startDate, @Nullable Date endDate) {
        long time = date.getTime();

        if (startDate != null && time < startDate.getTime()) {
            return false;
        }
        if (endDate != null && time > endDate.getTime()) {
            return false;
        }
        return true;
    }

    // ======================== 날짜 계산 ========================

    /**
     * 두 날짜 사이의 일수 계산
     */
    public static long getDaysBetween(@NonNull Date date1, @NonNull Date date2) {
        long diffMillis = Math.abs(date2.getTime() - date1.getTime());
        return TimeUnit.MILLISECONDS.toDays(diffMillis);
    }

    /**
     * 오늘부터의 일수 계산
     */
    public static long getDaysFromToday(@NonNull Date date) {
        return getDaysBetween(new Date(), date);
    }

    /**
     * 날짜에 일수 더하기
     */
    @NonNull
    public static Date addDays(@NonNull Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    /**
     * 날짜에 월 더하기
     */
    @NonNull
    public static Date addMonths(@NonNull Date date, int months) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, months);
        return cal.getTime();
    }

    // ======================== 파싱 ========================

    /**
     * 문자열을 Date로 파싱 (yyyy.MM.dd 포맷)
     */
    @Nullable
    public static Date parseDate(@Nullable String dateString) {
        return parseDate(dateString, FORMAT_DATE);
    }

    /**
     * 문자열을 Date로 파싱 (커스텀 포맷)
     */
    @Nullable
    public static Date parseDate(@Nullable String dateString, @NonNull String pattern) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            return new SimpleDateFormat(pattern, LOCALE_KOREA).parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 문자열을 Timestamp로 파싱
     */
    @Nullable
    public static Timestamp parseTimestamp(@Nullable String dateString) {
        Date date = parseDate(dateString);
        return date != null ? new Timestamp(date) : null;
    }

    // ======================== 유틸리티 ========================

    /**
     * 현재 Timestamp 가져오기
     */
    @NonNull
    public static Timestamp now() {
        return Timestamp.now();
    }

    /**
     * 현재 Date 가져오기
     */
    @NonNull
    public static Date today() {
        return new Date();
    }

    /**
     * 하루의 시작 시간 (00:00:00) 가져오기
     */
    @NonNull
    public static Date getStartOfDay(@NonNull Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 하루의 끝 시간 (23:59:59) 가져오기
     */
    @NonNull
    public static Date getEndOfDay(@NonNull Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * 기간 문자열 생성 (예: "2024.01.01 ~ 2024.12.31")
     */
    @NonNull
    public static String formatPeriod(@Nullable Date startDate, @Nullable Date endDate) {
        if (startDate != null && endDate != null) {
            return formatDate(startDate) + " ~ " + formatDate(endDate);
        } else if (startDate != null) {
            return formatDate(startDate) + " ~";
        } else if (endDate != null) {
            return "~ " + formatDate(endDate);
        }
        return "기간 설정 없음";
    }

    /**
     * Timestamp로 기간 문자열 생성
     */
    @NonNull
    public static String formatPeriod(@Nullable Timestamp startDate, @Nullable Timestamp endDate) {
        return formatPeriod(
                startDate != null ? startDate.toDate() : null,
                endDate != null ? endDate.toDate() : null
        );
    }
}
