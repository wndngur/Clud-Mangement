package com.example.clubmanagement.models;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Club {
    private String id;              // Club ID (unique identifier)
    private String name;            // Club name (동아리명)
    private String description;     // Club description
    private String purpose;         // 설립 목적
    private String schedule;        // 행사 일정
    private String location;        // 동아리방 위치
    private String professor;       // 지도 교수
    private String department;      // 학과
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp foundedAt;    // 동아리 설립일

    // 공금 관련 필드
    private long totalBudget;       // 총 예산 (원)
    private long currentBudget;     // 현재 잔액 (원)

    // 인원 관련 필드
    private int memberCount;        // 현재 인원 수
    private boolean isCentralClub;  // 중앙동아리 여부

    // 추천 시스템 키워드 필드
    private boolean isChristian;    // 기독교 동아리 여부
    private String atmosphere;      // 분위기: "lively" (활기찬) / "quiet" (조용한)
    private List<String> activityTypes;  // 활동 유형: volunteer, sports, outdoor
    private List<String> purposes;       // 목적: career, academic, art

    // 월별 행사 일정 (1~12월)
    private Map<String, String> monthlySchedule;  // "1" -> "신입생 환영회", "3" -> "MT"

    // 가입 신청 설정
    private boolean applicationOpen = true;  // 가입 신청 받기 여부
    private Timestamp applicationEndDate;    // 가입 신청 마감일 (null이면 무기한)

    // 중앙동아리 유지 최소 인원 (15명 이상)
    public static final int CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS = 15;
    // 중앙동아리 신청 가능 최소 일수 (약 6개월 = 180일)
    public static final int CENTRAL_CLUB_MIN_DAYS = 180;
    // 중앙동아리 신규 등록 최소 인원 (20명 이상)
    public static final int CENTRAL_CLUB_REGISTER_MIN_MEMBERS = 20;
    // 중앙동아리 신청 가능 최소 개월 수 (6개월)
    public static final int CENTRAL_CLUB_MIN_MONTHS = 6;

    // Firebase requires no-argument constructor
    public Club() {
    }

    public Club(String id, String name) {
        this.id = id;
        this.name = name;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    public Club(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getLocation() {
        return location;
    }

    public String getProfessor() {
        return professor;
    }

    public String getDepartment() {
        return department;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setProfessor(String professor) {
        this.professor = professor;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Timestamp getFoundedAt() {
        return foundedAt;
    }

    public void setFoundedAt(Timestamp foundedAt) {
        this.foundedAt = foundedAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 공금 관련 Getters & Setters
    public long getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(long totalBudget) {
        this.totalBudget = totalBudget;
    }

    public long getCurrentBudget() {
        return currentBudget;
    }

    public void setCurrentBudget(long currentBudget) {
        this.currentBudget = currentBudget;
    }

    // 공금 사용 비율 계산 (0.0 ~ 1.0)
    public float getBudgetUsageRatio() {
        if (totalBudget <= 0) return 0f;
        return (float) currentBudget / totalBudget;
    }

    // 공금 사용 퍼센트 계산 (0 ~ 100)
    public int getBudgetUsagePercent() {
        return (int) (getBudgetUsageRatio() * 100);
    }

    // 인원 관련 Getters & Setters
    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public boolean isCentralClub() {
        return isCentralClub;
    }

    public void setCentralClub(boolean centralClub) {
        isCentralClub = centralClub;
    }

    // 중앙동아리 유지 가능 여부 (17명 이상)
    public boolean canMaintainCentralStatus() {
        return memberCount >= CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS;
    }

    // 중앙동아리 신규 등록 가능 여부 (20명 이상)
    public boolean canRegisterAsCentral() {
        return memberCount >= CENTRAL_CLUB_REGISTER_MIN_MEMBERS;
    }

    // 중앙동아리 유지까지 필요한 인원 수
    public int getMembersNeededToMaintain() {
        if (memberCount >= CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS) {
            return 0;
        }
        return CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS - memberCount;
    }

    // 중앙동아리 등록까지 필요한 인원 수
    public int getMembersNeededForCentral() {
        if (memberCount >= CENTRAL_CLUB_REGISTER_MIN_MEMBERS) {
            return 0;
        }
        return CENTRAL_CLUB_REGISTER_MIN_MEMBERS - memberCount;
    }

    // 인원 현황 비율 계산 (중앙동아리: 17명 기준, 일반동아리: 20명 기준)
    public float getMemberRatio(boolean isCentral) {
        int targetMembers = isCentral ? CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS : CENTRAL_CLUB_REGISTER_MIN_MEMBERS;
        if (memberCount >= targetMembers) {
            return 1.0f;
        }
        return (float) memberCount / targetMembers;
    }

    // 인원 현황 퍼센트 계산 (0 ~ 100)
    public int getMemberPercent(boolean isCentral) {
        return (int) (getMemberRatio(isCentral) * 100);
    }

    // 설립일로부터 경과한 일수 계산
    public long getDaysSinceFounding() {
        if (foundedAt == null) return 0;
        long foundedMillis = foundedAt.toDate().getTime();
        long currentMillis = System.currentTimeMillis();
        long diffMillis = currentMillis - foundedMillis;
        return diffMillis / (1000 * 60 * 60 * 24); // 밀리초를 일수로 변환
    }

    // 중앙동아리 신청 가능 여부 (설립 후 6개월 = 180일 이상)
    public boolean canApplyForCentralByDate() {
        return getDaysSinceFounding() >= CENTRAL_CLUB_MIN_DAYS;
    }

    // 중앙동아리 신청까지 남은 일수
    public long getDaysUntilCentralEligible() {
        long daysSinceFounding = getDaysSinceFounding();
        if (daysSinceFounding >= CENTRAL_CLUB_MIN_DAYS) {
            return 0;
        }
        return CENTRAL_CLUB_MIN_DAYS - daysSinceFounding;
    }

    // 설립일 진행률 계산 (0.0 ~ 1.0)
    public float getFoundingProgressRatio() {
        long daysSinceFounding = getDaysSinceFounding();
        if (daysSinceFounding >= CENTRAL_CLUB_MIN_DAYS) {
            return 1.0f;
        }
        return (float) daysSinceFounding / CENTRAL_CLUB_MIN_DAYS;
    }

    // 설립일 진행률 퍼센트 (0 ~ 100)
    public int getFoundingProgressPercent() {
        return (int) (getFoundingProgressRatio() * 100);
    }

    // 추천 시스템 관련 Getters & Setters
    public boolean isChristian() {
        return isChristian;
    }

    public void setChristian(boolean christian) {
        isChristian = christian;
    }

    public String getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(String atmosphere) {
        this.atmosphere = atmosphere;
    }

    public List<String> getActivityTypes() {
        if (activityTypes == null) {
            activityTypes = new ArrayList<>();
        }
        return activityTypes;
    }

    public void setActivityTypes(List<String> activityTypes) {
        this.activityTypes = activityTypes != null ? activityTypes : new ArrayList<>();
    }

    public List<String> getPurposes() {
        if (purposes == null) {
            purposes = new ArrayList<>();
        }
        return purposes;
    }

    public void setPurposes(List<String> purposes) {
        this.purposes = purposes != null ? purposes : new ArrayList<>();
    }

    // 추천 점수 계산 (사용자 선택과 동아리 키워드 매칭)
    public int calculateRecommendScore(boolean wantChristian, String wantAtmosphere,
                                       List<String> wantActivityTypes, List<String> wantPurposes) {
        int score = 0;

        // 기독교 동아리 매칭 (가중치 높음)
        if (wantChristian && isChristian) {
            score += 30;
        }

        // 분위기 매칭
        if (wantAtmosphere != null && wantAtmosphere.equals(atmosphere)) {
            score += 20;
        }

        // 활동 유형 매칭
        if (wantActivityTypes != null && activityTypes != null) {
            for (String type : wantActivityTypes) {
                if (activityTypes.contains(type)) {
                    score += 15;
                }
            }
        }

        // 목적 매칭
        if (wantPurposes != null && purposes != null) {
            for (String purpose : wantPurposes) {
                if (purposes.contains(purpose)) {
                    score += 15;
                }
            }
        }

        return score;
    }

    // 키워드가 설정되어 있는지 확인
    public boolean hasKeywords() {
        return isChristian ||
               (atmosphere != null && !atmosphere.isEmpty()) ||
               (activityTypes != null && !activityTypes.isEmpty()) ||
               (purposes != null && !purposes.isEmpty());
    }

    // 월별 일정 관련 Getters & Setters
    public Map<String, String> getMonthlySchedule() {
        if (monthlySchedule == null) {
            monthlySchedule = new HashMap<>();
        }
        return monthlySchedule;
    }

    public void setMonthlySchedule(Map<String, String> monthlySchedule) {
        this.monthlySchedule = monthlySchedule != null ? monthlySchedule : new HashMap<>();
    }

    // 특정 월의 일정 가져오기
    public String getScheduleForMonth(int month) {
        if (monthlySchedule == null) return "";
        String schedule = monthlySchedule.get(String.valueOf(month));
        return schedule != null ? schedule : "";
    }

    // 특정 월의 일정 설정하기
    public void setScheduleForMonth(int month, String schedule) {
        if (monthlySchedule == null) {
            monthlySchedule = new HashMap<>();
        }
        if (schedule != null && !schedule.trim().isEmpty()) {
            monthlySchedule.put(String.valueOf(month), schedule.trim());
        } else {
            monthlySchedule.remove(String.valueOf(month));
        }
    }

    // 월별 일정이 있는지 확인
    public boolean hasMonthlySchedule() {
        return monthlySchedule != null && !monthlySchedule.isEmpty();
    }

    // 월별 일정을 문자열로 변환 (표시용)
    public String getMonthlyScheduleAsString() {
        if (monthlySchedule == null || monthlySchedule.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String[] monthNames = {"", "1월", "2월", "3월", "4월", "5월", "6월",
                              "7월", "8월", "9월", "10월", "11월", "12월"};
        for (int i = 1; i <= 12; i++) {
            String schedule = monthlySchedule.get(String.valueOf(i));
            if (schedule != null && !schedule.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(monthNames[i]).append(": ").append(schedule);
            }
        }
        return sb.toString();
    }

    // 가입 신청 설정 관련 Getters & Setters
    public boolean isApplicationOpen() {
        return applicationOpen;
    }

    public void setApplicationOpen(boolean applicationOpen) {
        this.applicationOpen = applicationOpen;
    }

    public Timestamp getApplicationEndDate() {
        return applicationEndDate;
    }

    public void setApplicationEndDate(Timestamp applicationEndDate) {
        this.applicationEndDate = applicationEndDate;
    }

    // 현재 가입 신청 가능 여부 확인
    public boolean canApplyNow() {
        // 가입 신청이 닫혀있으면 불가
        if (!applicationOpen) {
            return false;
        }
        // 마감일이 설정되어 있고, 현재 시간이 마감일을 지났으면 불가
        if (applicationEndDate != null) {
            return System.currentTimeMillis() < applicationEndDate.toDate().getTime();
        }
        return true;
    }

    // 마감일까지 남은 일수 (마감일이 없으면 -1)
    public long getDaysUntilApplicationEnd() {
        if (applicationEndDate == null) {
            return -1;
        }
        long endTime = applicationEndDate.toDate().getTime();
        long currentTime = System.currentTimeMillis();
        if (currentTime >= endTime) {
            return 0;
        }
        return (endTime - currentTime) / (1000 * 60 * 60 * 24);
    }

    // 마감일 포맷팅
    public String getFormattedApplicationEndDate() {
        if (applicationEndDate == null) {
            return "무기한";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA);
        return sdf.format(applicationEndDate.toDate());
    }
}
