package com.example.clubmanagement.utils;

import android.content.Context;
import android.widget.Toast;

import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.models.Member;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트 데이터 삽입용 유틸리티
 * 컴퓨터공학과 학술동아리 테스트 데이터
 */
public class TestDataInjector {

    private static final String CLUB_ID = "club_lighthouse";
    private static final String CLUB_NAME = "라이트 하우스";

    public static void injectTestData(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. 동아리 정보 생성
        Club club = createTestClub();

        // 2. 동아리 저장
        db.collection("clubs")
                .document(CLUB_ID)
                .set(club)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "동아리 정보 저장 완료", Toast.LENGTH_SHORT).show();

                    // 3. 부원 데이터 저장
                    injectMembers(context, db);

                    // 4. 캐러셀 아이템 생성
                    injectCarouselItem(context, db);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "동아리 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private static void injectCarouselItem(Context context, FirebaseFirestore db) {
        Map<String, Object> carouselItem = new HashMap<>();
        carouselItem.put("position", 0);
        carouselItem.put("title", CLUB_NAME);
        carouselItem.put("description", "IT 기술로 세상을 밝히는 프로그래밍 동아리");
        carouselItem.put("clubId", CLUB_ID);
        carouselItem.put("clubName", CLUB_NAME);
        carouselItem.put("backgroundColor", "#2196F3");

        db.collection("carousel_items")
                .document("carousel_" + CLUB_ID)
                .set(carouselItem)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "캐러셀 아이템 생성 완료", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "캐러셀 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static Club createTestClub() {
        Club club = new Club(CLUB_ID, CLUB_NAME);

        // 기본 정보
        club.setDescription("라이트 하우스는 IT 기술을 통해 세상을 밝히자는 뜻을 가진 프로그래밍 동아리입니다. " +
                "웹/앱 개발, 인공지능, 데이터 분석 등 다양한 분야의 프로젝트를 진행하며, " +
                "정기적인 스터디와 해커톤 참여를 통해 실력을 키워가고 있습니다.");
        club.setPurpose("IT 기술을 활용한 사회 공헌 및 개발 역량 강화");
        club.setLocation("공학관 A동 401호");
        club.setProfessor("이영희 교수");

        // 설립일 (6개월 전으로 설정 - 중앙동아리 신청 가능)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -8);
        club.setFoundedAt(new Timestamp(cal.getTime()));

        // 예산 정보
        club.setTotalBudget(2000000);  // 총 예산 200만원
        club.setCurrentBudget(1350000); // 현재 잔액 135만원

        // 인원 정보
        club.setMemberCount(22);
        club.setCentralClub(true);  // 중앙동아리
        club.setApplicationOpen(true);  // 가입 신청 열림

        // 추천 시스템 키워드
        club.setChristian(false);
        club.setAtmosphere("lively");  // 활기찬 분위기

        List<String> activityTypes = new ArrayList<>();
        activityTypes.add("study");  // 스터디
        club.setActivityTypes(activityTypes);

        List<String> purposes = new ArrayList<>();
        purposes.add("career");   // 취업 준비
        purposes.add("academic"); // 학술
        club.setPurposes(purposes);

        // 월별 행사 일정
        Map<String, String> monthlySchedule = new HashMap<>();
        monthlySchedule.put("1", "겨울방학 알고리즘 스터디");
        monthlySchedule.put("2", "신입생 모집, 개강 파티");
        monthlySchedule.put("3", "신입생 환영회, OT");
        monthlySchedule.put("4", "봄 MT");
        monthlySchedule.put("5", "중간고사 스터디");
        monthlySchedule.put("6", "기말고사 스터디, 1학기 종강 파티");
        monthlySchedule.put("7", "여름방학 프로젝트 시작");
        monthlySchedule.put("8", "프로젝트 발표회");
        monthlySchedule.put("9", "2학기 개강, 신입 부원 모집");
        monthlySchedule.put("10", "가을 해커톤 참가");
        monthlySchedule.put("11", "기술 세미나");
        monthlySchedule.put("12", "송년회, 2학기 종강 파티");
        club.setMonthlySchedule(monthlySchedule);

