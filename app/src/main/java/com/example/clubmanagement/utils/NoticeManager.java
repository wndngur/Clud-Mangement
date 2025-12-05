package com.example.clubmanagement.utils;

import android.util.Log;

import com.example.clubmanagement.models.ClubNotice;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 공지사항 관리 매니저 클래스
 * 공지사항 CRUD, 댓글, 조회수 등을 담당합니다.
 */
public class NoticeManager {
    private static final String TAG = "NoticeManager";

    private static NoticeManager instance;
    private final FirebaseFirestore db;

    private NoticeManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized NoticeManager getInstance() {
        if (instance == null) {
            instance = new NoticeManager();
        }
        return instance;
    }

    // ======================== 콜백 인터페이스 ========================

    public interface NoticeCallback {
        void onSuccess(ClubNotice notice);
        void onFailure(Exception e);
    }

    public interface NoticeListCallback {
        void onSuccess(List<ClubNotice> notices);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // ======================== 공지사항 조회 ========================

    /**
     * 동아리 공지사항 목록 조회
     */
    public void getNotices(String clubId, NoticeListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClubNotice> notices = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ClubNotice notice = doc.toObject(ClubNotice.class);
                        if (notice != null) {
                            notice.setId(doc.getId());
                            notices.add(notice);
                        }
                    }
                    // 고정 공지사항을 맨 위로 정렬
                    notices.sort((n1, n2) -> {
                        if (n1.isPinned() && !n2.isPinned()) return -1;
                        if (!n1.isPinned() && n2.isPinned()) return 1;
                        return 0;
                    });
                    callback.onSuccess(notices);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 고정 공지사항만 조회
     */
    public void getPinnedNotices(String clubId, NoticeListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .whereEqualTo("isPinned", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClubNotice> notices = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ClubNotice notice = doc.toObject(ClubNotice.class);
                        if (notice != null) {
                            notice.setId(doc.getId());
                            notices.add(notice);
                        }
                    }
                    callback.onSuccess(notices);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 특정 공지사항 조회
     */
    public void getNotice(String clubId, String noticeId, NoticeCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        ClubNotice notice = doc.toObject(ClubNotice.class);
                        if (notice != null) {
                            notice.setId(doc.getId());
                        }
                        callback.onSuccess(notice);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 최근 공지사항 N개 조회
     */
    public void getRecentNotices(String clubId, int limit, NoticeListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ClubNotice> notices = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ClubNotice notice = doc.toObject(ClubNotice.class);
                        if (notice != null) {
                            notice.setId(doc.getId());
                            notices.add(notice);
                        }
                    }
                    callback.onSuccess(notices);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 공지사항 생성 ========================

    /**
     * 공지사항 생성
     */
    public void createNotice(ClubNotice notice, SimpleCallback callback) {
        String noticeId = db.collection("clubs")
                .document(notice.getClubId())
                .collection("notices")
                .document()
                .getId();

        notice.setId(noticeId);
        notice.setCreatedAt(Timestamp.now());
        notice.setUpdatedAt(Timestamp.now());

        db.collection("clubs")
                .document(notice.getClubId())
                .collection("notices")
                .document(noticeId)
                .set(notice)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 공지사항 생성 (알림 포함)
     */
    public void createNoticeWithNotification(ClubNotice notice, String clubName, SimpleCallback callback) {
        createNotice(notice, new SimpleCallback() {
            @Override
            public void onSuccess() {
                // 멤버들에게 알림 생성 로직 (FirebaseManager에 위임)
                callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    // ======================== 공지사항 수정 ========================

    /**
     * 공지사항 수정
     */
    public void updateNotice(ClubNotice notice, SimpleCallback callback) {
        notice.setUpdatedAt(Timestamp.now());

        db.collection("clubs")
                .document(notice.getClubId())
                .collection("notices")
                .document(notice.getId())
                .set(notice)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 공지사항 부분 수정
     */
    public void updateNoticeFields(String clubId, String noticeId, Map<String, Object> updates, SimpleCallback callback) {
        updates.put("updatedAt", Timestamp.now());

        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 공지사항 고정/해제
     */
    public void togglePinned(String clubId, String noticeId, boolean isPinned, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isPinned", isPinned);
        updates.put("updatedAt", Timestamp.now());

        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 공지사항 삭제 ========================

    /**
     * 공지사항 삭제
     */
    public void deleteNotice(String clubId, String noticeId, SimpleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 조회수 ========================

    /**
     * 조회수 증가
     */
    public void incrementViewCount(String clubId, String noticeId) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .update("viewCount", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to increment view count: " + e.getMessage()));
    }

    // ======================== 댓글 수 ========================

    /**
     * 댓글 수 증가
     */
    public void incrementCommentCount(String clubId, String noticeId) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .update("commentCount", FieldValue.increment(1))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to increment comment count: " + e.getMessage()));
    }

    /**
     * 댓글 수 감소
     */
    public void decrementCommentCount(String clubId, String noticeId) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .update("commentCount", FieldValue.increment(-1))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to decrement comment count: " + e.getMessage()));
    }

    // ======================== 전체 공지 ========================

    /**
     * 전체 동아리에 공지사항 생성
     */
    public void createGlobalNotice(ClubNotice notice, SimpleCallback callback) {
        // 모든 동아리 목록 조회 후 각각에 공지사항 생성
        db.collection("clubs")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    final int[] remaining = {querySnapshot.size()};
                    final boolean[] hasError = {false};

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String clubId = doc.getId();
                        String clubName = doc.getString("name");

                        ClubNotice clubNotice = new ClubNotice(
                                clubId,
                                notice.getTitle(),
                                notice.getContent(),
                                notice.getAuthorId(),
                                notice.getAuthorName()
                        );
                        clubNotice.setPinned(notice.isPinned());

                        createNotice(clubNotice, new SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                remaining[0]--;
                                if (remaining[0] == 0 && !hasError[0]) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                hasError[0] = true;
                                remaining[0]--;
                                if (remaining[0] == 0) {
                                    callback.onFailure(e);
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
}
