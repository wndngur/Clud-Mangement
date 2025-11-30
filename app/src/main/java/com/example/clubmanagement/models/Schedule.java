package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Schedule {
    private String id;
    private String clubId;
    private String title;
    private String description;
    private Timestamp eventDate;
    private Timestamp createdAt;
    private String createdBy;
    private String color; // 일정 표시 색상

    // 기본 생성자 (Firebase 필수)
    public Schedule() {
    }

    public Schedule(String clubId, String title, Timestamp eventDate) {
        this.clubId = clubId;
        this.title = title;
        this.eventDate = eventDate;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClubId() {
        return clubId;
    }

    public void setClubId(String clubId) {
        this.clubId = clubId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getEventDate() {
        return eventDate;
    }

    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    // D-day 계산 메서드
    public int getDday() {
        if (eventDate == null) {
            return 0;
        }

        // 오늘 날짜 (시간 제외)
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // 이벤트 날짜 (시간 제외)
        Calendar eventCal = Calendar.getInstance();
        eventCal.setTime(eventDate.toDate());
        eventCal.set(Calendar.HOUR_OF_DAY, 0);
        eventCal.set(Calendar.MINUTE, 0);
        eventCal.set(Calendar.SECOND, 0);
        eventCal.set(Calendar.MILLISECOND, 0);

        long diffInMillis = eventCal.getTimeInMillis() - today.getTimeInMillis();
        return (int) TimeUnit.MILLISECONDS.toDays(diffInMillis);
    }

    // D-day 문자열 반환
    public String getDdayString() {
        int dday = getDday();
        if (dday == 0) {
            return "D-Day";
        } else if (dday > 0) {
            return "D-" + dday;
        } else {
            return "D+" + Math.abs(dday);
        }
    }

    // 이벤트가 지났는지 확인
    public boolean isPast() {
        return getDday() < 0;
    }

    // 이벤트가 오늘인지 확인
    public boolean isToday() {
        return getDday() == 0;
    }

    // 이벤트 날짜의 년, 월, 일 반환
    public int getYear() {
        if (eventDate == null) return 0;
        Calendar cal = Calendar.getInstance();
        cal.setTime(eventDate.toDate());
        return cal.get(Calendar.YEAR);
    }

    public int getMonth() {
        if (eventDate == null) return 0;
        Calendar cal = Calendar.getInstance();
        cal.setTime(eventDate.toDate());
        return cal.get(Calendar.MONTH);
    }

    public int getDay() {
        if (eventDate == null) return 0;
        Calendar cal = Calendar.getInstance();
        cal.setTime(eventDate.toDate());
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    // 날짜 문자열 반환 (yyyy.MM.dd 형식)
    public String getDateString() {
        if (eventDate == null) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA);
        return sdf.format(eventDate.toDate());
    }
}
