package com.example.clubmanagement.utils;

import android.util.Log;

import com.example.clubmanagement.models.Club;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 동아리 정보 관리 매니저 클래스
 * 동아리 CRUD, 가입/탈퇴 등을 담당합니다.
 */
public class ClubManager {
    private static final String TAG = "ClubManager";

    private static ClubManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private ClubManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized ClubManager getInstance() {
        if (instance == null) {
            instance = new ClubManager();
        }
        return instance;
    }

    // ======================== 콜백 인터페이스 ========================

    public interface ClubCallback {
        void onSuccess(Club club);
        void onFailure(Exception e);
    }

    public interface ClubListCallback {
        void onSuccess(List<Club> clubs);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // ======================== 동아리 조회 ========================

    /**
     * 모든 동아리 목록 조회
     */
    public void getAllClubs(ClubListCallback callback) {
        db.collection("clubs")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Club> clubs = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Club club = doc.toObject(Club.class);
                        if (club != null) {
                            club.setId(doc.getId());
                            clubs.add(club);
                        }
                    }
                    callback.onSuccess(clubs);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 중앙동아리 목록만 조회
     */
    public void getCentralClubs(ClubListCallback callback) {
        db.collection("clubs")
                .whereEqualTo("centralClub", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Club> clubs = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Club club = doc.toObject(Club.class);
                        if (club != null) {
                            club.setId(doc.getId());
                            clubs.add(club);
                        }
                    }
                    callback.onSuccess(clubs);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 일반동아리 목록만 조회
     */
    public void getGeneralClubs(ClubListCallback callback) {
        db.collection("clubs")
                .whereEqualTo("centralClub", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Club> clubs = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Club club = doc.toObject(Club.class);
                        if (club != null) {
                            club.setId(doc.getId());
                            clubs.add(club);
                        }
                    }
                    callback.onSuccess(clubs);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 동아리 정보 조회 (ID로)
     */
    public void getClub(String clubId, ClubCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .get(Source.SERVER)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Club club = documentSnapshot.toObject(Club.class);
                        if (club != null) {
                            club.setId(documentSnapshot.getId());
                        }
                        callback.onSuccess(club);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    // 서버 연결 실패 시 캐시에서 시도
                    db.collection("clubs")
                            .document(clubId)
                            .get(Source.CACHE)
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Club club = documentSnapshot.toObject(Club.class);
                                    if (club != null) {
                                        club.setId(documentSnapshot.getId());
                                    }
                                    callback.onSuccess(club);
                                } else {
                                    callback.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(cacheError -> callback.onSuccess(null));
                });
    }