        return club;
    }

    private static void injectMembers(Context context, FirebaseFirestore db) {
        // 테스트 부원 목록 (22명)
        List<Member> members = new ArrayList<>();

        // 임원진
        members.add(createMember("user_001", "김태양", "20210001", "컴퓨터공학과", "010-1234-5678", "회장", true));
        members.add(createMember("user_002", "박하늘", "20210015", "컴퓨터공학과", "010-2345-6789", "부회장", true));
        members.add(createMember("user_003", "이별빛", "20210023", "컴퓨터공학과", "010-3456-7890", "총무", true));
        members.add(createMember("user_004", "정새벽", "20210042", "컴퓨터공학과", "010-4567-8901", "회계", true));

        // 일반 부원들
        members.add(createMember("user_005", "최은하", "20220005", "컴퓨터공학과", "010-5678-9012", "회원", false));
        members.add(createMember("user_006", "강우주", "20220018", "컴퓨터공학과", "010-6789-0123", "회원", false));
        members.add(createMember("user_007", "윤빛나", "20220031", "소프트웨어학과", "010-7890-1234", "회원", false));
        members.add(createMember("user_008", "송달빛", "20220044", "컴퓨터공학과", "010-8901-2345", "회원", false));
        members.add(createMember("user_009", "임솔빛", "20220057", "정보통신공학과", "010-9012-3456", "회원", false));
        members.add(createMember("user_010", "한나래", "20230003", "컴퓨터공학과", "010-0123-4567", "회원", false));
        members.add(createMember("user_011", "오늘찬", "20230016", "컴퓨터공학과", "010-1111-2222", "회원", false));
        members.add(createMember("user_012", "배하람", "20230029", "소프트웨어학과", "010-2222-3333", "회원", false));
        members.add(createMember("user_013", "신아람", "20230042", "컴퓨터공학과", "010-3333-4444", "회원", false));
        members.add(createMember("user_014", "조미르", "20230055", "정보통신공학과", "010-4444-5555", "회원", false));
        members.add(createMember("user_015", "권세찬", "20230068", "컴퓨터공학과", "010-5555-6666", "회원", false));
        members.add(createMember("user_016", "황빛솔", "20240001", "컴퓨터공학과", "010-6666-7777", "회원", false));
        members.add(createMember("user_017", "양은별", "20240014", "소프트웨어학과", "010-7777-8888", "회원", false));
        members.add(createMember("user_018", "장하린", "20240027", "컴퓨터공학과", "010-8888-9999", "회원", false));
        members.add(createMember("user_019", "류세빈", "20240040", "정보통신공학과", "010-9999-0000", "회원", false));
        members.add(createMember("user_020", "노찬우", "20240053", "컴퓨터공학과", "010-1212-3434", "회원", false));
        members.add(createMember("user_021", "문가온", "20240066", "컴퓨터공학과", "010-5656-7878", "회원", false));
        members.add(createMember("user_022", "서라온", "20240079", "소프트웨어학과", "010-9090-1212", "회원", false));

        // 각 부원을 Firebase에 저장
        int[] successCount = {0};
        int totalCount = members.size();

        for (Member member : members) {
            db.collection("clubs")
                    .document(CLUB_ID)
                    .collection("members")
                    .document(member.getUserId())
                    .set(member)
                    .addOnSuccessListener(aVoid -> {
                        successCount[0]++;
                        if (successCount[0] == totalCount) {
                            Toast.makeText(context, "테스트 데이터 입력 완료!\n동아리: " + CLUB_NAME + "\n부원: " + totalCount + "명", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "부원 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private static Member createMember(String userId, String name, String studentId,
                                        String department, String phone, String role, boolean isAdmin) {
        Member member = new Member(userId, name, studentId, department, isAdmin);
        member.setPhone(phone);
        member.setRole(role);
        member.setRequestStatus("approved");

        // 가입일을 랜덤하게 설정 (최근 8개월 이내)
        Calendar cal = Calendar.getInstance();
        int daysAgo = (int) (Math.random() * 240);  // 0~240일 전
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        member.setJoinedAt(cal.getTimeInMillis());

        return member;
    }

    /**
     * 기존 테스트 데이터 삭제
     */
    public static void deleteTestData(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 새 ID 삭제
        deleteClubData(db, CLUB_ID);

        // 이전 ID도 삭제 (컴퓨터공학과 학술동아리)
        deleteClubData(db, "club_cse_academic");

        Toast.makeText(context, "테스트 데이터 삭제 완료", Toast.LENGTH_SHORT).show();
    }

    private static void deleteClubData(FirebaseFirestore db, String clubId) {
        // 부원 컬렉션 삭제
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }

                    // 동아리 문서 삭제
                    db.collection("clubs")
                            .document(clubId)
                            .delete();
                });
    }

    public static String getTestClubId() {
        return CLUB_ID;
    }

    public static String getTestClubName() {
        return CLUB_NAME;
    }
}
