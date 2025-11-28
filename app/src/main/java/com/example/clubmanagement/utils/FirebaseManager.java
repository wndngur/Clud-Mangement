package com.example.clubmanagement.utils;

import android.net.Uri;
import android.util.Log;

import com.example.clubmanagement.models.SignatureData;
import com.example.clubmanagement.models.DocumentData;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
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
     * Get all carousel items ordered by position
     */
    public void getCarouselItems(CarouselListCallback callback) {
        db.collection("carousel_items")
                .orderBy("position")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    java.util.List<com.example.clubmanagement.models.CarouselItem> items = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        com.example.clubmanagement.models.CarouselItem item = doc.toObject(com.example.clubmanagement.models.CarouselItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            items.add(item);
                        }
                    }
                    callback.onSuccess(items);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Get single carousel item by position
     */
    public void getCarouselItemByPosition(int position, CarouselCallback callback) {
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

    /**
     * Get banner
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
     * Save banner
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

    /**
     * Get club information by ID
     */
    public void getClub(String clubId, ClubCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.clubmanagement.models.Club club = documentSnapshot.toObject(com.example.clubmanagement.models.Club.class);
                        callback.onSuccess(club);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
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
}
