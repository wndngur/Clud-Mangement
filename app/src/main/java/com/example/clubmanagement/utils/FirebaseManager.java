package com.example.clubmanagement.utils;

import android.net.Uri;
import android.util.Log;

import com.example.clubmanagement.models.SignatureData;
import com.example.clubmanagement.models.DocumentData;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    private static FirebaseManager instance;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;

    private FirebaseManager() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public FirebaseStorage getStorage() {
        return storage;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ========================================
    // Signature Methods
    // ========================================

    public interface SignatureCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    public interface SignatureDataCallback {
        void onSuccess(SignatureData signatureData);
        void onFailure(Exception e);
    }

    /**
     * 서명 이미지를 Storage에 업로드
     */
    public void uploadSignatureImage(byte[] imageData, String userId, String origin, SignatureCallback callback) {
        String fileName = userId + "_" + origin + "_" + System.currentTimeMillis() + ".png";
        StorageReference signatureRef = storage.getReference()
                .child("signatures/" + userId + "/" + fileName);

        signatureRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    signatureRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        updateSignatureData(userId, downloadUrl, origin, new SignatureDataCallback() {
                            @Override
                            public void onSuccess(SignatureData signatureData) {
                                callback.onSuccess(downloadUrl);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(e);
                            }
                        });
                    }).addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Firestore에 서명 데이터 저장/업데이트
     */
    public void updateSignatureData(String userId, String signatureUrl, String origin, SignatureDataCallback callback) {
        Map<String, Object> signatureData = new HashMap<>();

        if ("pad".equals(origin)) {
            signatureData.put("signaturePadUrl", signatureUrl);
        } else if ("image".equals(origin)) {
            signatureData.put("signatureImageUrl", signatureUrl);
        }

        signatureData.put("origin", origin);
        signatureData.put("lastUpdated", Timestamp.now());
        signatureData.put("userId", userId);

        db.collection("signatures")
                .document(userId)
                .set(signatureData)
                .addOnSuccessListener(aVoid -> {
                    getSignatureData(userId, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 서명 데이터 가져오기
     */
    public void getSignatureData(String userId, SignatureDataCallback callback) {
        db.collection("signatures")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        SignatureData signatureData = documentSnapshot.toObject(SignatureData.class);
                        callback.onSuccess(signatureData);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 서명 삭제
     */
    public void deleteSignature(String userId, SignatureDataCallback callback) {
        db.collection("signatures")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Storage에서도 삭제
                    StorageReference userSignaturesRef = storage.getReference()
                            .child("signatures/" + userId);

                    userSignaturesRef.listAll()
                            .addOnSuccessListener(listResult -> {
                                for (StorageReference item : listResult.getItems()) {
                                    item.delete();
                                }
                                callback.onSuccess(null);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Document Methods
    // ========================================

    public interface DocumentCallback {
        void onSuccess(DocumentData documentData);
        void onFailure(Exception e);
    }

    /**
     * 문서 생성
     */
    public void createDocument(DocumentData documentData, DocumentCallback callback) {
        String docId = documentData.getDocId() != null ? documentData.getDocId()
                : db.collection("documents").document().getId();
        documentData.setDocId(docId);

        db.collection("documents")
                .document(docId)
                .set(documentData)
                .addOnSuccessListener(aVoid -> callback.onSuccess(documentData))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 문서 가져오기
     */
    public void getDocument(String docId, DocumentCallback callback) {
        db.collection("documents")
                .document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        DocumentData documentData = documentSnapshot.toObject(DocumentData.class);
                        callback.onSuccess(documentData);
                    } else {
                        callback.onFailure(new Exception("Document not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 문서에 서명 배치 상태 업데이트
     */
    public void updateDocumentSignatureStatus(String docId, boolean signaturePlaced, String pdfUrl, DocumentCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("signaturePlaced", signaturePlaced);
        if (pdfUrl != null) {
            updates.put("pdfUrl", pdfUrl);
        }

        db.collection("documents")
                .document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid -> getDocument(docId, callback))
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Admin & User Role Methods
    // ========================================

    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
        void onFailure(Exception e);
    }

    public interface UserDataCallback {
        void onSuccess(com.example.clubmanagement.models.UserData userData);
        void onFailure(Exception e);
    }

    public interface AdminPasswordCallback {
        void onSuccess(com.example.clubmanagement.models.AdminPassword adminPassword);
        void onFailure(Exception e);
    }

    public interface PasswordVerifyCallback {
        void onSuccess(boolean isValid, String adminLevel, String clubId);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OperationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Check if current user is admin (DEPRECATED - use getUserData instead)
     */
    @Deprecated
    public void isCurrentUserAdmin(AdminCheckCallback callback) {
        getUserData(getCurrentUserId(), new UserDataCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.UserData userData) {
                if (userData != null) {
                    callback.onResult(userData.isAnyAdmin());
                } else {
                    callback.onResult(false);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Get user data
     */
    public void getUserData(String userId, UserDataCallback callback) {
        if (userId == null) {
            callback.onSuccess(null);
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.clubmanagement.models.UserData userData = documentSnapshot.toObject(com.example.clubmanagement.models.UserData.class);
                        callback.onSuccess(userData);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Set user admin level
     */
    public void setUserAdminLevel(String userId, String adminLevel, String clubId, SimpleCallback callback) {
        com.example.clubmanagement.models.UserData userData = new com.example.clubmanagement.models.UserData(userId, adminLevel, clubId);

        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Get admin password for specific level
     */
    public void getAdminPassword(String level, String clubId, AdminPasswordCallback callback) {
        String docId = level.equals("SUPER_ADMIN") ? "super_admin" : "club_admin_" + clubId;

        db.collection("admin_passwords")
                .document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.clubmanagement.models.AdminPassword password = documentSnapshot.toObject(com.example.clubmanagement.models.AdminPassword.class);
                        callback.onSuccess(password);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Verify admin password
     */
    public void verifyAdminPassword(String level, String clubId, String inputPassword, PasswordVerifyCallback callback) {
        getAdminPassword(level, clubId, new AdminPasswordCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.AdminPassword adminPassword) {
                if (adminPassword == null) {
                    // No password set, use default passwords
                    String defaultPassword = level.equals("SUPER_ADMIN") ? "superadmin123" : "clubadmin123";
                    boolean isValid = defaultPassword.equals(inputPassword);
                    callback.onSuccess(isValid, level, clubId);
                } else {
                    // Simple comparison (in production, use proper hashing)
                    boolean isValid = adminPassword.getPassword().equals(inputPassword);
                    callback.onSuccess(isValid, adminPassword.getLevel(), adminPassword.getClubId());
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Set admin password (for super admin to configure)
     */
    public void setAdminPassword(String level, String clubId, String password, SimpleCallback callback) {
        String docId = level.equals("SUPER_ADMIN") ? "super_admin" : "club_admin_" + clubId;
        com.example.clubmanagement.models.AdminPassword adminPassword = new com.example.clubmanagement.models.AdminPassword(docId, level, password, clubId);

        db.collection("admin_passwords")
                .document(docId)
                .set(adminPassword)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Set admin role for a user (DEPRECATED)
     */
    @Deprecated
    public void setAdminRole(String userId, boolean isAdmin, SimpleCallback callback) {
        String adminLevel = isAdmin ? "SUPER_ADMIN" : "NONE";
        setUserAdminLevel(userId, adminLevel, null, callback);
    }

    // ========================================
    // Carousel Methods
    // ========================================

    public interface CarouselCallback {
        void onSuccess(com.example.clubmanagement.models.CarouselItem carouselItem);
        void onFailure(Exception e);
    }

    public interface CarouselListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.CarouselItem> items);
        void onFailure(Exception e);
    }

    /**
     * Get all carousel items ordered by position (중앙동아리 캐러셀만 - position 0, 1, 2)
     */
    public void getCarouselItems(CarouselListCallback callback) {
        // 먼저 carousel_items 컬렉션에서 시도
        db.collection("carousel_items")
                .whereLessThanOrEqualTo("position", 2)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.CarouselItem> items = new java.util.ArrayList<>();
                    java.util.List<com.google.firebase.firestore.DocumentSnapshot> carouselDocs = new java.util.ArrayList<>();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.CarouselItem item = doc.toObject(com.example.clubmanagement.models.CarouselItem.class);
                        if (item != null && item.getPosition() >= 0 && item.getPosition() <= 2) {
                            item.setId(doc.getId());
                            items.add(item);
                            carouselDocs.add(doc);
                        }
                    }

                    // carousel_items가 없으면 중앙동아리에서 직접 로드
                    if (items.isEmpty()) {
                        loadCentralClubsAsCarousel(callback);
                    } else {
                        // 강등된 동아리의 캐러셀 아이템 정리
                        cleanupDemotedClubCarouselItems(items, carouselDocs, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    // 실패 시 중앙동아리에서 직접 로드
                    loadCentralClubsAsCarousel(callback);
                });
    }

    /**
     * 강등된 동아리의 캐러셀 아이템을 정리하고 유효한 아이템만 반환
     */
    private void cleanupDemotedClubCarouselItems(
            java.util.List<com.example.clubmanagement.models.CarouselItem> items,
            java.util.List<com.google.firebase.firestore.DocumentSnapshot> carouselDocs,
            CarouselListCallback callback) {

        java.util.List<com.example.clubmanagement.models.CarouselItem> validItems = new java.util.ArrayList<>();
        java.util.List<com.google.firebase.firestore.DocumentSnapshot> docsToDelete = new java.util.ArrayList<>();

        // clubId가 있는 아이템들의 동아리 정보 확인
        java.util.List<String> clubIdsToCheck = new java.util.ArrayList<>();
        for (com.example.clubmanagement.models.CarouselItem item : items) {
            if (item.getClubId() != null && !item.getClubId().isEmpty()) {
                clubIdsToCheck.add(item.getClubId());
            }
        }

        if (clubIdsToCheck.isEmpty()) {
            // clubId가 없는 아이템만 있으면 그대로 반환
            java.util.Collections.sort(items, (i1, i2) -> Integer.compare(i1.getPosition(), i2.getPosition()));
            callback.onSuccess(items);
            return;
        }

        // 모든 관련 동아리 정보를 한 번에 조회
        final int[] checkedCount = {0};
        final int totalToCheck = clubIdsToCheck.size();

        for (int i = 0; i < items.size(); i++) {
            com.example.clubmanagement.models.CarouselItem item = items.get(i);
            com.google.firebase.firestore.DocumentSnapshot carouselDoc = carouselDocs.get(i);

            if (item.getClubId() == null || item.getClubId().isEmpty()) {
                // clubId가 없으면 유효한 것으로 간주
                validItems.add(item);
                continue;
            }

            final int index = i;
            db.collection("clubs")
                    .document(item.getClubId())
                    .get()
                    .addOnSuccessListener(clubDoc -> {
                        synchronized (validItems) {
                            if (clubDoc.exists()) {
                                Boolean isCentralClub = clubDoc.getBoolean("centralClub");
                                if (isCentralClub != null && isCentralClub) {
                                    // 중앙동아리이면 유효한 아이템
                                    validItems.add(items.get(index));
                                } else {
                                    // 강등된 동아리 - 삭제 대상
                                    docsToDelete.add(carouselDocs.get(index));
                                }
                            } else {
                                // 동아리가 존재하지 않음 - 삭제 대상
                                docsToDelete.add(carouselDocs.get(index));
                            }

                            checkedCount[0]++;
                            if (checkedCount[0] == totalToCheck) {
                                // 모든 검사 완료
                                finishCarouselCleanup(validItems, docsToDelete, callback);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        synchronized (validItems) {
                            // 조회 실패 시 일단 유효한 것으로 간주
                            validItems.add(items.get(index));

                            checkedCount[0]++;
                            if (checkedCount[0] == totalToCheck) {
                                finishCarouselCleanup(validItems, docsToDelete, callback);
                            }
                        }
                    });
        }
    }

    /**
     * 캐러셀 정리 완료 - 삭제할 아이템 삭제하고 유효한 아이템 반환
     */
    private void finishCarouselCleanup(
            java.util.List<com.example.clubmanagement.models.CarouselItem> validItems,
            java.util.List<com.google.firebase.firestore.DocumentSnapshot> docsToDelete,
            CarouselListCallback callback) {

        // 삭제할 캐러셀 아이템이 있으면 삭제
        if (!docsToDelete.isEmpty()) {
            WriteBatch batch = db.batch();
            for (com.google.firebase.firestore.DocumentSnapshot doc : docsToDelete) {
                batch.delete(doc.getReference());
            }
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        // 삭제 성공
                        Log.d(TAG, "Cleaned up " + docsToDelete.size() + " demoted club carousel items");
                    })
                    .addOnFailureListener(e -> {
                        // 삭제 실패해도 무시
                        Log.e(TAG, "Failed to cleanup carousel items", e);
                    });
        }

        // 유효한 아이템 정렬 후 반환
        java.util.Collections.sort(validItems, (i1, i2) -> Integer.compare(i1.getPosition(), i2.getPosition()));
        callback.onSuccess(validItems);
    }

    /**
     * 중앙동아리 목록에서 직접 캐러셀 아이템 생성
     */
    private void loadCentralClubsAsCarousel(CarouselListCallback callback) {
        db.collection("clubs")
                .whereEqualTo("centralClub", true)
                .limit(3) // 최대 3개
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<com.example.clubmanagement.models.CarouselItem> items = new java.util.ArrayList<>();
                    int position = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String clubId = doc.getId();
                        String clubName = doc.getString("name");
                        String description = doc.getString("description");

                        com.example.clubmanagement.models.CarouselItem item = new com.example.clubmanagement.models.CarouselItem();
                        item.setId(clubId);
                        item.setClubId(clubId);
                        item.setClubName(clubName);
                        item.setTitle(clubName != null ? clubName : "중앙동아리");
                        item.setDescription(description != null ? description : "중앙동아리입니다");
                        item.setPosition(position++);
                        items.add(item);
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Get single carousel item by position (중앙동아리 캐러셀만 - position 0, 1, 2)
     */
    public void getCarouselItemByPosition(int position, CarouselCallback callback) {
        // position이 0, 1, 2인 경우만 중앙동아리 캐러셀
        if (position < 0 || position > 2) {
            callback.onSuccess(null);
            return;
        }

        db.collection("carousel_items")
                .whereEqualTo("position", position)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        com.example.clubmanagement.models.CarouselItem item = doc.toObject(com.example.clubmanagement.models.CarouselItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                        }
                        callback.onSuccess(item);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Create or update carousel item
     */
    public void saveCarouselItem(com.example.clubmanagement.models.CarouselItem item, CarouselCallback callback) {
        String docId = item.getId() != null ? item.getId() : db.collection("carousel_items").document().getId();
        item.setId(docId);

        db.collection("carousel_items")
                .document(docId)
                .set(item)
                .addOnSuccessListener(aVoid -> callback.onSuccess(item))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Add a new carousel item (승인된 중앙동아리용)
     * position 0, 1, 2 중 빈 슬롯을 찾아서 할당
     */
    public void addCarouselItem(com.example.clubmanagement.models.CarouselItem item, SimpleCallback callback) {
        // position 0, 1, 2인 캐러셀 아이템들을 가져와서 빈 슬롯 찾기
        db.collection("carousel_items")
                .whereLessThanOrEqualTo("position", 2)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // 사용 중인 position 확인
                    java.util.Set<Integer> usedPositions = new java.util.HashSet<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Long posLong = doc.getLong("position");
                        if (posLong != null && posLong >= 0 && posLong <= 2) {
                            usedPositions.add(posLong.intValue());
                        }
                    }

                    // 빈 position 찾기 (0, 1, 2 중에서)
                    int emptyPosition = -1;
                    for (int i = 0; i <= 2; i++) {
                        if (!usedPositions.contains(i)) {
                            emptyPosition = i;
                            break;
                        }
                    }

                    // 빈 슬롯이 없으면 오류
                    if (emptyPosition == -1) {
                        callback.onFailure(new Exception("캐러셀 슬롯이 가득 찼습니다. (최대 3개)"));
                        return;
                    }

                    // position 설정
                    item.setPosition(emptyPosition);

                    // 문서 ID 생성
                    String docId = item.getClubId() != null ? item.getClubId() : db.collection("carousel_items").document().getId();
                    item.setId(docId);

                    // Firestore에 저장
                    db.collection("carousel_items")
                            .document(docId)
                            .set(item)
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Remove carousel item by club ID (중앙동아리 해제용)
     */
    public void removeCarouselItem(String clubId, SimpleCallback callback) {
        db.collection("carousel_items")
                .document(clubId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Upload carousel image to Storage
     */
    public void uploadCarouselImage(byte[] imageData, int position, SignatureCallback callback) {
        String fileName = "carousel_" + position + "_" + System.currentTimeMillis() + ".png";
        StorageReference imageRef = storage.getReference()
                .child("carousel/" + fileName);

        imageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        callback.onSuccess(uri.toString());
                    }).addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Upload carousel image to Storage by club ID
     */
    public void uploadCarouselImage(String clubId, byte[] imageData, SignatureCallback callback) {
        String fileName = "carousel_" + clubId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storage.getReference()
                .child("carousel/" + fileName);

        imageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        callback.onSuccess(uri.toString());
                    }).addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Delete carousel item
     */
    public void deleteCarouselItem(String itemId, SimpleCallback callback) {
        db.collection("carousel_items")
                .document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Notice Methods
    // ========================================

    public interface NoticeCallback {
        void onSuccess(com.example.clubmanagement.models.Notice notice);
        void onFailure(Exception e);
    }

    public interface NoticeListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.Notice> notices);
        void onFailure(Exception e);
    }

    /**
     * Get all notices ordered by position
     */
    public void getNotices(NoticeListCallback callback) {
        db.collection("notices")
                .orderBy("position")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.Notice> notices = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.Notice notice = doc.toObject(com.example.clubmanagement.models.Notice.class);
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
     * Save or update notice
     */
    public void saveNotice(com.example.clubmanagement.models.Notice notice, NoticeCallback callback) {
        String docId = notice.getId() != null ? notice.getId() : db.collection("notices").document().getId();
        notice.setId(docId);
        notice.setUpdatedAt(Timestamp.now());

        if (notice.getCreatorId() == null) {
            notice.setCreatorId(getCurrentUserId());
        }
        if (notice.getCreatedAt() == null) {
            notice.setCreatedAt(Timestamp.now());
        }

        db.collection("notices")
                .document(docId)
                .set(notice)
                .addOnSuccessListener(aVoid -> callback.onSuccess(notice))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Delete notice
     */
    public void deleteNotice(String noticeId, SimpleCallback callback) {
        db.collection("notices")
                .document(noticeId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Banner Methods
    // ========================================

    public interface BannerCallback {
        void onSuccess(com.example.clubmanagement.models.Banner banner);
        void onFailure(Exception e);
    }

    public interface BannerListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.Banner> banners);
        void onFailure(Exception e);
    }

    public interface BannerSettingsCallback {
        void onSuccess(long slideInterval);
        void onFailure(Exception e);
    }

    /**
     * Get banner (legacy - single banner)
     */
    public void getBanner(BannerCallback callback) {
        db.collection("banners")
                .document("main_banner")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.clubmanagement.models.Banner banner = documentSnapshot.toObject(com.example.clubmanagement.models.Banner.class);
                        if (banner != null) {
                            banner.setId(documentSnapshot.getId());
                        }
                        callback.onSuccess(banner);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Get all banners ordered by position
     */
    public void getBanners(BannerListCallback callback) {
        db.collection("banners")
                .orderBy("position")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.Banner> banners = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.Banner banner = doc.toObject(com.example.clubmanagement.models.Banner.class);
                        if (banner != null) {
                            banner.setId(doc.getId());
                            banners.add(banner);
                        }
                    }
                    callback.onSuccess(banners);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Save banner (legacy - single banner)
     */
    public void saveBanner(com.example.clubmanagement.models.Banner banner, BannerCallback callback) {
        banner.setId("main_banner");
        banner.setUpdatedAt(Timestamp.now());

        db.collection("banners")
                .document("main_banner")
                .set(banner)
                .addOnSuccessListener(aVoid -> callback.onSuccess(banner))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Add new banner
     */
    public void addBanner(com.example.clubmanagement.models.Banner banner, BannerCallback callback) {
        banner.setUpdatedAt(Timestamp.now());

        db.collection("banners")
                .add(banner)
                .addOnSuccessListener(documentReference -> {
                    banner.setId(documentReference.getId());
                    callback.onSuccess(banner);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Update existing banner
     */
    public void updateBanner(com.example.clubmanagement.models.Banner banner, BannerCallback callback) {
        if (banner.getId() == null) {
            callback.onFailure(new Exception("Banner ID is null"));
            return;
        }

        banner.setUpdatedAt(Timestamp.now());

        db.collection("banners")
                .document(banner.getId())
                .set(banner)
                .addOnSuccessListener(aVoid -> callback.onSuccess(banner))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Delete banner
     */
    public void deleteBanner(String bannerId, SimpleCallback callback) {
        db.collection("banners")
                .document(bannerId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 동아리의 모든 배너 삭제 (초기화)
     * 중앙동아리 신청 거절 시 사용
     */
    public void clearAllBanners(SimpleCallback callback) {
        db.collection("banners")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 모든 동아리에 배너 추가 (최고 관리자용)
     * 각 동아리의 banners 서브컬렉션에 동일한 배너를 추가합니다.
     */
    public void addBannerToAllClubs(String title, String content, String imageUrl, String linkUrl, SimpleCallback callback) {
        // 먼저 모든 동아리 목록을 가져옴
        db.collection("clubs")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onFailure(new Exception("등록된 동아리가 없습니다"));
                        return;
                    }

                    // 배너 데이터 생성
                    Map<String, Object> bannerData = new HashMap<>();
                    bannerData.put("title", title);
                    bannerData.put("description", content);  // Banner 모델의 description 필드와 일치
                    bannerData.put("imageUrl", imageUrl != null ? imageUrl : "");
                    bannerData.put("linkUrl", linkUrl != null ? linkUrl : "");
                    bannerData.put("isGlobal", true);  // 전체 배너 표시
                    bannerData.put("createdAt", Timestamp.now());
                    bannerData.put("updatedAt", Timestamp.now());

                    // 각 동아리에 배너 추가
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    int clubCount = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot clubDoc : querySnapshot) {
                        String clubId = clubDoc.getId();
                        // 각 동아리의 banners 서브컬렉션에 추가
                        com.google.firebase.firestore.DocumentReference bannerRef =
                                db.collection("clubs").document(clubId)
                                        .collection("banners").document();
                        batch.set(bannerRef, bannerData);
                        clubCount++;
                    }

                    final int totalClubs = clubCount;
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("FirebaseManager", totalClubs + "개 동아리에 배너 추가 완료");
                                callback.onSuccess();
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 모든 동아리의 전체 배너 삭제 (최고 관리자용)
     * isGlobal이 true인 배너들만 삭제합니다.
     */
    public void clearGlobalBannersFromAllClubs(SimpleCallback callback) {
        db.collection("clubs")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    // 각 동아리의 글로벌 배너를 순차적으로 삭제
                    final int[] processedCount = {0};
                    final int totalClubs = querySnapshot.size();
                    final boolean[] hasError = {false};

                    for (com.google.firebase.firestore.DocumentSnapshot clubDoc : querySnapshot) {
                        String clubId = clubDoc.getId();

                        db.collection("clubs").document(clubId)
                                .collection("banners")
                                .whereEqualTo("isGlobal", true)
                                .get()
                                .addOnSuccessListener(bannerSnapshot -> {
                                    if (!bannerSnapshot.isEmpty()) {
                                        com.google.firebase.firestore.WriteBatch batch = db.batch();
                                        for (com.google.firebase.firestore.DocumentSnapshot bannerDoc : bannerSnapshot) {
                                            batch.delete(bannerDoc.getReference());
                                        }
                                        batch.commit()
                                                .addOnCompleteListener(task -> {
                                                    processedCount[0]++;
                                                    if (processedCount[0] >= totalClubs) {
                                                        if (hasError[0]) {
                                                            callback.onFailure(new Exception("일부 동아리에서 배너 삭제 실패"));
                                                        } else {
                                                            callback.onSuccess();
                                                        }
                                                    }
                                                });
                                    } else {
                                        processedCount[0]++;
                                        if (processedCount[0] >= totalClubs) {
                                            if (hasError[0]) {
                                                callback.onFailure(new Exception("일부 동아리에서 배너 삭제 실패"));
                                            } else {
                                                callback.onSuccess();
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    hasError[0] = true;
                                    processedCount[0]++;
                                    if (processedCount[0] >= totalClubs) {
                                        callback.onFailure(new Exception("일부 동아리에서 배너 삭제 실패"));
                                    }
                                });
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 특정 동아리의 배너 목록 가져오기
     */
    public void getClubBanners(String clubId, BannerListCallback callback) {
        db.collection("clubs").document(clubId)
                .collection("banners")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<com.example.clubmanagement.models.Banner> banners = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        com.example.clubmanagement.models.Banner banner =
                                doc.toObject(com.example.clubmanagement.models.Banner.class);
                        if (banner != null) {
                            banner.setId(doc.getId());
                            banners.add(banner);
                        }
                    }
                    // 클라이언트 측에서 정렬 (최신순)
                    banners.sort((b1, b2) -> {
                        if (b1.getUpdatedAt() == null && b2.getUpdatedAt() == null) return 0;
                        if (b1.getUpdatedAt() == null) return 1;
                        if (b2.getUpdatedAt() == null) return -1;
                        return b2.getUpdatedAt().compareTo(b1.getUpdatedAt());
                    });
                    callback.onSuccess(banners);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Get banner slide interval (in milliseconds)
     */
    public void getBannerSlideInterval(BannerSettingsCallback callback) {
        db.collection("settings")
                .document("banner_settings")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long interval = documentSnapshot.getLong("slideInterval");
                        callback.onSuccess(interval != null ? interval : 3000L); // Default 3 seconds
                    } else {
                        callback.onSuccess(3000L); // Default 3 seconds
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Save banner slide interval (in milliseconds)
     */
    public void saveBannerSlideInterval(long intervalMs, SimpleCallback callback) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("slideInterval", intervalMs);
        settings.put("updatedAt", Timestamp.now());

        db.collection("settings")
                .document("banner_settings")
                .set(settings)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Upload banner image to Storage
     */
    public void uploadBannerImage(byte[] imageData, SignatureCallback callback) {
        String fileName = "banner_" + System.currentTimeMillis() + ".png";
        StorageReference imageRef = storage.getReference()
                .child("banners/" + fileName);

        imageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        callback.onSuccess(uri.toString());
                    }).addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // LinkButton Methods
    // ========================================

    public interface LinkButtonCallback {
        void onSuccess(com.example.clubmanagement.models.LinkButton linkButton);
        void onFailure(Exception e);
    }

    public interface LinkButtonListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.LinkButton> linkButtons);
        void onFailure(Exception e);
    }

    /**
     * Get all link buttons ordered by position
     */
    public void getLinkButtons(LinkButtonListCallback callback) {
        db.collection("link_buttons")
                .orderBy("position")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.LinkButton> linkButtons = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.LinkButton linkButton = doc.toObject(com.example.clubmanagement.models.LinkButton.class);
                        if (linkButton != null) {
                            linkButton.setId(doc.getId());
                            linkButtons.add(linkButton);
                        }
                    }
                    callback.onSuccess(linkButtons);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Save link button
     */
    public void saveLinkButton(com.example.clubmanagement.models.LinkButton linkButton, LinkButtonCallback callback) {
        String docId = linkButton.getId() != null ? linkButton.getId() : db.collection("link_buttons").document().getId();
        linkButton.setId(docId);

        db.collection("link_buttons")
                .document(docId)
                .set(linkButton)
                .addOnSuccessListener(aVoid -> callback.onSuccess(linkButton))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Delete link button
     */
    public void deleteLinkButton(String linkButtonId, SimpleCallback callback) {
        db.collection("link_buttons")
                .document(linkButtonId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Club Methods
    // ========================================

    public interface ClubCallback {
        void onSuccess(com.example.clubmanagement.models.Club club);
        void onFailure(Exception e);
    }

    public interface ClubListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.Club> clubs);
        void onFailure(Exception e);
    }

    /**
     * Get all clubs
     */
    public void getAllClubs(ClubListCallback callback) {
        db.collection("clubs")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    java.util.List<com.example.clubmanagement.models.Club> clubs = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        com.example.clubmanagement.models.Club club = doc.toObject(com.example.clubmanagement.models.Club.class);
                        if (club != null) {
                            // document ID를 club ID로 설정
                            club.setId(doc.getId());
                            clubs.add(club);
                        }
                    }
                    callback.onSuccess(clubs);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Get club information by ID
     */
    public void getClub(String clubId, ClubCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.clubmanagement.models.Club club = documentSnapshot.toObject(com.example.clubmanagement.models.Club.class);
                        if (club != null) {
                            club.setId(documentSnapshot.getId());
                        }
                        callback.onSuccess(club);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    // 서버 연결 실패 시 캐시에서 가져오기 시도
                    db.collection("clubs")
                            .document(clubId)
                            .get(com.google.firebase.firestore.Source.CACHE)
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    com.example.clubmanagement.models.Club club = documentSnapshot.toObject(com.example.clubmanagement.models.Club.class);
                                    if (club != null) {
                                        club.setId(documentSnapshot.getId());
                                    }
                                    callback.onSuccess(club);
                                } else {
                                    callback.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(cacheError -> {
                                // 캐시도 없으면 null 반환 (새 동아리로 처리)
                                callback.onSuccess(null);
                            });
                });
    }

    /**
     * Save or update club information
     */
    public void saveClub(com.example.clubmanagement.models.Club club, ClubCallback callback) {
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

    // ========================================
    // User & Central Club Management
    // ========================================

    public interface UserCallback {
        void onSuccess(com.example.clubmanagement.models.User user);
        void onFailure(Exception e);
    }

    /**
     * Save user to Firestore
     */
    public void saveUser(com.example.clubmanagement.models.User user, OperationCallback callback) {
        if (user.getUid() == null || user.getUid().isEmpty()) {
            callback.onFailure(new Exception("User UID cannot be null or empty"));
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Update user profile (name, department, phone)
     */
    public void updateUserProfile(String name, String department, String phone, SimpleCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new Exception("로그인되지 않았습니다"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("department", department);
        updates.put("phone", phone);

        db.collection("users")
                .document(firebaseUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Get current user information
     */
    public void getCurrentUser(UserCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            // 로그인되지 않은 경우 null 반환 (에러 대신)
            callback.onSuccess(null);
            return;
        }

        db.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.clubmanagement.models.User user = documentSnapshot.toObject(com.example.clubmanagement.models.User.class);
                        callback.onSuccess(user);
                    } else {
                        // Create new user
                        com.example.clubmanagement.models.User newUser = new com.example.clubmanagement.models.User(
                                firebaseUser.getUid(),
                                firebaseUser.getEmail()
                        );
                        callback.onSuccess(newUser);
                    }
                })
                .addOnFailureListener(e -> {
                    // 오류 발생 시에도 null 반환
                    callback.onSuccess(null);
                });
    }

    /**
     * Join central club
     */
    public void joinCentralClub(String clubId, String clubName, SimpleCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        // 로그인되지 않은 경우 즉시 성공 처리 (Firebase 호출 없이)
        if (firebaseUser == null) {
            callback.onSuccess();
            return;
        }

        // Get current date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA);
        String joinDate = sdf.format(new java.util.Date());

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", firebaseUser.getUid());
        userData.put("email", firebaseUser.getEmail());
        userData.put("centralClubId", clubId);
        userData.put("centralClubName", clubName);
        userData.put("joinDate", joinDate);

        db.collection("users")
                .document(firebaseUser.getUid())
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // 동아리 members 서브컬렉션에도 멤버 추가
                    addMemberToClub(clubId, firebaseUser.getUid(), firebaseUser.getEmail(), joinDate, callback);
                })
                .addOnFailureListener(e -> callback.onSuccess());
    }

    /**
     * 동아리 members 서브컬렉션에 멤버 추가
     */
    private void addMemberToClub(String clubId, String userId, String email, String joinDate, SimpleCallback callback) {
        // 먼저 사용자 정보 가져오기
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("userId", userId);
                    memberData.put("email", email);
                    memberData.put("joinDate", joinDate);
                    memberData.put("joinedAt", System.currentTimeMillis());
                    memberData.put("isAdmin", false);

                    // 사용자 정보가 있으면 추가
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String department = documentSnapshot.getString("department");
                        String studentId = documentSnapshot.getString("studentId");
                        String phone = documentSnapshot.getString("phone");

                        if (name != null) memberData.put("name", name);
                        if (department != null) memberData.put("department", department);
                        if (studentId != null) memberData.put("studentId", studentId);
                        if (phone != null) memberData.put("phone", phone);
                    }

                    // 멤버 컬렉션에 추가
                    db.collection("clubs")
                            .document(clubId)
                            .collection("members")
                            .document(userId)
                            .set(memberData)
                            .addOnSuccessListener(aVoid -> {
                                // 동아리 멤버 수 증가
                                db.collection("clubs")
                                        .document(clubId)
                                        .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1))
                                        .addOnSuccessListener(aVoid2 -> {
                                            // 단체 채팅방에 자동 참여
                                            joinGroupChatRoom(clubId, new SimpleCallback() {
                                                @Override
                                                public void onSuccess() {
                                                    callback.onSuccess();
                                                }

                                                @Override
                                                public void onFailure(Exception e) {
                                                    // 채팅방 참여 실패해도 가입은 성공 처리
                                                    callback.onSuccess();
                                                }
                                            });
                                        })
                                        .addOnFailureListener(e -> callback.onSuccess());
                            })
                            .addOnFailureListener(e -> callback.onSuccess());
                })
                .addOnFailureListener(e -> callback.onSuccess());
    }

    /**
     * Leave central club
     */
    public void leaveCentralClub(SimpleCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            // 로그인되지 않은 경우 성공 처리
            callback.onSuccess();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("centralClubId", null);
        updates.put("centralClubName", null);
        updates.put("joinDate", null);

        db.collection("users")
                .document(firebaseUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onSuccess());
    }

    /**
     * Join general club
     */
    public void joinGeneralClub(String clubId, String clubName, SimpleCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        // 로그인되지 않은 경우 즉시 성공 처리 (Firebase 호출 없이)
        if (firebaseUser == null) {
            callback.onSuccess();
            return;
        }

        // Get current date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA);
        String joinDate = sdf.format(new java.util.Date());

        // 로그인된 경우에만 Firebase 호출
        Map<String, Object> updates = new HashMap<>();
        updates.put("generalClubIds", com.google.firebase.firestore.FieldValue.arrayUnion(clubId));
        updates.put("generalClubNames", com.google.firebase.firestore.FieldValue.arrayUnion(clubName));

        db.collection("users")
                .document(firebaseUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 동아리 members 서브컬렉션에도 멤버 추가
                    addMemberToClub(clubId, firebaseUser.getUid(), firebaseUser.getEmail(), joinDate, callback);
                })
                .addOnFailureListener(e -> callback.onSuccess());
    }

    /**
     * Leave general club
     */
    public void leaveGeneralClub(String clubId, SimpleCallback callback) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        // 로그인되지 않은 경우 즉시 성공 처리
        if (firebaseUser == null) {
            callback.onSuccess();
            return;
        }

        // 로그인된 경우에만 Firebase 호출
        Map<String, Object> updates = new HashMap<>();
        updates.put("generalClubIds", com.google.firebase.firestore.FieldValue.arrayRemove(clubId));

        db.collection("users")
                .document(firebaseUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onSuccess());
    }

    // ========================================
    // Budget Transaction Methods
    // ========================================

    /**
     * Callback for budget transaction list
     */
    public interface BudgetTransactionListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.BudgetTransaction> transactions);
        void onFailure(Exception e);
    }

    /**
     * Callback for single budget transaction
     */
    public interface BudgetTransactionCallback {
        void onSuccess(com.example.clubmanagement.models.BudgetTransaction transaction);
        void onFailure(Exception e);
    }

    /**
     * Get budget transactions for a club
     */
    public void getBudgetTransactions(String clubId, BudgetTransactionListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("transactions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.BudgetTransaction> transactions = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.BudgetTransaction transaction = doc.toObject(com.example.clubmanagement.models.BudgetTransaction.class);
                        if (transaction != null) {
                            transaction.setId(doc.getId());
                            transactions.add(transaction);
                        }
                    }
                    // 클라이언트 측 정렬 (createdAt 내림차순)
                    java.util.Collections.sort(transactions, (t1, t2) -> {
                        if (t1.getCreatedAt() == null && t2.getCreatedAt() == null) return 0;
                        if (t1.getCreatedAt() == null) return 1;
                        if (t2.getCreatedAt() == null) return -1;
                        return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                    });
                    callback.onSuccess(transactions);
                })
                .addOnFailureListener(e -> callback.onSuccess(new java.util.ArrayList<>()));
    }

    /**
     * Save budget transaction and update club balance
     */
    public void saveBudgetTransaction(com.example.clubmanagement.models.BudgetTransaction transaction, long newBalance, BudgetTransactionCallback callback) {
        String oderId = getCurrentUserId();
        if (oderId == null) {
            oderId = "guest";
        }

        transaction.setCreatedBy(oderId);
        transaction.setCreatedAt(com.google.firebase.Timestamp.now());

        String clubId = transaction.getClubId();

        // Use batch write to ensure atomicity
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        // Add transaction document
        com.google.firebase.firestore.DocumentReference transactionRef = db.collection("clubs")
                .document(clubId)
                .collection("transactions")
                .document();

        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("clubId", transaction.getClubId());
        transactionData.put("type", transaction.getType());
        transactionData.put("amount", transaction.getAmount());
        transactionData.put("description", transaction.getDescription());
        transactionData.put("receiptImageUrl", transaction.getReceiptImageUrl());
        transactionData.put("createdBy", transaction.getCreatedBy());
        transactionData.put("createdByName", transaction.getCreatedByName());
        transactionData.put("createdAt", transaction.getCreatedAt());
        transactionData.put("balanceAfter", transaction.getBalanceAfter());

        batch.set(transactionRef, transactionData);

        // Update club balance
        com.google.firebase.firestore.DocumentReference clubRef = db.collection("clubs").document(clubId);
        batch.update(clubRef, "currentBudget", newBalance);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    transaction.setId(transactionRef.getId());
                    callback.onSuccess(transaction);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Upload receipt image
     */
    public void uploadReceiptImage(String clubId, byte[] imageData, SignatureCallback callback) {
        String fileName = "receipts/" + clubId + "/" + System.currentTimeMillis() + ".jpg";
        com.google.firebase.storage.StorageReference storageRef = storage.getReference().child(fileName);

        storageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Update budget transaction
     */
    public void updateBudgetTransaction(com.example.clubmanagement.models.BudgetTransaction transaction, long newBalance, SimpleCallback callback) {
        String clubId = transaction.getClubId();
        String transactionId = transaction.getId();

        if (clubId == null || transactionId == null) {
            callback.onFailure(new Exception("유효하지 않은 거래 정보입니다"));
            return;
        }

        com.google.firebase.firestore.WriteBatch batch = db.batch();

        // Update transaction document
        com.google.firebase.firestore.DocumentReference transactionRef = db.collection("clubs")
                .document(clubId)
                .collection("transactions")
                .document(transactionId);

        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("type", transaction.getType());
        transactionData.put("amount", transaction.getAmount());
        transactionData.put("description", transaction.getDescription());
        transactionData.put("receiptImageUrl", transaction.getReceiptImageUrl());
        transactionData.put("balanceAfter", transaction.getBalanceAfter());

        batch.update(transactionRef, transactionData);

        // Update club balance
        com.google.firebase.firestore.DocumentReference clubRef = db.collection("clubs").document(clubId);
        batch.update(clubRef, "currentBudget", newBalance);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Delete budget transaction
     */
    public void deleteBudgetTransaction(String clubId, String transactionId, long newBalance, SimpleCallback callback) {
        if (clubId == null || transactionId == null) {
            callback.onFailure(new Exception("유효하지 않은 거래 정보입니다"));
            return;
        }

        com.google.firebase.firestore.WriteBatch batch = db.batch();

        // Delete transaction document
        com.google.firebase.firestore.DocumentReference transactionRef = db.collection("clubs")
                .document(clubId)
                .collection("transactions")
                .document(transactionId);

        batch.delete(transactionRef);

        // Update club balance
        com.google.firebase.firestore.DocumentReference clubRef = db.collection("clubs").document(clubId);
        batch.update(clubRef, "currentBudget", newBalance);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Super Admin Methods
    // ========================================

    /**
     * Delete carousel image from storage and clear URL in Firestore
     */
    public void deleteCarouselImage(int position, SimpleCallback callback) {
        // First, get the carousel item to find the image URL
        db.collection("carousel")
                .whereEqualTo("position", position)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onSuccess();
                        return;
                    }

                    com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    String imageUrl = doc.getString("imageUrl");

                    // Update Firestore to remove image URL
                    doc.getReference().update("imageUrl", null)
                            .addOnSuccessListener(aVoid -> {
                                // If there was an image URL, delete from storage
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    try {
                                        StorageReference storageRef = storage.getReferenceFromUrl(imageUrl);
                                        storageRef.delete()
                                                .addOnSuccessListener(aVoid2 -> callback.onSuccess())
                                                .addOnFailureListener(e -> {
                                                    // Even if storage delete fails, Firestore was updated
                                                    callback.onSuccess();
                                                });
                                    } catch (Exception e) {
                                        callback.onSuccess();
                                    }
                                } else {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Cancel central club status - change to general club
     */
    public void cancelCentralClubStatus(String clubId, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("centralClub", false);

        db.collection("clubs")
                .document(clubId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 캐러셀에서도 해당 동아리 제거
                    removeCarouselItemByClubId(clubId, new SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            // 캐러셀 삭제 실패해도 강등은 성공으로 처리
                            callback.onSuccess();
                        }
                    });
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Remove carousel item by club ID (캐러셀에서 clubId로 검색하여 삭제)
     */
    private void removeCarouselItemByClubId(String clubId, SimpleCallback callback) {
        // clubId 필드로 캐러셀 아이템 검색
        db.collection("carousel_items")
                .whereEqualTo("clubId", clubId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // 캐러셀 아이템이 없으면 성공으로 처리
                        callback.onSuccess();
                        return;
                    }

                    // 찾은 모든 캐러셀 아이템 삭제
                    WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Member Management Methods
    // ========================================

    public interface MembersCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.Member> members);
        void onFailure(Exception e);
    }

    /**
     * Get club members
     */
    public void getClubMembers(String clubId, MembersCallback callback) {
        java.util.List<com.example.clubmanagement.models.Member> allMembers = new java.util.ArrayList<>();
        java.util.Set<String> memberUserIds = new java.util.HashSet<>();

        // 1. clubs/{clubId}/members 컬렉션에서 멤버 가져오기
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.Member member = doc.toObject(com.example.clubmanagement.models.Member.class);
                        if (member != null) {
                            member.setUserId(doc.getId());
                            allMembers.add(member);
                            memberUserIds.add(doc.getId());
                        }
                    }

                    // 2. users 컬렉션에서 이 동아리에 가입한 사용자들 찾기
                    findMembersFromUsersCollection(clubId, allMembers, memberUserIds, callback);
                })
                .addOnFailureListener(e -> {
                    // members 컬렉션 실패해도 users 컬렉션에서 찾기 시도
                    findMembersFromUsersCollection(clubId, allMembers, memberUserIds, callback);
                });
    }

    /**
     * users 컬렉션에서 해당 동아리에 가입한 사용자들 찾기
     */
    private void findMembersFromUsersCollection(String clubId,
            java.util.List<com.example.clubmanagement.models.Member> existingMembers,
            java.util.Set<String> existingMemberIds, MembersCallback callback) {

        // clubId의 여러 형태를 확인 (소문자, 언더스코어 등)
        String clubIdLower = clubId.toLowerCase();
        String clubIdNormalized = clubId.replaceAll("\\s+", "_").toLowerCase();

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // userId -> name 매핑 생성
                    java.util.Map<String, String> userIdToName = new java.util.HashMap<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String userName = doc.getString("name");
                        if (userName != null && !userName.isEmpty()) {
                            userIdToName.put(doc.getId(), userName);
                        }
                    }

                    // 기존 멤버들의 이름이 이메일이면 실제 이름으로 업데이트
                    for (com.example.clubmanagement.models.Member member : existingMembers) {
                        String currentName = member.getName();
                        if (currentName == null || currentName.contains("@")) {
                            String actualName = userIdToName.get(member.getUserId());
                            if (actualName != null && !actualName.isEmpty()) {
                                member.setName(actualName);
                            }
                        }
                    }

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String odUserId = doc.getId();

                        // 이미 목록에 있으면 스킵
                        if (existingMemberIds.contains(odUserId)) {
                            continue;
                        }

                        boolean isMember = false;

                        // centralClubId 확인
                        String centralClubId = doc.getString("centralClubId");
                        if (centralClubId != null) {
                            String centralIdLower = centralClubId.toLowerCase();
                            String centralIdNormalized = centralClubId.replaceAll("\\s+", "_").toLowerCase();
                            if (centralClubId.equals(clubId) ||
                                centralIdLower.equals(clubIdLower) ||
                                centralIdNormalized.equals(clubIdNormalized) ||
                                clubIdNormalized.equals(centralIdNormalized)) {
                                isMember = true;
                            }
                        }

                        // generalClubIds 확인
                        if (!isMember) {
                            java.util.List<String> generalClubIds = (java.util.List<String>) doc.get("generalClubIds");
                            if (generalClubIds != null) {
                                for (String gClubId : generalClubIds) {
                                    if (gClubId == null) continue;
                                    String gIdLower = gClubId.toLowerCase();
                                    String gIdNormalized = gClubId.replaceAll("\\s+", "_").toLowerCase();
                                    if (gClubId.equals(clubId) ||
                                        gIdLower.equals(clubIdLower) ||
                                        gIdNormalized.equals(clubIdNormalized) ||
                                        clubIdNormalized.equals(gIdNormalized)) {
                                        isMember = true;
                                        break;
                                    }
                                }
                            }
                        }

                        // 멤버라면 목록에 추가
                        if (isMember) {
                            com.example.clubmanagement.models.Member member = createMemberFromUserDoc(doc);
                            if (member != null) {
                                existingMembers.add(member);
                                existingMemberIds.add(odUserId);
                            }
                        }
                    }

                    callback.onSuccess(existingMembers);
                })
                .addOnFailureListener(e -> {
                    // 실패해도 기존 멤버는 반환
                    callback.onSuccess(existingMembers);
                });
    }

    /**
     * 현재 로그인한 사용자가 이 동아리 멤버인지 확인하고 추가
     */
    private void addCurrentUserIfMember(String clubId, java.util.List<com.example.clubmanagement.models.Member> existingMembers,
                                        java.util.Set<String> existingMemberIds, MembersCallback callback) {
        String currentUserId = getCurrentUserId();

        // 로그인 안되어 있거나 이미 목록에 있으면 바로 반환
        if (currentUserId == null || existingMemberIds.contains(currentUserId)) {
            callback.onSuccess(existingMembers);
            return;
        }

        // 현재 사용자 정보 가져오기
        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        // 이 동아리에 가입되어 있는지 확인
                        String centralClubId = userDoc.getString("centralClubId");
                        java.util.List<String> generalClubIds = (java.util.List<String>) userDoc.get("generalClubIds");

                        boolean isMember = false;

                        // centralClubId 확인 (정확한 ID 또는 clubName 기반 ID)
                        if (centralClubId != null && (centralClubId.equals(clubId) ||
                            centralClubId.replaceAll("\\s+", "_").toLowerCase().equals(clubId))) {
                            isMember = true;
                        }

                        // generalClubIds 확인
                        if (!isMember && generalClubIds != null) {
                            for (String gClubId : generalClubIds) {
                                if (gClubId.equals(clubId) ||
                                    gClubId.replaceAll("\\s+", "_").toLowerCase().equals(clubId)) {
                                    isMember = true;
                                    break;
                                }
                            }
                        }

                        // 동아리 멤버라면 목록에 추가
                        if (isMember) {
                            com.example.clubmanagement.models.Member member = createMemberFromUserDoc(userDoc);
                            if (member != null) {
                                existingMembers.add(member);
                                // members 컬렉션에도 동기화
                                syncMemberToClubCollection(clubId, member);
                            }
                        }
                    }
                    callback.onSuccess(existingMembers);
                })
                .addOnFailureListener(e -> {
                    // 실패해도 기존 멤버는 반환
                    callback.onSuccess(existingMembers);
                });
    }

    /**
     * 사용자 문서에서 Member 객체 생성
     */
    private com.example.clubmanagement.models.Member createMemberFromUserDoc(com.google.firebase.firestore.DocumentSnapshot doc) {
        com.example.clubmanagement.models.Member member = new com.example.clubmanagement.models.Member();
        member.setUserId(doc.getId());
        member.setName(doc.getString("name"));
        member.setEmail(doc.getString("email"));
        member.setDepartment(doc.getString("department"));
        member.setStudentId(doc.getString("studentId"));
        member.setPhone(doc.getString("phone"));
        member.setJoinDate(doc.getString("joinDate"));
        member.setAdmin(false);
        return member;
    }

    /**
     * 멤버를 clubs/{clubId}/members 컬렉션에 동기화
     */
    private void syncMemberToClubCollection(String clubId, com.example.clubmanagement.models.Member member) {
        if (member.getUserId() == null) return;

        Map<String, Object> memberData = new HashMap<>();
        memberData.put("userId", member.getUserId());
        if (member.getName() != null) memberData.put("name", member.getName());
        if (member.getEmail() != null) memberData.put("email", member.getEmail());
        if (member.getDepartment() != null) memberData.put("department", member.getDepartment());
        if (member.getStudentId() != null) memberData.put("studentId", member.getStudentId());
        if (member.getPhone() != null) memberData.put("phone", member.getPhone());
        if (member.getJoinDate() != null) memberData.put("joinDate", member.getJoinDate());
        memberData.put("joinedAt", System.currentTimeMillis());
        memberData.put("isAdmin", false);

        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(member.getUserId())
                .set(memberData)
                .addOnSuccessListener(aVoid -> {
                    // 멤버 수 업데이트
                    db.collection("clubs")
                            .document(clubId)
                            .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync member: " + e.getMessage()));
    }

    /**
     * Get join requests for a club (from both join_requests and membershipApplications)
     */
    public void getJoinRequests(String clubId, MembersCallback callback) {
        Log.d(TAG, "getJoinRequests - clubId: " + clubId);
        java.util.List<com.example.clubmanagement.models.Member> allMembers = new java.util.ArrayList<>();

        // 1. 먼저 기존 join_requests 컬렉션에서 가져오기
        db.collection("clubs")
                .document(clubId)
                .collection("join_requests")
                .whereEqualTo("requestStatus", "pending")
                .get()
                .addOnSuccessListener(joinRequestsSnapshots -> {
                    Log.d(TAG, "getJoinRequests - join_requests count: " + joinRequestsSnapshots.size());
                    for (com.google.firebase.firestore.DocumentSnapshot doc : joinRequestsSnapshots) {
                        com.example.clubmanagement.models.Member member = doc.toObject(com.example.clubmanagement.models.Member.class);
                        if (member != null) {
                            member.setUserId(doc.getId());
                            allMembers.add(member);
                        }
                    }

                    // 2. 그 다음 membershipApplications 컬렉션에서도 가져오기
                    db.collection("clubs")
                            .document(clubId)
                            .collection("membershipApplications")
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener(applicationSnapshots -> {
                                Log.d(TAG, "getJoinRequests - membershipApplications count: " + applicationSnapshots.size());
                                for (com.google.firebase.firestore.DocumentSnapshot doc : applicationSnapshots) {
                                    Log.d(TAG, "getJoinRequests - application doc: " + doc.getId() + ", data: " + doc.getData());
                                    // Member 객체로 변환
                                    com.example.clubmanagement.models.Member member = new com.example.clubmanagement.models.Member();
                                    member.setUserId(doc.getString("userId"));
                                    member.setName(doc.getString("name"));
                                    member.setDepartment(doc.getString("department"));
                                    member.setStudentId(doc.getString("studentId"));
                                    member.setPhone(doc.getString("phone"));
                                    member.setEmail(doc.getString("email"));

                                    Long birthMonth = doc.getLong("birthMonth");
                                    Long birthDay = doc.getLong("birthDay");
                                    if (birthMonth != null) member.setBirthMonth(birthMonth.intValue());
                                    if (birthDay != null) member.setBirthDay(birthDay.intValue());

                                    // applicationId 저장 (승인/거절 시 사용)
                                    member.setApplicationId(doc.getId());

                                    allMembers.add(member);
                                }
                                Log.d(TAG, "getJoinRequests - total members: " + allMembers.size());
                                callback.onSuccess(allMembers);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "getJoinRequests - membershipApplications error: " + e.getMessage());
                                // membershipApplications 실패해도 join_requests 결과는 반환
                                callback.onSuccess(allMembers);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getJoinRequests - join_requests error: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    /**
     * Get leave requests for a club
     */
    public void getLeaveRequests(String clubId, MembersCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("leave_requests")
                .whereEqualTo("requestStatus", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.Member> members = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.Member member = doc.toObject(com.example.clubmanagement.models.Member.class);
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
     * Set member admin permission
     */
    public void setMemberAdminPermission(String clubId, String userId, boolean isAdmin, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAdmin", isAdmin);

        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Check if user has admin permission for a club
     */
    public void checkMemberAdminPermission(String clubId, String userId, AdminCheckCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                        callback.onResult(isAdmin != null && isAdmin);
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    /**
     * Expel member from club
     */
    public void expelMember(String clubId, String userId, SimpleCallback callback) {
        // Remove from members collection
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Also update club member count
                    db.collection("clubs")
                            .document(clubId)
                            .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-1))
                            .addOnSuccessListener(aVoid2 -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onSuccess()); // Still success if count update fails
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Approve join request
     */
    public void approveJoinRequest(String clubId, String userId, SimpleCallback callback) {
        // Get request data first
        db.collection("clubs")
                .document(clubId)
                .collection("join_requests")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> memberData = documentSnapshot.getData();
                        if (memberData != null) {
                            memberData.put("isAdmin", false);
                            memberData.put("joinedAt", System.currentTimeMillis());
                            memberData.remove("requestStatus");

                            // Add to members collection
                            db.collection("clubs")
                                    .document(clubId)
                                    .collection("members")
                                    .document(userId)
                                    .set(memberData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Remove from join_requests
                                        db.collection("clubs")
                                                .document(clubId)
                                                .collection("join_requests")
                                                .document(userId)
                                                .delete()
                                                .addOnSuccessListener(aVoid2 -> {
                                                    // Update member count
                                                    db.collection("clubs")
                                                            .document(clubId)
                                                            .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1))
                                                            .addOnSuccessListener(aVoid3 -> callback.onSuccess())
                                                            .addOnFailureListener(e -> callback.onSuccess());
                                                })
                                                .addOnFailureListener(callback::onFailure);
                                    })
                                    .addOnFailureListener(callback::onFailure);
                        } else {
                            callback.onFailure(new Exception("Request data is empty"));
                        }
                    } else {
                        callback.onFailure(new Exception("Join request not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Reject join request
     */
    public void rejectJoinRequest(String clubId, String userId, SimpleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("join_requests")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Approve leave request
     */
    public void approveLeaveRequest(String clubId, String userId, SimpleCallback callback) {
        // Remove from members
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove from leave_requests
                    db.collection("clubs")
                            .document(clubId)
                            .collection("leave_requests")
                            .document(userId)
                            .delete()
                            .addOnSuccessListener(aVoid2 -> {
                                // Update member count
                                db.collection("clubs")
                                        .document(clubId)
                                        .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-1))
                                        .addOnSuccessListener(aVoid3 -> callback.onSuccess())
                                        .addOnFailureListener(e -> callback.onSuccess());
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Reject leave request
     */
    public void rejectLeaveRequest(String clubId, String userId, SimpleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("leave_requests")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Submit join request
     */
    public void submitJoinRequest(String clubId, com.example.clubmanagement.models.Member member, SimpleCallback callback) {
        member.setRequestStatus("pending");

        db.collection("clubs")
                .document(clubId)
                .collection("join_requests")
                .document(member.getUserId())
                .set(member)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Submit leave request
     */
    public void submitLeaveRequest(String clubId, String userId, SimpleCallback callback) {
        // Get member data first
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> memberData = documentSnapshot.getData();
                        if (memberData != null) {
                            memberData.put("requestStatus", "pending");

                            db.collection("clubs")
                                    .document(clubId)
                                    .collection("leave_requests")
                                    .document(userId)
                                    .set(memberData)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                                    .addOnFailureListener(callback::onFailure);
                        } else {
                            callback.onFailure(new Exception("Member data is empty"));
                        }
                    } else {
                        callback.onFailure(new Exception("Member not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Edit Request Methods (최고 관리자 수정 요청)
    // ========================================

    public interface EditRequestCallback {
        void onSuccess(com.example.clubmanagement.models.EditRequest request);
        void onFailure(Exception e);
    }

    public interface EditRequestListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.EditRequest> requests);
        void onFailure(Exception e);
    }

    /**
     * 수정 요청 생성 (최고 관리자가 동아리 정보 수정 시)
     */
    public void createEditRequest(com.example.clubmanagement.models.EditRequest request, SimpleCallback callback) {
        String docId = db.collection("edit_requests").document().getId();
        request.setId(docId);

        db.collection("edit_requests")
                .document(docId)
                .set(request)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 특정 동아리의 수정 요청 목록 가져오기 (동아리 관리자용)
     */
    public void getEditRequestsForClub(String clubId, EditRequestListCallback callback) {
        db.collection("edit_requests")
                .whereEqualTo("clubId", clubId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.EditRequest> requests = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.EditRequest request = doc.toObject(com.example.clubmanagement.models.EditRequest.class);
                        if (request != null) {
                            request.setId(doc.getId());
                            requests.add(request);
                        }
                    }
                    // 클라이언트 측 정렬 (최신순)
                    requests.sort((r1, r2) -> {
                        if (r1.getCreatedAt() == null && r2.getCreatedAt() == null) return 0;
                        if (r1.getCreatedAt() == null) return 1;
                        if (r2.getCreatedAt() == null) return -1;
                        return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                    });
                    callback.onSuccess(requests);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 특정 동아리의 읽지 않은 수정 요청 개수 가져오기
     */
    public void getUnreadEditRequestCount(String clubId, CountCallback callback) {
        db.collection("edit_requests")
                .whereEqualTo("clubId", clubId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(queryDocumentSnapshots.size());
                })
                .addOnFailureListener(callback::onFailure);
    }

    public interface CountCallback {
        void onSuccess(int count);
        void onFailure(Exception e);
    }

    /**
     * 수정 요청 읽음 처리
     */
    public void markEditRequestAsRead(String requestId, SimpleCallback callback) {
        db.collection("edit_requests")
                .document(requestId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 수정 요청 삭제
     */
    public void deleteEditRequest(String requestId, SimpleCallback callback) {
        db.collection("edit_requests")
                .document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Central Club Application Methods
    // ========================================

    public interface CentralApplicationCallback {
        void onSuccess(com.example.clubmanagement.models.CentralClubApplication application);
        void onFailure(Exception e);
    }

    public interface CentralApplicationListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.CentralClubApplication> applications);
        void onFailure(Exception e);
    }

    /**
     * 중앙동아리 신청
     */
    public void submitCentralClubApplication(com.example.clubmanagement.models.CentralClubApplication application,
                                              CentralApplicationCallback callback) {
        application.setCreatedAt(Timestamp.now());
        application.setStatus(com.example.clubmanagement.models.CentralClubApplication.STATUS_PENDING);

        db.collection("central_club_applications")
                .add(application)
                .addOnSuccessListener(documentReference -> {
                    application.setId(documentReference.getId());
                    callback.onSuccess(application);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 해당 동아리의 대기중인 신청 확인
     */
    public void getPendingApplicationForClub(String clubId, CentralApplicationCallback callback) {
        db.collection("central_club_applications")
                .whereEqualTo("clubId", clubId)
                .whereEqualTo("status", com.example.clubmanagement.models.CentralClubApplication.STATUS_PENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        com.example.clubmanagement.models.CentralClubApplication app =
                                doc.toObject(com.example.clubmanagement.models.CentralClubApplication.class);
                        if (app != null) {
                            app.setId(doc.getId());
                        }
                        callback.onSuccess(app);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 대기중인 모든 중앙동아리 신청 목록 (최고 관리자용)
     */
    public void getPendingCentralApplications(CentralApplicationListCallback callback) {
        db.collection("central_club_applications")
                .whereEqualTo("status", com.example.clubmanagement.models.CentralClubApplication.STATUS_PENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.CentralClubApplication> applications = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.CentralClubApplication app =
                                doc.toObject(com.example.clubmanagement.models.CentralClubApplication.class);
                        if (app != null) {
                            app.setId(doc.getId());
                            applications.add(app);
                        }
                    }
                    // 클라이언트 측 정렬 (최신순)
                    applications.sort((a1, a2) -> {
                        if (a1.getCreatedAt() == null && a2.getCreatedAt() == null) return 0;
                        if (a1.getCreatedAt() == null) return 1;
                        if (a2.getCreatedAt() == null) return -1;
                        return a2.getCreatedAt().compareTo(a1.getCreatedAt());
                    });
                    callback.onSuccess(applications);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 대기중인 중앙동아리 신청 수
     */
    public void getPendingCentralApplicationCount(CountCallback callback) {
        db.collection("central_club_applications")
                .whereEqualTo("status", com.example.clubmanagement.models.CentralClubApplication.STATUS_PENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(queryDocumentSnapshots.size());
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 중앙동아리 신청 승인
     */
    public void approveCentralApplication(String applicationId, String clubId, SimpleCallback callback) {
        // 먼저 신청 상태 업데이트
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", com.example.clubmanagement.models.CentralClubApplication.STATUS_APPROVED);
        updates.put("processedAt", Timestamp.now());

        db.collection("central_club_applications")
                .document(applicationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 동아리를 중앙동아리로 변경 (centralClub 필드 사용 - JavaBean 컨벤션)
                    db.collection("clubs")
                            .document(clubId)
                            .update("centralClub", true)
                            .addOnSuccessListener(aVoid2 -> {
                                // 캐러셀에 자동 등록
                                addCentralClubToCarousel(clubId, callback);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 중앙동아리를 캐러셀에 자동 등록 (빈 위치 찾아서 추가)
     */
    private void addCentralClubToCarousel(String clubId, SimpleCallback callback) {
        // 먼저 동아리 정보 가져오기
        db.collection("clubs").document(clubId).get()
                .addOnSuccessListener(clubDoc -> {
                    if (!clubDoc.exists()) {
                        callback.onSuccess(); // 동아리 정보가 없어도 승인은 성공
                        return;
                    }

                    String clubName = clubDoc.getString("name");
                    String clubDescription = clubDoc.getString("description");

                    // 현재 캐러셀 아이템들의 position 확인
                    db.collection("carousel_items")
                            .whereLessThanOrEqualTo("position", 2)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                // 사용 중인 position 확인
                                java.util.Set<Integer> usedPositions = new java.util.HashSet<>();
                                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                                    Long pos = doc.getLong("position");
                                    if (pos != null && pos >= 0 && pos <= 2) {
                                        usedPositions.add(pos.intValue());
                                    }
                                }

                                // 빈 position 찾기 (0, 1, 2 중에서)
                                int availablePosition = -1;
                                for (int i = 0; i <= 2; i++) {
                                    if (!usedPositions.contains(i)) {
                                        availablePosition = i;
                                        break;
                                    }
                                }

                                if (availablePosition == -1) {
                                    // 빈 자리가 없으면 캐러셀 추가 생략, 승인만 완료
                                    callback.onSuccess();
                                    return;
                                }

                                // 캐러셀 아이템 생성
                                java.util.Map<String, Object> carouselData = new java.util.HashMap<>();
                                carouselData.put("clubId", clubId);
                                carouselData.put("clubName", clubName);
                                carouselData.put("title", clubName != null ? clubName : "중앙동아리");
                                carouselData.put("description", clubDescription != null ? clubDescription : "중앙동아리입니다");
                                carouselData.put("position", availablePosition);
                                carouselData.put("backgroundColor", "#6200EE"); // 기본 보라색
                                carouselData.put("createdAt", Timestamp.now());

                                db.collection("carousel_items")
                                        .add(carouselData)
                                        .addOnSuccessListener(docRef -> callback.onSuccess())
                                        .addOnFailureListener(e -> callback.onSuccess()); // 캐러셀 추가 실패해도 승인은 성공
                            })
                            .addOnFailureListener(e -> callback.onSuccess()); // 조회 실패해도 승인은 성공
                })
                .addOnFailureListener(e -> callback.onSuccess()); // 동아리 조회 실패해도 승인은 성공
    }

    /**
     * 중앙동아리 신청 거절
     */
    public void rejectCentralApplication(String applicationId, String rejectReason, SimpleCallback callback) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", com.example.clubmanagement.models.CentralClubApplication.STATUS_REJECTED);
        updates.put("rejectReason", rejectReason);
        updates.put("processedAt", Timestamp.now());

        db.collection("central_club_applications")
                .document(applicationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 중앙동아리 신청 삭제
     */
    public void deleteCentralApplication(String applicationId, SimpleCallback callback) {
        db.collection("central_club_applications")
                .document(applicationId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Club Establishment Application Methods (일반동아리 설립 신청)
    // ========================================

    public interface ClubApplicationListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.ClubApplication> applications);
        void onFailure(Exception e);
    }

    /**
     * 일반동아리 설립 신청
     */
    public void submitClubApplication(com.example.clubmanagement.models.ClubApplication application, SimpleCallback callback) {
        String docId = db.collection("club_applications").document().getId();
        application.setId(docId);
        application.setCreatedAt(Timestamp.now());
        application.setStatus("pending");

        Log.d(TAG, "Submitting club application: " + application.getClubName() + ", docId: " + docId);

        // Map으로 변환해서 저장 (직렬화 문제 방지)
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", application.getId());
        data.put("clubName", application.getClubName());
        data.put("description", application.getDescription());
        data.put("purpose", application.getPurpose());
        data.put("activityPlan", application.getActivityPlan());
        data.put("applicantId", application.getApplicantId());
        data.put("applicantEmail", application.getApplicantEmail());
        data.put("applicantName", application.getApplicantName());
        data.put("status", application.getStatus());
        data.put("createdAt", application.getCreatedAt());

        db.collection("club_applications")
                .document(docId)
                .set(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Club application submitted successfully: " + docId);
                        callback.onSuccess();
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Club application submission failed: " + (e != null ? e.getMessage() : "unknown"), e);
                        callback.onFailure(e != null ? e : new Exception("Unknown error"));
                    }
                });
    }

    /**
     * 대기중인 일반동아리 설립 신청 목록 가져오기 (최고 관리자용)
     */
    public void getPendingClubApplications(ClubApplicationListCallback callback) {
        Log.d(TAG, "Getting pending club applications...");

        db.collection("club_applications")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Query successful, found " + queryDocumentSnapshots.size() + " documents");

                    java.util.List<com.example.clubmanagement.models.ClubApplication> applications = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Log.d(TAG, "Processing doc: " + doc.getId() + ", data: " + doc.getData());
                        com.example.clubmanagement.models.ClubApplication app =
                                doc.toObject(com.example.clubmanagement.models.ClubApplication.class);
                        if (app != null) {
                            app.setId(doc.getId());
                            applications.add(app);
                        }
                    }
                    // 클라이언트에서 createdAt 기준 내림차순 정렬
                    applications.sort((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    Log.d(TAG, "Returning " + applications.size() + " applications");
                    callback.onSuccess(applications);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get pending applications: " + e.getMessage(), e);
                    callback.onFailure(e);
                });
    }

    /**
     * 일반동아리 설립 신청 승인
     * 1. 신청 상태를 approved로 변경
     * 2. clubs 컬렉션에 새 동아리 추가
     * 3. 설립자를 첫 번째 멤버로 추가하고 관리자 권한 부여
     */
    public void approveClubApplication(com.example.clubmanagement.models.ClubApplication application, SimpleCallback callback) {
        // 1. 신청 상태 업데이트
        java.util.Map<String, Object> applicationUpdates = new java.util.HashMap<>();
        applicationUpdates.put("status", "approved");

        db.collection("club_applications")
                .document(application.getId())
                .update(applicationUpdates)
                .addOnSuccessListener(aVoid -> {
                    // 2. 새 동아리 생성
                    String clubId = "club_" + System.currentTimeMillis();

                    com.example.clubmanagement.models.Club newClub = new com.example.clubmanagement.models.Club(
                            clubId,
                            application.getClubName()
                    );
                    newClub.setPurpose(application.getPurpose());
                    newClub.setDescription(application.getDescription());
                    newClub.setCentralClub(false); // 일반동아리로 설정
                    newClub.setMemberCount(1); // 설립자 1명

                    db.collection("clubs")
                            .document(clubId)
                            .set(newClub)
                            .addOnSuccessListener(aVoid2 -> {
                                // 3. 설립자를 첫 번째 멤버로 추가 (관리자 권한 부여)
                                addFounderAsMember(clubId, application, callback);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 설립자를 동아리의 첫 번째 멤버로 추가 (관리자 권한 부여)
     */
    private void addFounderAsMember(String clubId, com.example.clubmanagement.models.ClubApplication application, SimpleCallback callback) {
        String founderId = application.getApplicantId();
        String founderEmail = application.getApplicantEmail();
        String founderName = application.getApplicantName();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA);
        String joinDate = sdf.format(new java.util.Date());

        java.util.Map<String, Object> memberData = new java.util.HashMap<>();
        memberData.put("userId", founderId);
        memberData.put("email", founderEmail);
        memberData.put("name", founderName);
        memberData.put("joinDate", joinDate);
        memberData.put("joinedAt", System.currentTimeMillis());
        memberData.put("isAdmin", true); // 설립자에게 관리자 권한 부여
        memberData.put("role", "회장"); // 설립자는 회장

        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(founderId)
                .set(memberData)
                .addOnSuccessListener(aVoid -> {
                    // 사용자 문서에도 일반동아리 가입 정보 추가
                    java.util.Map<String, Object> userUpdates = new java.util.HashMap<>();
                    userUpdates.put("generalClubIds", com.google.firebase.firestore.FieldValue.arrayUnion(clubId));
                    userUpdates.put("generalClubNames", com.google.firebase.firestore.FieldValue.arrayUnion(application.getClubName()));

                    db.collection("users")
                            .document(founderId)
                            .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onSuccess()); // 실패해도 동아리 생성은 성공
                })
                .addOnFailureListener(e -> callback.onSuccess()); // 멤버 추가 실패해도 동아리 생성은 성공
    }

    /**
     * 일반동아리 설립 신청 거절 (목록에서 삭제)
     */
    public void rejectClubApplication(String applicationId, SimpleCallback callback) {
        db.collection("club_applications")
                .document(applicationId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Withdrawal Request Methods (탈퇴 신청)
    // ========================================

    public interface WithdrawalRequestCallback {
        void onSuccess(com.example.clubmanagement.models.WithdrawalRequest request);
        void onFailure(Exception e);
    }

    public interface WithdrawalRequestListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.WithdrawalRequest> requests);
        void onFailure(Exception e);
    }

    /**
     * 탈퇴 신청 제출
     */
    public void submitWithdrawalRequest(com.example.clubmanagement.models.WithdrawalRequest request, SimpleCallback callback) {
        String docId = db.collection("withdrawal_requests").document().getId();
        request.setId(docId);

        db.collection("withdrawal_requests")
                .document(docId)
                .set(request)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 사용자의 대기중인 탈퇴 신청 가져오기
     */
    public void getPendingWithdrawalRequest(String clubId, String userId, WithdrawalRequestCallback callback) {
        db.collection("withdrawal_requests")
                .whereEqualTo("clubId", clubId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "pending")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        com.example.clubmanagement.models.WithdrawalRequest request =
                                doc.toObject(com.example.clubmanagement.models.WithdrawalRequest.class);
                        if (request != null) {
                            request.setId(doc.getId());
                        }
                        callback.onSuccess(request);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 동아리의 모든 대기중인 탈퇴 신청 목록 가져오기 (관리자용)
     */
    public void getWithdrawalRequests(String clubId, WithdrawalRequestListCallback callback) {
        db.collection("withdrawal_requests")
                .whereEqualTo("clubId", clubId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.WithdrawalRequest> requests = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.WithdrawalRequest request =
                                doc.toObject(com.example.clubmanagement.models.WithdrawalRequest.class);
                        if (request != null) {
                            request.setId(doc.getId());
                            requests.add(request);
                        }
                    }
                    // 클라이언트 측에서 날짜 기준으로 정렬 (최신순)
                    requests.sort((r1, r2) -> {
                        if (r1.getCreatedAt() == null && r2.getCreatedAt() == null) return 0;
                        if (r1.getCreatedAt() == null) return 1;
                        if (r2.getCreatedAt() == null) return -1;
                        return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                    });
                    callback.onSuccess(requests);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 탈퇴 신청 취소
     */
    public void cancelWithdrawalRequest(String requestId, SimpleCallback callback) {
        db.collection("withdrawal_requests")
                .document(requestId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 탈퇴 신청 승인 (멤버 삭제)
     */
    public void approveWithdrawalRequest(String requestId, String clubId, String userId, SimpleCallback callback) {
        // 1. 탈퇴 신청 상태 업데이트
        db.collection("withdrawal_requests")
                .document(requestId)
                .update("status", "approved")
                .addOnSuccessListener(aVoid -> {
                    // 2. 멤버에서 삭제
                    db.collection("clubs")
                            .document(clubId)
                            .collection("members")
                            .document(userId)
                            .delete()
                            .addOnSuccessListener(aVoid2 -> {
                                // 3. 멤버 카운트 감소
                                db.collection("clubs")
                                        .document(clubId)
                                        .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-1))
                                        .addOnSuccessListener(aVoid3 -> callback.onSuccess())
                                        .addOnFailureListener(e -> callback.onSuccess());
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 탈퇴 신청 거절
     */
    public void rejectWithdrawalRequest(String requestId, SimpleCallback callback) {
        db.collection("withdrawal_requests")
                .document(requestId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 회원 퇴출 (사유 기록 포함)
     */
    public void expelMemberWithReason(String clubId, String clubName, String userId, String reason, SimpleCallback callback) {
        // 1. 먼저 사용자의 퇴출 이력에 기록 추가 및 동아리 정보 제거
        Map<String, Object> expulsionRecord = new HashMap<>();
        expulsionRecord.put("clubId", clubId);
        expulsionRecord.put("clubName", clubName);
        expulsionRecord.put("reason", reason);
        expulsionRecord.put("expelledAt", System.currentTimeMillis());

        // 사용자 문서 업데이트: 퇴출 이력 추가 + 동아리 정보 제거
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("expulsionHistory", com.google.firebase.firestore.FieldValue.arrayUnion(expulsionRecord));
        userUpdates.put("centralClubId", com.google.firebase.firestore.FieldValue.delete());
        userUpdates.put("centralClubName", com.google.firebase.firestore.FieldValue.delete());

        db.collection("users")
                .document(userId)
                .update(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    // 2. 멤버에서 삭제
                    deleteMemberAndCleanup(clubId, userId, callback);
                })
                .addOnFailureListener(e -> {
                    // 사용자 문서가 없을 수 있으므로 생성 후 재시도
                    Map<String, Object> userData = new HashMap<>();
                    java.util.List<Map<String, Object>> history = new java.util.ArrayList<>();
                    history.add(expulsionRecord);
                    userData.put("expulsionHistory", history);
                    userData.put("uid", userId);

                    db.collection("users")
                            .document(userId)
                            .set(userData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                deleteMemberAndCleanup(clubId, userId, callback);
                            })
                            .addOnFailureListener(callback::onFailure);
                });
    }

    /**
     * 멤버 삭제 및 관련 데이터 정리 (퇴출용)
     */
    private void deleteMemberAndCleanup(String clubId, String userId, SimpleCallback callback) {
        // 멤버에서 삭제 - userId 필드로 검색하여 삭제 (문서 ID가 다를 수 있음)
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(memberSnapshots -> {
                    // 해당 사용자의 모든 멤버 문서 삭제
                    for (com.google.firebase.firestore.DocumentSnapshot doc : memberSnapshots) {
                        doc.getReference().delete();
                    }

                    // 기존 문서 ID로도 삭제 시도 (혹시 모를 경우 대비)
                    db.collection("clubs")
                            .document(clubId)
                            .collection("members")
                            .document(userId)
                            .delete();

                    // 멤버 카운트 감소
                    if (!memberSnapshots.isEmpty()) {
                        db.collection("clubs")
                                .document(clubId)
                                .update("memberCount", com.google.firebase.firestore.FieldValue.increment(-memberSnapshots.size()));
                    }

                    // 기존 가입 신청 상태를 "expelled"로 업데이트
                    db.collection("clubs")
                            .document(clubId)
                            .collection("membershipApplications")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                                    doc.getReference().update("status", "expelled");
                                }
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> callback.onSuccess());
                })
                .addOnFailureListener(e -> {
                    // 쿼리 실패해도 기존 방식으로 삭제 시도
                    db.collection("clubs")
                            .document(clubId)
                            .collection("members")
                            .document(userId)
                            .delete()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                });
    }

    /**
     * 사용자의 퇴출 이력 가져오기
     */
    public void getUserExpulsionHistory(String userId, UserCallback callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.clubmanagement.models.User user = documentSnapshot.toObject(com.example.clubmanagement.models.User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 동아리 완전 삭제 (모든 관련 데이터 삭제 및 부원들에게 알림)
     */
    public void deleteClubCompletely(String clubId, String clubName, SimpleCallback callback) {
        // 1. 먼저 동아리 멤버 목록을 가져옴
        getClubMembers(clubId, new MembersCallback() {
            @Override
            public void onSuccess(java.util.List<com.example.clubmanagement.models.Member> members) {
                // 2. 각 멤버에게 알림 저장 및 동아리 정보 제거
                for (com.example.clubmanagement.models.Member member : members) {
                    sendClubDeletionNotification(member.getUserId(), clubId, clubName);
                    removeClubFromUser(member.getUserId(), clubId);
                }

                // 3. users 컬렉션에서 이 동아리에 가입된 모든 사용자 찾아서 동아리 정보 제거
                removeClubFromAllUsers(clubId);

                // 4. 동아리의 서브컬렉션들 삭제
                deleteClubSubcollections(clubId, () -> {
                    // 5. 동아리 문서 삭제
                    db.collection("clubs")
                            .document(clubId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // 6. 관련 신청서 삭제
                                deleteClubApplications(clubId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(callback::onFailure);
                });
            }

            @Override
            public void onFailure(Exception e) {
                // 멤버 조회 실패해도 삭제 시도
                deleteClubSubcollections(clubId, () -> {
                    db.collection("clubs")
                            .document(clubId)
                            .delete()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                });
            }
        });
    }

    /**
     * 사용자에게 동아리 삭제 알림 저장
     */
    private void sendClubDeletionNotification(String userId, String clubId, String clubName) {
        if (userId == null) return;

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "club_deleted");
        notification.put("clubId", clubId);
        notification.put("clubName", clubName);
        notification.put("message", "'" + clubName + "' 동아리가 관리자에 의해 삭제되었습니다.");
        notification.put("createdAt", System.currentTimeMillis());
        notification.put("read", false);

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(notification)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send notification: " + e.getMessage()));
    }

    /**
     * 사용자에서 동아리 정보 제거
     */
    private void removeClubFromUser(String userId, String clubId) {
        if (userId == null) return;

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String centralClubId = doc.getString("centralClubId");

                        Map<String, Object> updates = new HashMap<>();

                        // 중앙동아리인 경우
                        if (clubId.equals(centralClubId)) {
                            updates.put("centralClubId", null);
                            updates.put("centralClubName", null);
                        }

                        // 일반동아리 목록에서 제거
                        updates.put("generalClubIds", com.google.firebase.firestore.FieldValue.arrayRemove(clubId));
                        updates.put("generalClubNames", com.google.firebase.firestore.FieldValue.arrayRemove(clubId));

                        if (!updates.isEmpty()) {
                            db.collection("users")
                                    .document(userId)
                                    .update(updates);
                        }
                    }
                });
    }

    /**
     * 모든 사용자에서 동아리 정보 제거
     */
    private void removeClubFromAllUsers(String clubId) {
        // centralClubId가 이 동아리인 사용자들
        db.collection("users")
                .whereEqualTo("centralClubId", clubId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("centralClubId", null);
                        updates.put("centralClubName", null);
                        doc.getReference().update(updates);
                    }
                });

        // generalClubIds에 이 동아리가 포함된 사용자들
        db.collection("users")
                .whereArrayContains("generalClubIds", clubId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update(
                                "generalClubIds", com.google.firebase.firestore.FieldValue.arrayRemove(clubId)
                        );
                    }
                });
    }

    /**
     * 동아리 서브컬렉션 삭제 (members, notices, banners 등)
     */
    private void deleteClubSubcollections(String clubId, Runnable onComplete) {
        String[] subcollections = {"members", "notices", "banners", "join_requests", "leave_requests"};
        final int[] completed = {0};

        for (String subcollection : subcollections) {
            db.collection("clubs")
                    .document(clubId)
                    .collection(subcollection)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            doc.getReference().delete();
                        }
                        completed[0]++;
                        if (completed[0] >= subcollections.length) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        completed[0]++;
                        if (completed[0] >= subcollections.length) {
                            onComplete.run();
                        }
                    });
        }
    }

    /**
     * 동아리 관련 신청서 삭제
     */
    private void deleteClubApplications(String clubId) {
        // 탈퇴 신청 삭제
        db.collection("withdrawal_requests")
                .whereEqualTo("clubId", clubId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });

        // 중앙동아리 신청 삭제
        db.collection("centralClubApplications")
                .whereEqualTo("clubId", clubId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });

        // 예산 거래 내역 삭제
        db.collection("budgetTransactions")
                .whereEqualTo("clubId", clubId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });
    }

    // ============ 중앙동아리 인원 제한 설정 ============

    /**
     * 인원 제한 콜백 인터페이스
     */
    public interface MemberLimitsCallback {
        void onSuccess(int registerLimit, int maintainLimit);
        void onFailure(Exception e);
    }

    /**
     * 중앙동아리 인원 제한 가져오기
     */
    public void getMemberLimits(MemberLimitsCallback callback) {
        db.collection("settings")
                .document("memberLimits")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long registerLimit = documentSnapshot.getLong("registerLimit");
                        Long maintainLimit = documentSnapshot.getLong("maintainLimit");

                        int register = registerLimit != null ? registerLimit.intValue() : 20;
                        int maintain = maintainLimit != null ? maintainLimit.intValue() : 15;

                        callback.onSuccess(register, maintain);
                    } else {
                        // 기본값 반환
                        callback.onSuccess(20, 15);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 중앙동아리 인원 제한 저장
     */
    public void saveMemberLimits(int registerLimit, int maintainLimit, SimpleCallback callback) {
        Map<String, Object> limits = new HashMap<>();
        limits.put("registerLimit", registerLimit);
        limits.put("maintainLimit", maintainLimit);
        limits.put("updatedAt", System.currentTimeMillis());

        db.collection("settings")
                .document("memberLimits")
                .set(limits)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 중앙동아리 등록 최소 인원 가져오기 (동기화 없이 캐시 사용 가능)
     */
    public void getRegisterMinMembers(CountCallback callback) {
        getMemberLimits(new MemberLimitsCallback() {
            @Override
            public void onSuccess(int registerLimit, int maintainLimit) {
                callback.onSuccess(registerLimit);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onSuccess(20); // 기본값
            }
        });
    }

    /**
     * 중앙동아리 유지 최소 인원 가져오기
     */
    public void getMaintainMinMembers(CountCallback callback) {
        getMemberLimits(new MemberLimitsCallback() {
            @Override
            public void onSuccess(int registerLimit, int maintainLimit) {
                callback.onSuccess(maintainLimit);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onSuccess(15); // 기본값
            }
        });
    }

    // ============ 사용자 가입 동아리 조회 ============

    /**
     * 사용자 가입 동아리 콜백 인터페이스
     */
    public interface UserClubsCallback {
        void onSuccess(String centralClubId, java.util.List<String> generalClubIds);
        void onFailure(Exception e);
    }

    /**
     * 사용자가 가입한 동아리 조회
     */
    public void getUserJoinedClubs(String userId, UserClubsCallback callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String centralClubId = documentSnapshot.getString("centralClubId");
                        java.util.List<String> generalClubIds = (java.util.List<String>) documentSnapshot.get("generalClubIds");
                        callback.onSuccess(centralClubId, generalClubIds);
                    } else {
                        callback.onSuccess(null, null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ============ 서명 업로드 관련 ============

    /**
     * URL 콜백 인터페이스
     */
    public interface UrlCallback {
        void onSuccess(String url);
        void onFailure(Exception e);
    }

    /**
     * 서명을 Firebase Storage에 업로드
     */
    public void uploadSignature(String userId, android.graphics.Bitmap signatureBitmap, UrlCallback callback) {
        if (userId == null || signatureBitmap == null) {
            callback.onFailure(new Exception("유효하지 않은 데이터"));
            return;
        }

        // Bitmap을 바이트 배열로 변환
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        signatureBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        // Firebase Storage에 업로드
        String path = "signatures/" + userId + "_" + System.currentTimeMillis() + ".png";
        com.google.firebase.storage.StorageReference signatureRef = storage.getReference().child(path);

        signatureRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    // 다운로드 URL 가져오기
                    signatureRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                // 사용자 문서에 서명 URL 저장
                                db.collection("users").document(userId)
                                        .update("signatureUrl", uri.toString())
                                        .addOnSuccessListener(aVoid -> callback.onSuccess(uri.toString()))
                                        .addOnFailureListener(e -> {
                                            // 문서가 없으면 set으로 생성
                                            java.util.Map<String, Object> userData = new java.util.HashMap<>();
                                            userData.put("signatureUrl", uri.toString());
                                            db.collection("users").document(userId)
                                                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                                                    .addOnSuccessListener(aVoid2 -> callback.onSuccess(uri.toString()))
                                                    .addOnFailureListener(callback::onFailure);
                                        });
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 기존 서명을 삭제하고 새 서명을 업로드
     */
    public void deleteAndReuploadSignature(String clubId, String userId, android.graphics.Bitmap signatureBitmap, UrlCallback callback) {
        if (userId == null || signatureBitmap == null) {
            callback.onFailure(new Exception("유효하지 않은 데이터"));
            return;
        }

        // 1. 먼저 clubs/{clubId}/members/{userId}에서 기존 서명 URL 가져오기
        db.collection("clubs").document(clubId).collection("members").document(userId)
                .get()
                .addOnSuccessListener(memberDoc -> {
                    String oldSignatureUrl = null;
                    if (memberDoc.exists()) {
                        oldSignatureUrl = memberDoc.getString("signatureUrl");
                    }

                    // 2. 기존 서명이 있으면 Storage에서 삭제
                    if (oldSignatureUrl != null && !oldSignatureUrl.isEmpty()) {
                        try {
                            com.google.firebase.storage.StorageReference oldRef = storage.getReferenceFromUrl(oldSignatureUrl);
                            oldRef.delete().addOnCompleteListener(task -> {
                                // 삭제 성공/실패 상관없이 새 서명 업로드 진행
                                uploadNewSignatureAndUpdate(clubId, userId, signatureBitmap, callback);
                            });
                        } catch (Exception e) {
                            // URL이 잘못된 경우 그냥 새 서명 업로드
                            uploadNewSignatureAndUpdate(clubId, userId, signatureBitmap, callback);
                        }
                    } else {
                        // 기존 서명이 없으면 바로 새 서명 업로드
                        uploadNewSignatureAndUpdate(clubId, userId, signatureBitmap, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    // 멤버 문서가 없어도 새 서명 업로드 진행
                    uploadNewSignatureAndUpdate(clubId, userId, signatureBitmap, callback);
                });
    }

    /**
     * 새 서명 업로드 및 Firestore 업데이트
     */
    private void uploadNewSignatureAndUpdate(String clubId, String userId, android.graphics.Bitmap signatureBitmap, UrlCallback callback) {
        // Bitmap을 바이트 배열로 변환
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        signatureBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();

        // Firebase Storage에 업로드
        String path = "signatures/" + userId + "_" + System.currentTimeMillis() + ".png";
        com.google.firebase.storage.StorageReference signatureRef = storage.getReference().child(path);

        signatureRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    signatureRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String newSignatureUrl = uri.toString();

                                // 1. users 컬렉션 업데이트
                                db.collection("users").document(userId)
                                        .update("signatureUrl", newSignatureUrl)
                                        .addOnFailureListener(e -> {
                                            // 문서가 없으면 set으로 생성
                                            java.util.Map<String, Object> userData = new java.util.HashMap<>();
                                            userData.put("signatureUrl", newSignatureUrl);
                                            db.collection("users").document(userId)
                                                    .set(userData, com.google.firebase.firestore.SetOptions.merge());
                                        });

                                // 2. clubs/{clubId}/members/{userId} 업데이트
                                db.collection("clubs").document(clubId).collection("members").document(userId)
                                        .update("signatureUrl", newSignatureUrl)
                                        .addOnSuccessListener(aVoid -> callback.onSuccess(newSignatureUrl))
                                        .addOnFailureListener(e -> {
                                            // 문서가 없으면 set으로 생성
                                            java.util.Map<String, Object> memberData = new java.util.HashMap<>();
                                            memberData.put("signatureUrl", newSignatureUrl);
                                            memberData.put("userId", userId);
                                            db.collection("clubs").document(clubId).collection("members").document(userId)
                                                    .set(memberData, com.google.firebase.firestore.SetOptions.merge())
                                                    .addOnSuccessListener(aVoid2 -> callback.onSuccess(newSignatureUrl))
                                                    .addOnFailureListener(callback::onFailure);
                                        });
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 중앙동아리 가입 (서명 포함)
     */
    public void joinCentralClubWithSignature(String clubId, String clubName, String signatureUrl, SimpleCallback callback) {
        joinCentralClubWithSignature(clubId, clubName, signatureUrl, 0, 0, callback);
    }

    /**
     * 중앙동아리 가입 (생일만 포함, 서명 없음)
     */
    public void joinCentralClubWithBirthday(String clubId, String clubName,
                                             int birthMonth, int birthDay, SimpleCallback callback) {
        joinCentralClubWithSignature(clubId, clubName, null, birthMonth, birthDay, callback);
    }

    /**
     * 중앙동아리 가입 (서명 및 생일 포함)
     */
    public void joinCentralClubWithSignature(String clubId, String clubName, String signatureUrl,
                                              int birthMonth, int birthDay, SimpleCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("centralClubId", clubId);
        updates.put("centralClubName", clubName);
        if (signatureUrl != null && !signatureUrl.isEmpty()) {
            updates.put("signatureUrl", signatureUrl);
        }
        if (birthMonth > 0 && birthDay > 0) {
            updates.put("birthMonth", birthMonth);
            updates.put("birthDay", birthDay);
        }

        db.collection("users")
                .document(userId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // clubs 컬렉션의 members 하위 컬렉션에도 추가
                    addMemberToClubWithBirthday(clubId, userId, signatureUrl, birthMonth, birthDay, callback);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 일반동아리 가입 (서명 포함)
     */
    public void joinGeneralClubWithSignature(String clubId, String clubName, String signatureUrl, SimpleCallback callback) {
        joinGeneralClubWithSignature(clubId, clubName, signatureUrl, 0, 0, callback);
    }

    /**
     * 일반동아리 가입 (생일만 포함, 서명 없음)
     */
    public void joinGeneralClubWithBirthday(String clubId, String clubName,
                                             int birthMonth, int birthDay, SimpleCallback callback) {
        joinGeneralClubWithSignature(clubId, clubName, null, birthMonth, birthDay, callback);
    }

    /**
     * 일반동아리 가입 (서명 및 생일 포함)
     */
    public void joinGeneralClubWithSignature(String clubId, String clubName, String signatureUrl,
                                              int birthMonth, int birthDay, SimpleCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    java.util.List<String> generalClubIds = new java.util.ArrayList<>();
                    java.util.List<String> generalClubNames = new java.util.ArrayList<>();

                    if (documentSnapshot.exists()) {
                        java.util.List<String> existingIds = (java.util.List<String>) documentSnapshot.get("generalClubIds");
                        java.util.List<String> existingNames = (java.util.List<String>) documentSnapshot.get("generalClubNames");

                        if (existingIds != null) generalClubIds.addAll(existingIds);
                        if (existingNames != null) generalClubNames.addAll(existingNames);
                    }

                    // 이미 가입되어 있는지 확인
                    if (!generalClubIds.contains(clubId)) {
                        generalClubIds.add(clubId);
                        generalClubNames.add(clubName);
                    }

                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("generalClubIds", generalClubIds);
                    updates.put("generalClubNames", generalClubNames);
                    if (signatureUrl != null && !signatureUrl.isEmpty()) {
                        updates.put("signatureUrl", signatureUrl);
                    }
                    if (birthMonth > 0 && birthDay > 0) {
                        updates.put("birthMonth", birthMonth);
                        updates.put("birthDay", birthDay);
                    }

                    db.collection("users")
                            .document(userId)
                            .set(updates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                // clubs 컬렉션의 members 하위 컬렉션에도 추가
                                addMemberToClubWithBirthday(clubId, userId, signatureUrl, birthMonth, birthDay, callback);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * clubs/{clubId}/members에 멤버 추가
     */
    private void addMemberToClub(String clubId, String odUserId, String signatureUrl, SimpleCallback callback) {
        addMemberToClubWithBirthday(clubId, odUserId, signatureUrl, 0, 0, callback);
    }

    /**
     * clubs/{clubId}/members에 멤버 추가 (생일 포함)
     */
    private void addMemberToClubWithBirthday(String clubId, String odUserId, String signatureUrl,
                                             int birthMonth, int birthDay, SimpleCallback callback) {
        db.collection("users").document(odUserId).get()
                .addOnSuccessListener(userDoc -> {
                    java.util.Map<String, Object> memberData = new java.util.HashMap<>();
                    memberData.put("userId", odUserId);
                    if (signatureUrl != null && !signatureUrl.isEmpty()) {
                        memberData.put("signatureUrl", signatureUrl);
                    }
                    memberData.put("joinedAt", com.google.firebase.Timestamp.now());

                    if (userDoc.exists()) {
                        memberData.put("name", userDoc.getString("name"));
                        memberData.put("email", userDoc.getString("email"));
                        memberData.put("department", userDoc.getString("department"));
                        memberData.put("studentId", userDoc.getString("studentId"));
                        memberData.put("phone", userDoc.getString("phone"));
                    }

                    // 생일 정보 추가
                    if (birthMonth > 0 && birthDay > 0) {
                        memberData.put("birthMonth", birthMonth);
                        memberData.put("birthDay", birthDay);
                    }

                    db.collection("clubs").document(clubId)
                            .collection("members").document(odUserId)
                            .set(memberData)
                            .addOnSuccessListener(aVoid -> {
                                // 단체 채팅방에 자동 참여
                                joinGroupChatRoom(clubId, new SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onSuccess();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // 채팅방 참여 실패해도 가입은 성공 처리
                                        callback.onSuccess();
                                    }
                                });
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(e -> {
                    // 사용자 정보 가져오기 실패해도 멤버 추가는 진행
                    java.util.Map<String, Object> memberData = new java.util.HashMap<>();
                    memberData.put("userId", odUserId);
                    if (signatureUrl != null && !signatureUrl.isEmpty()) {
                        memberData.put("signatureUrl", signatureUrl);
                    }
                    memberData.put("joinedAt", com.google.firebase.Timestamp.now());

                    // 생일 정보 추가
                    if (birthMonth > 0 && birthDay > 0) {
                        memberData.put("birthMonth", birthMonth);
                        memberData.put("birthDay", birthDay);
                    }

                    db.collection("clubs").document(clubId)
                            .collection("members").document(odUserId)
                            .set(memberData)
                            .addOnSuccessListener(aVoid -> {
                                // 단체 채팅방에 자동 참여
                                joinGroupChatRoom(clubId, new SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onSuccess();
                                    }

                                    @Override
                                    public void onFailure(Exception ex) {
                                        callback.onSuccess();
                                    }
                                });
                            })
                            .addOnFailureListener(callback::onFailure);
                });
    }

    /**
     * 동아리 멤버들의 서명 목록 가져오기
     */
    public void getClubMembersWithSignatures(String clubId, MembersCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.Member> members = new java.util.ArrayList<>();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.Member member = doc.toObject(com.example.clubmanagement.models.Member.class);
                        if (member != null) {
                            member.setUserId(doc.getId());
                            // signatureUrl 가져오기
                            String signatureUrl = doc.getString("signatureUrl");
                            member.setSignatureUrl(signatureUrl);
                            members.add(member);
                        }
                    }

                    callback.onSuccess(members);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Schedule Methods (일정 관리)
    // ========================================

    public interface ScheduleCallback {
        void onSuccess(com.example.clubmanagement.models.Schedule schedule);
        void onFailure(Exception e);
    }

    public interface ScheduleListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.Schedule> schedules);
        void onFailure(Exception e);
    }

    /**
     * 일정 추가
     */
    public void addSchedule(com.example.clubmanagement.models.Schedule schedule, ScheduleCallback callback) {
        String docId = db.collection("schedules").document().getId();
        schedule.setId(docId);
        schedule.setCreatedAt(Timestamp.now());

        db.collection("schedules")
                .document(docId)
                .set(schedule)
                .addOnSuccessListener(aVoid -> callback.onSuccess(schedule))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 동아리의 모든 일정 가져오기
     */
    public void getSchedules(String clubId, ScheduleListCallback callback) {
        db.collection("schedules")
                .whereEqualTo("clubId", clubId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.Schedule> schedules = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.Schedule schedule =
                                doc.toObject(com.example.clubmanagement.models.Schedule.class);
                        if (schedule != null) {
                            schedule.setId(doc.getId());
                            schedules.add(schedule);
                        }
                    }
                    // 날짜 기준 정렬 (가까운 순)
                    schedules.sort((s1, s2) -> {
                        if (s1.getEventDate() == null && s2.getEventDate() == null) return 0;
                        if (s1.getEventDate() == null) return 1;
                        if (s2.getEventDate() == null) return -1;
                        return s1.getEventDate().compareTo(s2.getEventDate());
                    });
                    callback.onSuccess(schedules);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 다가오는 일정 가져오기 (오늘 이후)
     */
    public void getUpcomingSchedules(String clubId, ScheduleListCallback callback) {
        Timestamp today = Timestamp.now();

        db.collection("schedules")
                .whereEqualTo("clubId", clubId)
                .whereGreaterThanOrEqualTo("eventDate", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.Schedule> schedules = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.Schedule schedule =
                                doc.toObject(com.example.clubmanagement.models.Schedule.class);
                        if (schedule != null) {
                            schedule.setId(doc.getId());
                            schedules.add(schedule);
                        }
                    }
                    // 날짜 기준 정렬 (가까운 순)
                    schedules.sort((s1, s2) -> {
                        if (s1.getEventDate() == null && s2.getEventDate() == null) return 0;
                        if (s1.getEventDate() == null) return 1;
                        if (s2.getEventDate() == null) return -1;
                        return s1.getEventDate().compareTo(s2.getEventDate());
                    });
                    callback.onSuccess(schedules);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 일정 삭제
     */
    public void deleteSchedule(String scheduleId, SimpleCallback callback) {
        db.collection("schedules")
                .document(scheduleId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 일정 수정
     */
    public void updateSchedule(com.example.clubmanagement.models.Schedule schedule, ScheduleCallback callback) {
        if (schedule.getId() == null) {
            callback.onFailure(new Exception("Schedule ID is required"));
            return;
        }

        db.collection("schedules")
                .document(schedule.getId())
                .set(schedule)
                .addOnSuccessListener(aVoid -> callback.onSuccess(schedule))
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Club Notice Methods (동아리 공지)
    // ========================================

    public interface ClubNoticeCallback {
        void onSuccess(com.example.clubmanagement.models.ClubNotice notice);
        void onFailure(Exception e);
    }

    public interface ClubNoticeListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.ClubNotice> notices);
        void onFailure(Exception e);
    }

    /**
     * 동아리 공지 목록 가져오기
     */
    public void getClubNotices(String clubId, ClubNoticeListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.ClubNotice> notices = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.ClubNotice notice =
                                doc.toObject(com.example.clubmanagement.models.ClubNotice.class);
                        if (notice != null) {
                            notice.setId(doc.getId());
                            notices.add(notice);
                        }
                    }
                    // 클라이언트 측 정렬: isPinned 내림차순 → createdAt 내림차순
                    notices.sort((n1, n2) -> {
                        // isPinned 비교 (true가 먼저)
                        if (n1.isPinned() != n2.isPinned()) {
                            return n1.isPinned() ? -1 : 1;
                        }
                        // createdAt 비교 (최신순)
                        if (n1.getCreatedAt() == null && n2.getCreatedAt() == null) return 0;
                        if (n1.getCreatedAt() == null) return 1;
                        if (n2.getCreatedAt() == null) return -1;
                        return n2.getCreatedAt().compareTo(n1.getCreatedAt());
                    });
                    callback.onSuccess(notices);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 공지 상세 가져오기
     */
    public void getClubNotice(String clubId, String noticeId, ClubNoticeCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.clubmanagement.models.ClubNotice notice =
                                documentSnapshot.toObject(com.example.clubmanagement.models.ClubNotice.class);
                        if (notice != null) {
                            notice.setId(documentSnapshot.getId());
                        }
                        callback.onSuccess(notice);
                    } else {
                        callback.onFailure(new Exception("공지를 찾을 수 없습니다"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 공지 작성 (알림 자동 생성 포함)
     */
    public void createClubNotice(com.example.clubmanagement.models.ClubNotice notice, String clubName, SimpleCallback callback) {
        String noticeId = db.collection("clubs").document(notice.getClubId())
                .collection("notices").document().getId();
        notice.setId(noticeId);

        db.collection("clubs")
                .document(notice.getClubId())
                .collection("notices")
                .document(noticeId)
                .set(notice)
                .addOnSuccessListener(aVoid -> {
                    // 동아리 모든 멤버에게 알림 생성
                    createNoticeNotificationsForMembers(notice.getClubId(), clubName, noticeId, notice.getTitle());
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 공지 수정
     */
    public void updateClubNotice(com.example.clubmanagement.models.ClubNotice notice, SimpleCallback callback) {
        notice.setUpdatedAt(com.google.firebase.Timestamp.now());

        db.collection("clubs")
                .document(notice.getClubId())
                .collection("notices")
                .document(notice.getId())
                .set(notice)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 공지 삭제
     */
    public void deleteClubNotice(String clubId, String noticeId, SimpleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 전체 공지 - 모든 동아리에 공지 추가
     */
    public interface GlobalNoticeCallback {
        void onProgress(int current, int total);
        void onSuccess(int totalSent);
        void onFailure(Exception e);
    }

    public void sendGlobalNotice(String title, String content, String authorId, String authorName, GlobalNoticeCallback callback) {
        // 모든 동아리 가져오기
        db.collection("clubs")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onFailure(new Exception("등록된 동아리가 없습니다"));
                        return;
                    }

                    int totalClubs = querySnapshot.size();
                    final int[] successCount = {0};
                    final int[] processedCount = {0};

                    for (com.google.firebase.firestore.DocumentSnapshot clubDoc : querySnapshot.getDocuments()) {
                        String clubId = clubDoc.getId();
                        String clubName = clubDoc.getString("name");
                        if (clubName == null) clubName = "동아리";

                        // 공지 생성
                        com.example.clubmanagement.models.ClubNotice notice =
                                new com.example.clubmanagement.models.ClubNotice(
                                        clubId,
                                        "[전체공지] " + title,
                                        content,
                                        authorId,
                                        authorName
                                );
                        notice.setPinned(true); // 전체 공지는 상단 고정

                        String finalClubName = clubName;
                        String noticeId = db.collection("clubs").document(clubId)
                                .collection("notices").document().getId();
                        notice.setId(noticeId);

                        db.collection("clubs")
                                .document(clubId)
                                .collection("notices")
                                .document(noticeId)
                                .set(notice)
                                .addOnSuccessListener(aVoid -> {
                                    successCount[0]++;
                                    processedCount[0]++;
                                    callback.onProgress(processedCount[0], totalClubs);

                                    // 해당 동아리 멤버들에게 알림 생성
                                    createNoticeNotificationsForMembers(clubId, finalClubName, noticeId, notice.getTitle());

                                    if (processedCount[0] == totalClubs) {
                                        callback.onSuccess(successCount[0]);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    processedCount[0]++;
                                    callback.onProgress(processedCount[0], totalClubs);

                                    if (processedCount[0] == totalClubs) {
                                        callback.onSuccess(successCount[0]);
                                    }
                                });
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 공지 조회수 증가
     */
    public void incrementNoticeViewCount(String clubId, String noticeId) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .update("viewCount", com.google.firebase.firestore.FieldValue.increment(1));
    }

    // ========================================
    // Notice Comment Methods (공지 댓글)
    // ========================================

    public interface NoticeCommentCallback {
        void onSuccess(com.example.clubmanagement.models.NoticeComment comment);
        void onFailure(Exception e);
    }

    public interface NoticeCommentListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.NoticeComment> comments);
        void onFailure(Exception e);
    }

    /**
     * 공지 댓글 목록 가져오기
     */
    public void getNoticeComments(String clubId, String noticeId, NoticeCommentListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .collection("comments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.NoticeComment> comments = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.NoticeComment comment =
                                doc.toObject(com.example.clubmanagement.models.NoticeComment.class);
                        if (comment != null) {
                            comment.setId(doc.getId());
                            comments.add(comment);
                        }
                    }
                    // 클라이언트 측 정렬 (createdAt 오름차순)
                    java.util.Collections.sort(comments, (c1, c2) -> {
                        if (c1.getCreatedAt() == null && c2.getCreatedAt() == null) return 0;
                        if (c1.getCreatedAt() == null) return 1;
                        if (c2.getCreatedAt() == null) return -1;
                        return c1.getCreatedAt().compareTo(c2.getCreatedAt());
                    });
                    callback.onSuccess(comments);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 댓글 작성
     */
    public void createNoticeComment(com.example.clubmanagement.models.NoticeComment comment, SimpleCallback callback) {
        // Null 체크
        if (comment.getClubId() == null || comment.getNoticeId() == null) {
            callback.onFailure(new Exception("clubId 또는 noticeId가 null입니다"));
            return;
        }

        String commentId = db.collection("clubs").document(comment.getClubId())
                .collection("notices").document(comment.getNoticeId())
                .collection("comments").document().getId();
        comment.setId(commentId);

        // Map으로 변환하여 저장 (POJO 직렬화 문제 방지)
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", comment.getId());
        commentData.put("noticeId", comment.getNoticeId());
        commentData.put("clubId", comment.getClubId());
        commentData.put("content", comment.getContent());
        commentData.put("authorId", comment.getAuthorId());
        commentData.put("authorName", comment.getAuthorName());
        commentData.put("createdAt", comment.getCreatedAt());
        commentData.put("updatedAt", comment.getUpdatedAt());
        commentData.put("edited", comment.isEdited());

        db.collection("clubs")
                .document(comment.getClubId())
                .collection("notices")
                .document(comment.getNoticeId())
                .collection("comments")
                .document(commentId)
                .set(commentData)
                .addOnSuccessListener(aVoid -> {
                    // 댓글 수 증가
                    db.collection("clubs")
                            .document(comment.getClubId())
                            .collection("notices")
                            .document(comment.getNoticeId())
                            .update("commentCount", com.google.firebase.firestore.FieldValue.increment(1));
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 댓글 수정
     */
    public void updateNoticeComment(com.example.clubmanagement.models.NoticeComment comment, SimpleCallback callback) {
        comment.setUpdatedAt(com.google.firebase.Timestamp.now());
        comment.setEdited(true);

        // Map으로 변환하여 저장 (POJO 직렬화 문제 방지)
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", comment.getId());
        commentData.put("noticeId", comment.getNoticeId());
        commentData.put("clubId", comment.getClubId());
        commentData.put("content", comment.getContent());
        commentData.put("authorId", comment.getAuthorId());
        commentData.put("authorName", comment.getAuthorName());
        commentData.put("createdAt", comment.getCreatedAt());
        commentData.put("updatedAt", comment.getUpdatedAt());
        commentData.put("edited", comment.isEdited());

        db.collection("clubs")
                .document(comment.getClubId())
                .collection("notices")
                .document(comment.getNoticeId())
                .collection("comments")
                .document(comment.getId())
                .set(commentData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 댓글 삭제
     */
    public void deleteNoticeComment(String clubId, String noticeId, String commentId, SimpleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("notices")
                .document(noticeId)
                .collection("comments")
                .document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // 댓글 수 감소
                    db.collection("clubs")
                            .document(clubId)
                            .collection("notices")
                            .document(noticeId)
                            .update("commentCount", com.google.firebase.firestore.FieldValue.increment(-1));
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Club Notification Methods (내부 알림)
    // ========================================

    public interface ClubNotificationCallback {
        void onSuccess(com.example.clubmanagement.models.ClubNotification notification);
        void onFailure(Exception e);
    }

    public interface ClubNotificationListCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.ClubNotification> notifications);
        void onFailure(Exception e);
    }

    /**
     * 사용자 알림 목록 가져오기
     */
    public void getUserNotifications(String userId, ClubNotificationListCallback callback) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.ClubNotification> notifications = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.ClubNotification notification =
                                doc.toObject(com.example.clubmanagement.models.ClubNotification.class);
                        if (notification != null) {
                            notification.setId(doc.getId());
                            notifications.add(notification);
                        }
                    }
                    // 클라이언트 측 정렬 (최신순) 및 50개 제한
                    notifications.sort((n1, n2) -> {
                        if (n1.getCreatedAt() == null && n2.getCreatedAt() == null) return 0;
                        if (n1.getCreatedAt() == null) return 1;
                        if (n2.getCreatedAt() == null) return -1;
                        return n2.getCreatedAt().compareTo(n1.getCreatedAt());
                    });
                    if (notifications.size() > 50) {
                        notifications = notifications.subList(0, 50);
                    }
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 읽지 않은 알림 개수 가져오기
     */
    public void getUnreadNotificationCount(String userId, CountCallback callback) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(queryDocumentSnapshots.size());
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    /**
     * 알림 읽음 처리
     */
    public void markNotificationAsRead(String notificationId, SimpleCallback callback) {
        db.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 모든 알림 읽음 처리
     */
    public void markAllNotificationsAsRead(String userId, SimpleCallback callback) {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "isRead", true);
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 알림 삭제
     */
    public void deleteNotification(String notificationId, SimpleCallback callback) {
        db.collection("notifications")
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 공지 작성 시 모든 멤버에게 알림 생성
     */
    private void createNoticeNotificationsForMembers(String clubId, String clubName, String noticeId, String noticeTitle) {
        String currentUserId = getCurrentUserId();

        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String memberId = doc.getId();

                        // 작성자 본인에게는 알림 안 보냄
                        if (memberId.equals(currentUserId)) continue;

                        com.example.clubmanagement.models.ClubNotification notification =
                                com.example.clubmanagement.models.ClubNotification.createNoticeNotification(
                                        memberId, clubId, clubName, noticeId, noticeTitle
                                );

                        String notificationId = db.collection("notifications").document().getId();
                        notification.setId(notificationId);

                        batch.set(db.collection("notifications").document(notificationId), notification);
                    }

                    batch.commit();
                });
    }

    // ============================================
    // 회원 가입 신청 (승인 대기) 관련 메서드
    // ============================================

    /**
     * 동아리 가입 신청 생성 (승인 대기 상태)
     */
    public void createMembershipApplication(String clubId, String clubName, String name,
                                             String department, String studentId, String phone,
                                             String email, int birthMonth, int birthDay,
                                             boolean isCentralClub, SimpleCallback callback) {
        Log.d(TAG, "createMembershipApplication - clubId: " + clubId + ", clubName: " + clubName);
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        // 이미 신청했는지 확인
        db.collection("clubs")
                .document(clubId)
                .collection("membershipApplications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        callback.onFailure(new Exception("이미 가입 신청 중입니다"));
                        return;
                    }

                    // 가입 신청 데이터 생성
                    java.util.Map<String, Object> application = new java.util.HashMap<>();
                    application.put("userId", userId);
                    application.put("clubId", clubId);
                    application.put("clubName", clubName);
                    application.put("name", name);
                    application.put("department", department);
                    application.put("studentId", studentId);
                    application.put("phone", phone);
                    application.put("email", email);
                    application.put("birthMonth", birthMonth);
                    application.put("birthDay", birthDay);
                    application.put("isCentralClub", isCentralClub);
                    application.put("status", "pending"); // pending, approved, rejected
                    application.put("appliedAt", com.google.firebase.Timestamp.now());
                    application.put("reviewedAt", null);
                    application.put("reviewedBy", null);

                    // Firebase에 저장
                    db.collection("clubs")
                            .document(clubId)
                            .collection("membershipApplications")
                            .add(application)
                            .addOnSuccessListener(documentReference -> {
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> callback.onFailure(e));
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    /**
     * 가입 신청 승인
     */
    public void approveMembershipApplication(String clubId, String applicationId,
                                              SimpleCallback callback) {
        String reviewerId = getCurrentUserId();

        db.collection("clubs")
                .document(clubId)
                .collection("membershipApplications")
                .document(applicationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onFailure(new Exception("신청 정보를 찾을 수 없습니다"));
                        return;
                    }

                    String userId = documentSnapshot.getString("userId");
                    String clubName = documentSnapshot.getString("clubName");
                    String name = documentSnapshot.getString("name");
                    String department = documentSnapshot.getString("department");
                    String studentId = documentSnapshot.getString("studentId");
                    String phone = documentSnapshot.getString("phone");
                    String email = documentSnapshot.getString("email");
                    Long birthMonthLong = documentSnapshot.getLong("birthMonth");
                    Long birthDayLong = documentSnapshot.getLong("birthDay");
                    Boolean isCentralClub = documentSnapshot.getBoolean("centralClub");

                    int birthMonth = birthMonthLong != null ? birthMonthLong.intValue() : 0;
                    int birthDay = birthDayLong != null ? birthDayLong.intValue() : 0;

                    // 멤버로 추가
                    java.util.Map<String, Object> member = new java.util.HashMap<>();
                    member.put("userId", userId);
                    member.put("name", name);
                    member.put("department", department);
                    member.put("studentId", studentId);
                    member.put("phone", phone);
                    member.put("email", email);
                    member.put("birthMonth", birthMonth);
                    member.put("birthDay", birthDay);
                    member.put("role", "부원");
                    member.put("isAdmin", false);
                    member.put("joinedAt", com.google.firebase.Timestamp.now());

                    db.collection("clubs")
                            .document(clubId)
                            .collection("members")
                            .document(userId)
                            .set(member)
                            .addOnSuccessListener(aVoid -> {
                                // 사용자 문서에 동아리 정보 업데이트
                                java.util.Map<String, Object> userUpdates = new java.util.HashMap<>();
                                if (isCentralClub != null && isCentralClub) {
                                    // 중앙동아리인 경우
                                    userUpdates.put("centralClubId", clubId);
                                    userUpdates.put("centralClubName", clubName);
                                } else {
                                    // 일반동아리인 경우
                                    userUpdates.put("generalClubIds", com.google.firebase.firestore.FieldValue.arrayUnion(clubId));
                                    userUpdates.put("generalClubNames", com.google.firebase.firestore.FieldValue.arrayUnion(clubName));
                                }

                                db.collection("users")
                                        .document(userId)
                                        .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener(aVoid1 -> {
                                            // 신청 상태 업데이트
                                            java.util.Map<String, Object> updates = new java.util.HashMap<>();
                                            updates.put("status", "approved");
                                            updates.put("reviewedAt", com.google.firebase.Timestamp.now());
                                            updates.put("reviewedBy", reviewerId);
                                            updates.put("userNotified", false); // 사용자에게 알림 안 함 상태

                                            db.collection("clubs")
                                                    .document(clubId)
                                                    .collection("membershipApplications")
                                                    .document(applicationId)
                                                    .update(updates)
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        // 단체 채팅방에 사용자 추가
                                                        addUserToGroupChatRoom(clubId, clubName, userId, new SimpleCallback() {
                                                            @Override
                                                            public void onSuccess() {
                                                                callback.onSuccess();
                                                            }

                                                            @Override
                                                            public void onFailure(Exception e) {
                                                                // 채팅방 추가 실패해도 가입 승인은 성공
                                                                callback.onSuccess();
                                                            }
                                                        });
                                                    })
                                                    .addOnFailureListener(callback::onFailure);
                                        })
                                        .addOnFailureListener(callback::onFailure);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 가입 신청 거절
     */
    public void rejectMembershipApplication(String clubId, String applicationId,
                                             String reason, SimpleCallback callback) {
        String reviewerId = getCurrentUserId();

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", "rejected");
        updates.put("reviewedAt", com.google.firebase.Timestamp.now());
        updates.put("reviewedBy", reviewerId);
        updates.put("rejectReason", reason);

        db.collection("clubs")
                .document(clubId)
                .collection("membershipApplications")
                .document(applicationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 대기 중인 가입 신청 목록 조회 (관리자용)
     */
    public void getPendingMembershipApplications(String clubId, MembershipApplicationListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("membershipApplications")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<java.util.Map<String, Object>> applications = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        java.util.Map<String, Object> app = doc.getData();
                        if (app != null) {
                            app.put("id", doc.getId());
                            applications.add(app);
                        }
                    }
                    // 클라이언트 측 정렬 (appliedAt 오름차순)
                    java.util.Collections.sort(applications, (a1, a2) -> {
                        Object t1 = a1.get("appliedAt");
                        Object t2 = a2.get("appliedAt");
                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        if (t1 instanceof com.google.firebase.Timestamp && t2 instanceof com.google.firebase.Timestamp) {
                            return ((com.google.firebase.Timestamp) t1).compareTo((com.google.firebase.Timestamp) t2);
                        }
                        return 0;
                    });
                    callback.onSuccess(applications);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 사용자의 가입 신청 상태 확인
     */
    public void checkMembershipApplicationStatus(String clubId, MembershipStatusCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onResult("none", null);
            return;
        }

        db.collection("clubs")
                .document(clubId)
                .collection("membershipApplications")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onResult("none", null);
                    } else {
                        // 클라이언트 측에서 가장 최근 신청 찾기 (appliedAt 내림차순)
                        com.google.firebase.firestore.DocumentSnapshot mostRecent = null;
                        com.google.firebase.Timestamp mostRecentTime = null;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            com.google.firebase.Timestamp appliedAt = doc.getTimestamp("appliedAt");
                            if (mostRecent == null || (appliedAt != null && (mostRecentTime == null || appliedAt.compareTo(mostRecentTime) > 0))) {
                                mostRecent = doc;
                                mostRecentTime = appliedAt;
                            }
                        }
                        if (mostRecent != null) {
                            String status = mostRecent.getString("status");
                            callback.onResult(status != null ? status : "none", mostRecent.getId());
                        } else {
                            callback.onResult("none", null);
                        }
                    }
                })
                .addOnFailureListener(e -> callback.onResult("none", null));
    }

    // 가입 신청 목록 콜백
    public interface MembershipApplicationListCallback {
        void onSuccess(java.util.List<java.util.Map<String, Object>> applications);
        void onFailure(Exception e);
    }

    // 가입 상태 확인 콜백
    public interface MembershipStatusCallback {
        void onResult(String status, String applicationId);
    }

    /**
     * 승인되었지만 알림을 받지 않은 가입 신청 확인
     * 사용자가 로그인 시 승인된 동아리가 있는지 확인
     */
    public void checkApprovedMembershipApplication(ApprovedApplicationCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onResult(null);
            return;
        }

        // 모든 동아리의 membershipApplications에서 승인되었지만 알림을 보지 않은 것을 찾음
        db.collectionGroup("membershipApplications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "approved")
                .whereEqualTo("userNotified", false)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onResult(null);
                    } else {
                        com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        java.util.Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("applicationDocId", doc.getId());
                            data.put("applicationDocPath", doc.getReference().getPath());
                            callback.onResult(data);
                        } else {
                            callback.onResult(null);
                        }
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    /**
     * 가입 승인 알림 확인 완료 표시
     */
    public void markMembershipApplicationNotified(String applicationPath, SimpleCallback callback) {
        db.document(applicationPath)
                .update("userNotified", true)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // 승인된 가입 신청 콜백
    public interface ApprovedApplicationCallback {
        void onResult(java.util.Map<String, Object> application);
    }

    /**
     * 사용자가 멤버로 속한 동아리 찾기 (collectionGroup 쿼리)
     * centralClubId가 설정되지 않은 기존 사용자를 위한 fallback
     */
    public interface ClubMembershipCallback {
        void onResult(String clubId, String clubName, boolean isCentralClub);
    }

    public void findUserClubMembership(ClubMembershipCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onResult(null, null, false);
            return;
        }

        // 모든 동아리의 members 컬렉션에서 현재 사용자 찾기
        db.collectionGroup("members")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onResult(null, null, false);
                    } else {
                        com.google.firebase.firestore.DocumentSnapshot memberDoc = queryDocumentSnapshots.getDocuments().get(0);
                        // 문서 경로에서 clubId 추출: clubs/{clubId}/members/{userId}
                        String path = memberDoc.getReference().getPath();
                        String[] parts = path.split("/");
                        if (parts.length >= 2) {
                            String clubId = parts[1]; // clubs/{clubId}/members/...

                            // 동아리 정보 가져오기
                            db.collection("clubs")
                                    .document(clubId)
                                    .get()
                                    .addOnSuccessListener(clubDoc -> {
                                        if (clubDoc.exists()) {
                                            String clubName = clubDoc.getString("name");
                                            Boolean isCentral = clubDoc.getBoolean("centralClub");

                                            // 사용자 문서 업데이트 (다음번에는 바로 이동하도록)
                                            if (isCentral != null && isCentral) {
                                                Map<String, Object> updates = new HashMap<>();
                                                updates.put("centralClubId", clubId);
                                                updates.put("centralClubName", clubName);
                                                db.collection("users").document(userId)
                                                        .set(updates, com.google.firebase.firestore.SetOptions.merge());
                                            }

                                            callback.onResult(clubId, clubName, isCentral != null && isCentral);
                                        } else {
                                            callback.onResult(null, null, false);
                                        }
                                    })
                                    .addOnFailureListener(e -> callback.onResult(null, null, false));
                        } else {
                            callback.onResult(null, null, false);
                        }
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null, null, false));
    }

    // ============================================
    // 가입 신청 설정 관련 메서드
    // ============================================

    /**
     * 가입 신청 설정 업데이트 (열기/닫기)
     */
    public void updateApplicationSettings(String clubId, boolean isOpen, com.google.firebase.Timestamp endDate, SimpleCallback callback) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("applicationOpen", isOpen);
        if (endDate != null) {
            updates.put("applicationEndDate", endDate);
        } else {
            updates.put("applicationEndDate", com.google.firebase.firestore.FieldValue.delete());
        }

        db.collection("clubs")
                .document(clubId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 가입 신청 중단하기
     */
    public void stopApplications(String clubId, SimpleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .update("applicationOpen", false)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 가입 신청 다시 받기
     */
    public void resumeApplications(String clubId, SimpleCallback callback) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("applicationOpen", true);
        updates.put("applicationEndDate", com.google.firebase.firestore.FieldValue.delete());

        db.collection("clubs")
                .document(clubId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 가입 신청 기간 설정
     */
    public void setApplicationPeriod(String clubId, com.google.firebase.Timestamp endDate, SimpleCallback callback) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("applicationOpen", true);
        updates.put("applicationEndDate", endDate);

        db.collection("clubs")
                .document(clubId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 동아리 가입 신청 설정 가져오기
     */
    public void getApplicationSettings(String clubId, ApplicationSettingsCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isOpen = documentSnapshot.getBoolean("applicationOpen");
                        com.google.firebase.Timestamp endDate = documentSnapshot.getTimestamp("applicationEndDate");
                        callback.onResult(isOpen != null ? isOpen : true, endDate);
                    } else {
                        callback.onResult(true, null);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(true, null));
    }

    public interface ApplicationSettingsCallback {
        void onResult(boolean isOpen, com.google.firebase.Timestamp endDate);
    }

    /**
     * 동아리 가입 신청 설정 저장하기
     */
    public void setApplicationSettings(String clubId, boolean isOpen, com.google.firebase.Timestamp endDate, SimpleCallback callback) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("applicationOpen", isOpen);
        if (endDate != null) {
            updates.put("applicationEndDate", endDate);
        } else {
            updates.put("applicationEndDate", com.google.firebase.firestore.FieldValue.delete());
        }

        db.collection("clubs")
                .document(clubId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // ========================================
    // ChatRoom Methods
    // ========================================

    public interface ChatRoomCallback {
        void onSuccess(com.example.clubmanagement.models.ChatRoom chatRoom);
        void onFailure(Exception e);
    }

    public interface ChatRoomsCallback {
        void onSuccess(java.util.List<com.example.clubmanagement.models.ChatRoom> chatRooms);
        void onFailure(Exception e);
    }

    /**
     * 채팅방 생성 또는 기존 채팅방 반환
     */
    public void createOrGetChatRoom(String partnerUserId, String partnerName, String partnerRole, String clubId, String clubName, ChatRoomCallback callback) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            callback.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        // 디버그 로그
        android.util.Log.d("FirebaseManager", "createOrGetChatRoom - currentUserId: " + currentUserId + ", partnerUserId: " + partnerUserId + ", partnerName: " + partnerName);

        if (partnerUserId == null || partnerUserId.isEmpty()) {
            callback.onFailure(new Exception("상대방 ID가 없습니다"));
            return;
        }

        // 채팅방 ID 생성 (두 사용자 ID를 정렬하여 항상 같은 ID 생성)
        String chatRoomId;
        if (currentUserId.compareTo(partnerUserId) < 0) {
            chatRoomId = currentUserId + "_" + partnerUserId + "_" + clubId;
        } else {
            chatRoomId = partnerUserId + "_" + currentUserId + "_" + clubId;
        }

        // 기존 채팅방 확인
        db.collection("chatRooms")
                .document(chatRoomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 기존 채팅방이 있는 경우
                        java.util.List<String> participants = (java.util.List<String>) documentSnapshot.get("participants");
                        java.util.Map<String, Object> user1 = (java.util.Map<String, Object>) documentSnapshot.get("user1");
                        java.util.Map<String, Object> user2 = (java.util.Map<String, Object>) documentSnapshot.get("user2");

                        // 업데이트가 필요한지 확인
                        boolean needsUpdate = (participants == null || !participants.contains(currentUserId) || !participants.contains(partnerUserId));
                        boolean needsUserInfo = (user1 == null || user2 == null);
                        String leftUserId = documentSnapshot.getString("leftUserId");
                        boolean needsClearLeftUser = (leftUserId != null && !leftUserId.isEmpty());

                        if (needsUpdate || needsUserInfo || needsClearLeftUser) {
                            // 현재 사용자 정보 가져오기
                            getCurrentUser(new UserCallback() {
                                @Override
                                public void onSuccess(com.example.clubmanagement.models.User currentUser) {
                                    String currentUserName = currentUser != null ? currentUser.getName() : "알 수 없음";

                                    getMemberRole(clubId, currentUserId, new RoleCallback() {
                                        @Override
                                        public void onSuccess(String currentUserRole) {
                                            java.util.Map<String, Object> updateData = new java.util.HashMap<>();
                                            updateData.put("participants", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId, partnerUserId));
                                            updateData.put("leftUserId", com.google.firebase.firestore.FieldValue.delete());

                                            // user1/user2 정보가 없으면 추가
                                            if (needsUserInfo) {
                                                java.util.Map<String, Object> user1Info = new java.util.HashMap<>();
                                                user1Info.put("userId", currentUserId);
                                                user1Info.put("name", currentUserName);
                                                user1Info.put("role", currentUserRole != null ? currentUserRole : "회원");
                                                updateData.put("user1", user1Info);

                                                java.util.Map<String, Object> user2Info = new java.util.HashMap<>();
                                                user2Info.put("userId", partnerUserId);
                                                user2Info.put("name", partnerName);
                                                user2Info.put("role", partnerRole != null ? partnerRole : "회원");
                                                updateData.put("user2", user2Info);
                                            }

                                            db.collection("chatRooms")
                                                    .document(chatRoomId)
                                                    .update(updateData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        com.example.clubmanagement.models.ChatRoom chatRoom = new com.example.clubmanagement.models.ChatRoom(
                                                                chatRoomId, partnerUserId, partnerName, partnerRole, clubId, clubName);
                                                        callback.onSuccess(chatRoom);
                                                    })
                                                    .addOnFailureListener(callback::onFailure);
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            // 직급 조회 실패해도 업데이트 진행
                                            java.util.Map<String, Object> updateData = new java.util.HashMap<>();
                                            updateData.put("participants", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId, partnerUserId));
                                            updateData.put("leftUserId", com.google.firebase.firestore.FieldValue.delete());

                                            if (needsUserInfo) {
                                                java.util.Map<String, Object> user1Info = new java.util.HashMap<>();
                                                user1Info.put("userId", currentUserId);
                                                user1Info.put("name", currentUserName);
                                                user1Info.put("role", "회원");
                                                updateData.put("user1", user1Info);

                                                java.util.Map<String, Object> user2Info = new java.util.HashMap<>();
                                                user2Info.put("userId", partnerUserId);
                                                user2Info.put("name", partnerName);
                                                user2Info.put("role", partnerRole != null ? partnerRole : "회원");
                                                updateData.put("user2", user2Info);
                                            }

                                            db.collection("chatRooms")
                                                    .document(chatRoomId)
                                                    .update(updateData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        com.example.clubmanagement.models.ChatRoom chatRoom = new com.example.clubmanagement.models.ChatRoom(
                                                                chatRoomId, partnerUserId, partnerName, partnerRole, clubId, clubName);
                                                        callback.onSuccess(chatRoom);
                                                    })
                                                    .addOnFailureListener(callback::onFailure);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    callback.onFailure(new Exception("사용자 정보를 가져올 수 없습니다"));
                                }
                            });
                        } else {
                            // 이미 participants에 있고 user 정보도 있으면 그냥 반환
                            com.example.clubmanagement.models.ChatRoom chatRoom = new com.example.clubmanagement.models.ChatRoom(
                                    chatRoomId, partnerUserId, partnerName, partnerRole, clubId, clubName);
                            callback.onSuccess(chatRoom);
                        }
                    } else {
                        // 현재 사용자 정보 가져오기
                        getCurrentUser(new UserCallback() {
                            @Override
                            public void onSuccess(com.example.clubmanagement.models.User currentUser) {
                                String currentUserName = currentUser != null ? currentUser.getName() : "알 수 없음";

                                // 현재 사용자의 직급 가져오기
                                getMemberRole(clubId, currentUserId, new RoleCallback() {
                                    @Override
                                    public void onSuccess(String currentUserRole) {
                                        // 새 채팅방 생성
                                        com.example.clubmanagement.models.ChatRoom newChatRoom = new com.example.clubmanagement.models.ChatRoom(
                                                chatRoomId, partnerUserId, partnerName, partnerRole, clubId, clubName);

                                        java.util.Map<String, Object> chatRoomData = new java.util.HashMap<>();
                                        chatRoomData.put("chatRoomId", chatRoomId);
                                        chatRoomData.put("clubId", clubId);
                                        chatRoomData.put("clubName", clubName);
                                        chatRoomData.put("lastMessage", "");
                                        chatRoomData.put("lastMessageTime", System.currentTimeMillis());
                                        chatRoomData.put("unreadCount", 0);
                                        chatRoomData.put("notificationEnabled", true);
                                        chatRoomData.put("leftUserId", null);
                                        chatRoomData.put("participants", java.util.Arrays.asList(currentUserId, partnerUserId));

                                        // 디버그 로그
                                        android.util.Log.d("FirebaseManager", "Saving chatRoom - participants: [" + currentUserId + ", " + partnerUserId + "]");

                                        // 양쪽 사용자 정보 저장 (각 사용자별로 상대방 정보 조회 가능하게)
                                        java.util.Map<String, Object> user1Info = new java.util.HashMap<>();
                                        user1Info.put("userId", currentUserId);
                                        user1Info.put("name", currentUserName);
                                        user1Info.put("role", currentUserRole != null ? currentUserRole : "회원");
                                        chatRoomData.put("user1", user1Info);

                                        java.util.Map<String, Object> user2Info = new java.util.HashMap<>();
                                        user2Info.put("userId", partnerUserId);
                                        user2Info.put("name", partnerName);
                                        user2Info.put("role", partnerRole != null ? partnerRole : "회원");
                                        chatRoomData.put("user2", user2Info);

                                        db.collection("chatRooms")
                                                .document(chatRoomId)
                                                .set(chatRoomData)
                                                .addOnSuccessListener(aVoid -> callback.onSuccess(newChatRoom))
                                                .addOnFailureListener(callback::onFailure);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        // 직급 조회 실패해도 채팅방 생성
                                        com.example.clubmanagement.models.ChatRoom newChatRoom = new com.example.clubmanagement.models.ChatRoom(
                                                chatRoomId, partnerUserId, partnerName, partnerRole, clubId, clubName);

                                        java.util.Map<String, Object> chatRoomData = new java.util.HashMap<>();
                                        chatRoomData.put("chatRoomId", chatRoomId);
                                        chatRoomData.put("clubId", clubId);
                                        chatRoomData.put("clubName", clubName);
                                        chatRoomData.put("lastMessage", "");
                                        chatRoomData.put("lastMessageTime", System.currentTimeMillis());
                                        chatRoomData.put("unreadCount", 0);
                                        chatRoomData.put("notificationEnabled", true);
                                        chatRoomData.put("leftUserId", null);
                                        chatRoomData.put("participants", java.util.Arrays.asList(currentUserId, partnerUserId));

                                        java.util.Map<String, Object> user1Info = new java.util.HashMap<>();
                                        user1Info.put("userId", currentUserId);
                                        user1Info.put("name", currentUserName);
                                        user1Info.put("role", "회원");
                                        chatRoomData.put("user1", user1Info);

                                        java.util.Map<String, Object> user2Info = new java.util.HashMap<>();
                                        user2Info.put("userId", partnerUserId);
                                        user2Info.put("name", partnerName);
                                        user2Info.put("role", partnerRole != null ? partnerRole : "회원");
                                        chatRoomData.put("user2", user2Info);

                                        db.collection("chatRooms")
                                                .document(chatRoomId)
                                                .set(chatRoomData)
                                                .addOnSuccessListener(aVoid -> callback.onSuccess(newChatRoom))
                                                .addOnFailureListener(callback::onFailure);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(new Exception("사용자 정보를 가져올 수 없습니다"));
                            }
                        });
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 현재 사용자의 모든 채팅방 목록 가져오기
     */
    public void getChatRooms(ChatRoomsCallback callback) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            callback.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        // 복합 인덱스 없이 조회 후 클라이언트에서 정렬
        db.collection("chatRooms")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.ChatRoom> chatRooms = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String docId = doc.getId();

                        // 수동으로 ChatRoom 객체 생성 (toObject 대신)
                        com.example.clubmanagement.models.ChatRoom chatRoom = new com.example.clubmanagement.models.ChatRoom();
                        chatRoom.setChatRoomId(docId);

                        // 공통 필드 설정
                        String clubId = doc.getString("clubId");
                        String clubName = doc.getString("clubName");
                        String lastMessage = doc.getString("lastMessage");
                        Long lastMessageTime = doc.getLong("lastMessageTime");
                        Long unreadCount = doc.getLong("unreadCount");
                        Boolean notificationEnabled = doc.getBoolean("notificationEnabled");
                        String leftUserId = doc.getString("leftUserId");

                        chatRoom.setClubId(clubId);
                        chatRoom.setClubName(clubName != null ? clubName : "");
                        chatRoom.setLastMessage(lastMessage != null ? lastMessage : "");
                        chatRoom.setLastMessageTime(lastMessageTime != null ? lastMessageTime : 0);
                        chatRoom.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);
                        chatRoom.setNotificationEnabled(notificationEnabled != null ? notificationEnabled : true);

                        // participants 목록 가져오기
                        java.util.List<String> participantsList = (java.util.List<String>) doc.get("participants");

                        // 단체 채팅방 여부 확인 (chatRoomId가 "group_"로 시작하면 단체 채팅방)
                        if (docId.startsWith("group_")) {
                            chatRoom.setGroupChat(true);
                            Long memberCount = doc.getLong("memberCount");
                            chatRoom.setMemberCount(memberCount != null ? memberCount.intValue() : 0);
                        } else {
                            // 개인 채팅방
                            chatRoom.setGroupChat(false);

                            // user1, user2 데이터에서 상대방 정보 추출
                            java.util.Map<String, Object> user1 = (java.util.Map<String, Object>) doc.get("user1");
                            java.util.Map<String, Object> user2 = (java.util.Map<String, Object>) doc.get("user2");

                            boolean partnerInfoSet = false;

                            if (user1 != null && user2 != null) {
                                String user1Id = (String) user1.get("userId");
                                String user2Id = (String) user2.get("userId");

                                // 상대방 정보 설정 (현재 사용자가 아닌 쪽)
                                if (user1Id != null && user1Id.equals(currentUserId)) {
                                    // 현재 사용자가 user1이면 user2가 상대방
                                    chatRoom.setPartnerUserId(user2Id);
                                    chatRoom.setPartnerName((String) user2.get("name"));
                                    chatRoom.setPartnerRole((String) user2.get("role"));
                                    partnerInfoSet = true;
                                } else if (user2Id != null && user2Id.equals(currentUserId)) {
                                    // 현재 사용자가 user2이면 user1이 상대방
                                    chatRoom.setPartnerUserId(user1Id);
                                    chatRoom.setPartnerName((String) user1.get("name"));
                                    chatRoom.setPartnerRole((String) user1.get("role"));
                                    partnerInfoSet = true;
                                }
                            }

                            // user1/user2가 없는 경우 participants에서 상대방 찾기
                            if (!partnerInfoSet) {
                                if (participantsList != null) {
                                    for (String pid : participantsList) {
                                        if (pid != null && !pid.equals(currentUserId)) {
                                            chatRoom.setPartnerUserId(pid);
                                            break;
                                        }
                                    }
                                }
                            }

                            // 상대방이 실제로 participants에 없는 경우에만 "상대방이 나갔습니다" 표시
                            String partnerId = chatRoom.getPartnerUserId();
                            if (partnerId != null && participantsList != null && !participantsList.contains(partnerId)) {
                                chatRoom.setLeftUserId(partnerId);
                            } else {
                                chatRoom.setLeftUserId(null);
                            }
                        }

                        chatRooms.add(chatRoom);
                    }

                    // 클라이언트에서 lastMessageTime 기준 내림차순 정렬
                    java.util.Collections.sort(chatRooms, (a, b) ->
                        Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));

                    callback.onSuccess(chatRooms);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 채팅방 삭제
     */
    public void deleteChatRoom(String chatRoomId, SimpleCallback callback) {
        db.collection("chatRooms")
                .document(chatRoomId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 채팅방 나가기 (상대방에게 나갔음을 표시)
     */
    public void leaveChatRoom(String chatRoomId, SimpleCallback callback) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            callback.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        java.util.Map<String, Object> updateData = new java.util.HashMap<>();
        updateData.put("leftUserId", currentUserId);
        updateData.put("lastMessage", "상대방이 나갔습니다");
        updateData.put("lastMessageTime", System.currentTimeMillis());

        db.collection("chatRooms")
                .document(chatRoomId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    // 나간 사용자의 채팅방 목록에서 제거 (participants에서 제거)
                    db.collection("chatRooms")
                            .document(chatRoomId)
                            .update("participants", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                            .addOnSuccessListener(aVoid2 -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 채팅방 알림 설정 토글
     */
    public void toggleChatRoomNotification(String chatRoomId, boolean enabled, SimpleCallback callback) {
        db.collection("chatRooms")
                .document(chatRoomId)
                .update("notificationEnabled", enabled)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Group Chat Methods
    // ========================================

    /**
     * 동아리 단체 채팅방 생성 또는 기존 채팅방 반환
     */
    public void createOrGetGroupChatRoom(String clubId, String clubName, ChatRoomCallback callback) {
        String groupChatRoomId = "group_" + clubId;

        db.collection("chatRooms")
                .document(groupChatRoomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 기존 단체 채팅방 반환
                        com.example.clubmanagement.models.ChatRoom chatRoom = documentSnapshot.toObject(com.example.clubmanagement.models.ChatRoom.class);
                        if (chatRoom != null) {
                            chatRoom.setChatRoomId(groupChatRoomId);
                            callback.onSuccess(chatRoom);
                        } else {
                            callback.onFailure(new Exception("채팅방 정보를 가져올 수 없습니다"));
                        }
                    } else {
                        // 새 단체 채팅방 생성
                        com.example.clubmanagement.models.ChatRoom newChatRoom = new com.example.clubmanagement.models.ChatRoom(
                                groupChatRoomId, clubId, clubName, 1);

                        java.util.Map<String, Object> chatRoomData = new java.util.HashMap<>();
                        chatRoomData.put("chatRoomId", groupChatRoomId);
                        chatRoomData.put("clubId", clubId);
                        chatRoomData.put("clubName", clubName);
                        chatRoomData.put("lastMessage", "");
                        chatRoomData.put("lastMessageTime", System.currentTimeMillis());
                        chatRoomData.put("unreadCount", 0);
                        chatRoomData.put("notificationEnabled", true);
                        chatRoomData.put("isGroupChat", true);
                        chatRoomData.put("memberCount", 1);
                        chatRoomData.put("participants", new java.util.ArrayList<String>());

                        db.collection("chatRooms")
                                .document(groupChatRoomId)
                                .set(chatRoomData)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(newChatRoom))
                                .addOnFailureListener(callback::onFailure);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 사용자를 단체 채팅방에 추가
     */
    public void joinGroupChatRoom(String clubId, SimpleCallback callback) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            callback.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        String groupChatRoomId = "group_" + clubId;

        db.collection("chatRooms")
                .document(groupChatRoomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 기존 채팅방에 사용자 추가
                        db.collection("chatRooms")
                                .document(groupChatRoomId)
                                .update(
                                        "participants", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId),
                                        "memberCount", com.google.firebase.firestore.FieldValue.increment(1)
                                )
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        // 채팅방이 없으면 먼저 동아리 정보 가져와서 생성
                        getClub(clubId, new ClubCallback() {
                            @Override
                            public void onSuccess(com.example.clubmanagement.models.Club club) {
                                String clubName = club != null ? club.getName() : "동아리";

                                java.util.Map<String, Object> chatRoomData = new java.util.HashMap<>();
                                chatRoomData.put("chatRoomId", groupChatRoomId);
                                chatRoomData.put("clubId", clubId);
                                chatRoomData.put("clubName", clubName);
                                chatRoomData.put("lastMessage", "");
                                chatRoomData.put("lastMessageTime", System.currentTimeMillis());
                                chatRoomData.put("unreadCount", 0);
                                chatRoomData.put("notificationEnabled", true);
                                chatRoomData.put("isGroupChat", true);
                                chatRoomData.put("memberCount", 1);
                                chatRoomData.put("participants", java.util.Arrays.asList(currentUserId));

                                db.collection("chatRooms")
                                        .document(groupChatRoomId)
                                        .set(chatRoomData)
                                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                                        .addOnFailureListener(callback::onFailure);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(e);
                            }
                        });
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 사용자를 단체 채팅방에서 제거
     */
    public void leaveGroupChatRoom(String clubId, SimpleCallback callback) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            callback.onFailure(new Exception("로그인이 필요합니다"));
            return;
        }

        String groupChatRoomId = "group_" + clubId;

        db.collection("chatRooms")
                .document(groupChatRoomId)
                .update(
                        "participants", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId),
                        "memberCount", com.google.firebase.firestore.FieldValue.increment(-1)
                )
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 현재 사용자가 가입한 모든 동아리의 단체 채팅방에 참여
     * (기존 회원들을 위한 자동 참여 로직)
     */
    public void ensureGroupChatMembership(SimpleCallback callback) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            callback.onSuccess();
            return;
        }

        getCurrentUser(new UserCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.User user) {
                if (user == null) {
                    callback.onSuccess();
                    return;
                }

                java.util.List<String> clubIds = new java.util.ArrayList<>();
                java.util.List<String> clubNames = new java.util.ArrayList<>();

                // 중앙동아리
                if (user.getCentralClubId() != null && !user.getCentralClubId().isEmpty()) {
                    clubIds.add(user.getCentralClubId());
                    clubNames.add(user.getCentralClubName() != null ? user.getCentralClubName() : "동아리");
                }

                // 일반동아리들
                if (user.getGeneralClubIds() != null) {
                    for (int i = 0; i < user.getGeneralClubIds().size(); i++) {
                        String clubId = user.getGeneralClubIds().get(i);
                        String clubName = (user.getGeneralClubNames() != null && i < user.getGeneralClubNames().size())
                                ? user.getGeneralClubNames().get(i) : "동아리";
                        clubIds.add(clubId);
                        clubNames.add(clubName);
                    }
                }

                if (clubIds.isEmpty()) {
                    callback.onSuccess();
                    return;
                }

                // 각 동아리의 단체 채팅방에 참여
                final int[] completed = {0};
                for (int i = 0; i < clubIds.size(); i++) {
                    String clubId = clubIds.get(i);
                    String clubName = clubNames.get(i);

                    joinGroupChatRoom(clubId, new SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            completed[0]++;
                            if (completed[0] >= clubIds.size()) {
                                callback.onSuccess();
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            completed[0]++;
                            if (completed[0] >= clubIds.size()) {
                                callback.onSuccess();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onSuccess();
            }
        });
    }

    /**
     * 특정 사용자를 단체 채팅방에 추가 (가입 승인 시 사용)
     */
    public void addUserToGroupChatRoom(String clubId, String clubName, String userId, SimpleCallback callback) {
        String groupChatRoomId = "group_" + clubId;

        db.collection("chatRooms")
                .document(groupChatRoomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 기존 채팅방에 사용자 추가
                        db.collection("chatRooms")
                                .document(groupChatRoomId)
                                .update(
                                        "participants", com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                                        "memberCount", com.google.firebase.firestore.FieldValue.increment(1)
                                )
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        // 채팅방이 없으면 생성
                        java.util.Map<String, Object> chatRoomData = new java.util.HashMap<>();
                        chatRoomData.put("chatRoomId", groupChatRoomId);
                        chatRoomData.put("clubId", clubId);
                        chatRoomData.put("clubName", clubName);
                        chatRoomData.put("lastMessage", "");
                        chatRoomData.put("lastMessageTime", System.currentTimeMillis());
                        chatRoomData.put("unreadCount", 0);
                        chatRoomData.put("notificationEnabled", true);
                        chatRoomData.put("isGroupChat", true);
                        chatRoomData.put("memberCount", 1);
                        chatRoomData.put("participants", java.util.Arrays.asList(userId));

                        db.collection("chatRooms")
                                .document(groupChatRoomId)
                                .set(chatRoomData)
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(callback::onFailure);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ========================================
    // Member Role Methods
    // ========================================

    /**
     * 부원의 직급 설정
     * @param clubId 동아리 ID
     * @param userId 부원 사용자 ID
     * @param role 직급 ("부회장", "총무", "회계", "회원")
     * @param callback 콜백
     */
    public void setMemberRole(String clubId, String userId, String role, SimpleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .update("role", role)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 부원의 직급 가져오기
     * @param clubId 동아리 ID
     * @param userId 부원 사용자 ID
     * @param callback 콜백
     */
    public void getMemberRole(String clubId, String userId, RoleCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        callback.onSuccess(role != null ? role : "회원");
                    } else {
                        callback.onSuccess("회원");
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public interface RoleCallback {
        void onSuccess(String role);
        void onFailure(Exception e);
    }
}
