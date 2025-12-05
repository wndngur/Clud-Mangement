package com.example.clubmanagement.utils;

import android.util.Log;

import com.example.clubmanagement.models.Member;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 동아리 멤버 관리 매니저 클래스
 * 멤버 조회, 추가, 삭제, 역할 변경 등을 담당합니다.
 */
public class MemberManager {
    private static final String TAG = "MemberManager";

    private static MemberManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private MemberManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized MemberManager getInstance() {
        if (instance == null) {
            instance = new MemberManager();
        }
        return instance;
    }

    // ======================== 콜백 인터페이스 ========================

    public interface MemberCallback {
        void onSuccess(Member member);
        void onFailure(Exception e);
    }

    public interface MembersCallback {
        void onSuccess(List<Member> members);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface MembershipCheckCallback {
        void onResult(boolean isMember, String memberType);
    }

    // ======================== 멤버 조회 ========================

    /**
     * 동아리 멤버 목록 조회
     */
    public void getClubMembers(String clubId, MembersCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Member> members = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Member member = doc.toObject(Member.class);
                        if (member != null) {
                            member.setUserId(doc.getId());
                            members.add(member);
                        }
                    }
                    callback.onSuccess(members);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 특정 멤버 조회
     */
    public void getMember(String clubId, String userId, MemberCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Member member = doc.toObject(Member.class);
                        if (member != null) {
                            member.setUserId(doc.getId());
                        }
                        callback.onSuccess(member);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 현재 사용자가 동아리 멤버인지 확인
     */
    public void checkMembership(String clubId, MembershipCheckCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onResult(false, null);
            return;
        }

        String userId = user.getUid();

        // users 컬렉션에서 확인
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onResult(false, null);
                        return;
                    }

                    String centralClubId = doc.getString("centralClubId");
                    List<String> generalClubIds = (List<String>) doc.get("generalClubIds");

                    if (clubId.equals(centralClubId)) {
                        callback.onResult(true, "central");
                    } else if (generalClubIds != null && generalClubIds.contains(clubId)) {
                        callback.onResult(true, "general");
                    } else {
                        callback.onResult(false, null);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(false, null));
    }

    // ======================== 멤버 추가 ========================

    /**
     * 멤버 추가 (기본)
     */
    public void addMember(String clubId, Member member, SimpleCallback callback) {
        if (member.getJoinDate() == null || member.getJoinDate().isEmpty()) {
            member.setJoinDate(new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(new Date()));
        }

        if (member.getRole() == null || member.getRole().isEmpty()) {
            member.setRole("회원");
        }

        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(member.getUserId())
                .set(member)
                .addOnSuccessListener(aVoid -> {
                    incrementMemberCount(clubId);
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 멤버 추가 (서명 및 생일 포함)
     */
    public void addMemberWithDetails(String clubId, String userId, String signatureUrl,
                                      int birthMonth, int birthDay, SimpleCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure(new Exception("로그인되지 않았습니다"));
            return;
        }

        String joinDate = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(new Date());

        Map<String, Object> memberData = new HashMap<>();
        memberData.put("userId", userId);
        memberData.put("email", user.getEmail());
        memberData.put("joinDate", joinDate);
        memberData.put("role", "회원");
        memberData.put("joinedAt", Timestamp.now());

        if (signatureUrl != null && !signatureUrl.isEmpty()) {
            memberData.put("signatureUrl", signatureUrl);
        }

        if (birthMonth > 0 && birthDay > 0) {
            memberData.put("birthMonth", birthMonth);
            memberData.put("birthDay", birthDay);
        }

        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .set(memberData)
                .addOnSuccessListener(aVoid -> {
                    incrementMemberCount(clubId);
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 멤버 수정 ========================

    /**
     * 멤버 정보 업데이트
     */
    public void updateMember(String clubId, String userId, Map<String, Object> updates, SimpleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 멤버 역할 변경
     */
    public void updateMemberRole(String clubId, String userId, String newRole, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("role", newRole);

        updateMember(clubId, userId, updates, callback);
    }

    /**
     * 멤버 관리자 권한 변경
     */
    public void setMemberAdmin(String clubId, String userId, boolean isAdmin, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAdmin", isAdmin);

        updateMember(clubId, userId, updates, callback);
    }

    /**
     * 멤버 생일 업데이트
     */
    public void updateMemberBirthday(String clubId, String userId, int birthMonth, int birthDay, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("birthMonth", birthMonth);
        updates.put("birthDay", birthDay);

        updateMember(clubId, userId, updates, callback);
    }

    // ======================== 멤버 삭제 ========================

    /**
     * 멤버 삭제
     */
    public void removeMember(String clubId, String userId, SimpleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    decrementMemberCount(clubId);
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 생일 관련 ========================

    /**
     * 오늘 생일인 멤버 조회
     */
    public void getTodayBirthdayMembers(String clubId, MembersCallback callback) {
        Calendar today = Calendar.getInstance();
        int todayMonth = today.get(Calendar.MONTH) + 1;
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .whereEqualTo("birthMonth", todayMonth)
                .whereEqualTo("birthDay", todayDay)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Member> members = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Member member = doc.toObject(Member.class);
                        if (member != null) {
                            member.setUserId(doc.getId());
                            members.add(member);
                        }
                    }
                    callback.onSuccess(members);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 이번 달 생일인 멤버 조회
     */
    public void getThisMonthBirthdayMembers(String clubId, MembersCallback callback) {
        Calendar today = Calendar.getInstance();
        int currentMonth = today.get(Calendar.MONTH) + 1;

        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .whereEqualTo("birthMonth", currentMonth)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Member> members = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Member member = doc.toObject(Member.class);
                        if (member != null) {
                            member.setUserId(doc.getId());
                            members.add(member);
                        }
                    }
                    // 생일 일자 기준 정렬
                    members.sort((m1, m2) -> Integer.compare(m1.getBirthDay(), m2.getBirthDay()));
                    callback.onSuccess(members);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 유틸리티 ========================

    private void incrementMemberCount(String clubId) {
        db.collection("clubs")
                .document(clubId)
                .update("memberCount", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to increment member count: " + e.getMessage()));
    }

    private void decrementMemberCount(String clubId) {
        db.collection("clubs")
                .document(clubId)
                .update("memberCount", FieldValue.increment(-1))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to decrement member count: " + e.getMessage()));
    }
}