    /**
     * 동아리가 존재하는지 확인
     */
    public void checkClubExists(String clubId, ClubExistsCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .get()
                .addOnSuccessListener(doc -> callback.onResult(doc.exists()))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public interface ClubExistsCallback {
        void onResult(boolean exists);
    }

    // ======================== 동아리 저장/수정 ========================

    /**
     * 동아리 정보 저장/수정
     */
    public void saveClub(Club club, ClubCallback callback) {
        if (club.getId() == null || club.getId().isEmpty()) {
            callback.onFailure(new Exception("Club ID cannot be null or empty"));
            return;
        }

        club.setUpdatedAt(Timestamp.now());
        if (club.getCreatedAt() == null) {
            club.setCreatedAt(Timestamp.now());
        }

        db.collection("clubs")
                .document(club.getId())
                .set(club)
                .addOnSuccessListener(aVoid -> callback.onSuccess(club))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 동아리 정보 부분 업데이트
     */
    public void updateClub(String clubId, Map<String, Object> updates, SimpleCallback callback) {
        updates.put("updatedAt", Timestamp.now());

        db.collection("clubs")
                .document(clubId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 동아리 가입/탈퇴 ========================

    /**
     * 중앙동아리 가입
     */
    public void joinCentralClub(String clubId, String clubName, SimpleCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onSuccess();
            return;
        }

        String joinDate = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(new Date());

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", firebaseUser.getUid());
        userData.put("email", firebaseUser.getEmail());
        userData.put("centralClubId", clubId);
        userData.put("centralClubName", clubName);
        userData.put("joinDate", joinDate);

        db.collection("users")
                .document(firebaseUser.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    addMemberToClub(clubId, firebaseUser.getUid(), firebaseUser.getEmail(), joinDate, callback);
                })
                .addOnFailureListener(e -> callback.onSuccess());
    }

    /**
     * 중앙동아리 탈퇴
     */
    public void leaveCentralClub(SimpleCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new Exception("로그인되지 않았습니다"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("centralClubId", FieldValue.delete());
        updates.put("centralClubName", FieldValue.delete());

        db.collection("users")
                .document(firebaseUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 일반동아리 가입
     */
    public void joinGeneralClub(String clubId, String clubName, SimpleCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new Exception("로그인되지 않았습니다"));
            return;
        }

        String joinDate = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("generalClubIds", FieldValue.arrayUnion(clubId));
        updates.put("generalClubNames", FieldValue.arrayUnion(clubName));

        db.collection("users")
                .document(firebaseUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    addMemberToClub(clubId, firebaseUser.getUid(), firebaseUser.getEmail(), joinDate, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 일반동아리 탈퇴
     */
    public void leaveGeneralClub(String clubId, SimpleCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new Exception("로그인되지 않았습니다"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("generalClubIds", FieldValue.arrayRemove(clubId));

        db.collection("users")
                .document(firebaseUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 동아리 멤버 서브컬렉션에 멤버 추가
     */
    private void addMemberToClub(String clubId, String userId, String email, String joinDate, SimpleCallback callback) {
        Map<String, Object> memberData = new HashMap<>();
        memberData.put("userId", userId);
        memberData.put("email", email);
        memberData.put("joinDate", joinDate);
        memberData.put("role", "MEMBER");

        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .set(memberData)
                .addOnSuccessListener(aVoid -> {
                    incrementMemberCount(clubId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onSuccess());
    }

    /**
     * 멤버 수 증가
     */
    private void incrementMemberCount(String clubId) {
        db.collection("clubs")
                .document(clubId)
                .update("memberCount", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to increment member count: " + e.getMessage()));
    }

    /**
     * 멤버 수 감소
     */
    private void decrementMemberCount(String clubId) {
        db.collection("clubs")
                .document(clubId)
                .update("memberCount", FieldValue.increment(-1))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to decrement member count: " + e.getMessage()));
    }

    // ======================== 가입 신청 기간 관리 ========================

    /**
     * 가입 신청 가능 여부 확인
     */
    public void canApplyNow(String clubId, CanApplyCallback callback) {
        getClub(clubId, new ClubCallback() {
            @Override
            public void onSuccess(Club club) {
                if (club != null) {
                    callback.onResult(club.canApplyNow(), club.getApplicationStatusMessage());
                } else {
                    callback.onResult(false, "동아리를 찾을 수 없습니다");
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onResult(false, "오류가 발생했습니다");
            }
        });
    }

    public interface CanApplyCallback {
        void onResult(boolean canApply, String message);
    }

    /**
     * 가입 신청 기간 설정
     */
    public void setApplicationPeriod(String clubId, Timestamp startDate, Timestamp endDate, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("applicationStartDate", startDate);
        updates.put("applicationEndDate", endDate);
        updates.put("applicationOpen", true);
        updates.put("updatedAt", Timestamp.now());

        db.collection("clubs")
                .document(clubId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 가입 신청 열기/닫기 토글
     */
    public void toggleApplicationOpen(String clubId, boolean isOpen, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("applicationOpen", isOpen);
        updates.put("updatedAt", Timestamp.now());

        db.collection("clubs")
                .document(clubId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 동아리 삭제 ========================

    /**
     * 동아리 삭제 (문서만)
     */
    public void deleteClub(String clubId, SimpleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 유틸리티 ========================

    /**
     * 동아리 이름으로 ID 생성
     */
    public static String generateClubId(String clubName) {
        return clubName.replaceAll("\\s+", "_").toLowerCase();
    }
}
