package com.example.clubmanagement.utils;

import android.util.Log;

import com.example.clubmanagement.models.AdminPassword;
import com.example.clubmanagement.models.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * 인증 및 사용자 권한 관련 매니저 클래스
 * FirebaseAuth와 사용자 권한 관리를 담당합니다.
 */
public class AuthManager {
    private static final String TAG = "AuthManager";

    private static AuthManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    private AuthManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    // ======================== 콜백 인터페이스 ========================

    public interface UserDataCallback {
        void onSuccess(UserData userData);
        void onFailure(Exception e);
    }

    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
        void onFailure(Exception e);
    }

    public interface AdminPasswordCallback {
        void onSuccess(AdminPassword adminPassword);
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

    // ======================== 현재 사용자 정보 ========================

    /**
     * FirebaseAuth 인스턴스 반환
     */
    public FirebaseAuth getAuth() {
        return auth;
    }

    /**
     * 현재 로그인된 사용자 반환
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * 현재 로그인된 사용자 ID 반환
     */
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * 사용자가 로그인되어 있는지 확인
     */
    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    // ======================== 사용자 데이터 ========================

    /**
     * 사용자 데이터 가져오기
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
                        UserData userData = documentSnapshot.toObject(UserData.class);
                        callback.onSuccess(userData);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 현재 사용자 데이터 가져오기
     */
    public void getCurrentUserData(UserDataCallback callback) {
        getUserData(getCurrentUserId(), callback);
    }

    /**
     * 사용자 관리자 레벨 설정
     */
    public void setUserAdminLevel(String userId, String adminLevel, String clubId, SimpleCallback callback) {
        UserData userData = new UserData(userId, adminLevel, clubId);

        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 사용자 데이터 업데이트
     */
    public void updateUserData(String userId, UserData userData, SimpleCallback callback) {
        if (userId == null || userData == null) {
            callback.onFailure(new IllegalArgumentException("userId or userData is null"));
            return;
        }

        db.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 관리자 권한 확인 ========================

    /**
     * 현재 사용자가 관리자인지 확인 (DEPRECATED - use getCurrentUserData instead)
     */
    @Deprecated
    public void isCurrentUserAdmin(AdminCheckCallback callback) {
        getCurrentUserData(new UserDataCallback() {
            @Override
            public void onSuccess(UserData userData) {
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
     * 현재 사용자가 슈퍼 관리자인지 확인
     */
    public void isCurrentUserSuperAdmin(AdminCheckCallback callback) {
        getCurrentUserData(new UserDataCallback() {
            @Override
            public void onSuccess(UserData userData) {
                if (userData != null) {
                    callback.onResult(userData.isSuperAdmin());
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
     * 현재 사용자가 특정 동아리의 관리자인지 확인
     */
    public void isCurrentUserClubAdmin(String clubId, AdminCheckCallback callback) {
        getCurrentUserData(new UserDataCallback() {
            @Override
            public void onSuccess(UserData userData) {
                if (userData != null) {
                    boolean isAdmin = userData.isSuperAdmin() ||
                                     (userData.isClubAdmin() && clubId.equals(userData.getClubId()));
                    callback.onResult(isAdmin);
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

    // ======================== 관리자 비밀번호 ========================

    /**
     * 관리자 비밀번호 가져오기
     */
    public void getAdminPassword(String level, String clubId, AdminPasswordCallback callback) {
        String docId = level.equals("SUPER_ADMIN") ? "super_admin" : "club_admin_" + clubId;

        db.collection("admin_passwords")
                .document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        AdminPassword password = documentSnapshot.toObject(AdminPassword.class);
                        callback.onSuccess(password);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 관리자 비밀번호 검증
     */
    public void verifyAdminPassword(String level, String clubId, String inputPassword, PasswordVerifyCallback callback) {
        getAdminPassword(level, clubId, new AdminPasswordCallback() {
            @Override
            public void onSuccess(AdminPassword adminPassword) {
                if (adminPassword == null) {
                    // 비밀번호가 설정되지 않은 경우 기본 비밀번호 사용
                    String defaultPassword = level.equals("SUPER_ADMIN") ? "superadmin123" : "clubadmin123";
                    boolean isValid = defaultPassword.equals(inputPassword);
                    callback.onSuccess(isValid, level, clubId);
                } else {
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
     * 관리자 비밀번호 설정
     */
    public void setAdminPassword(String level, String clubId, String password, SimpleCallback callback) {
        String docId = level.equals("SUPER_ADMIN") ? "super_admin" : "club_admin_" + clubId;
        AdminPassword adminPassword = new AdminPassword(docId, level, password, clubId);

        db.collection("admin_passwords")
                .document(docId)
                .set(adminPassword)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 관리자 역할 설정 (DEPRECATED)
     */
    @Deprecated
    public void setAdminRole(String userId, boolean isAdmin, SimpleCallback callback) {
        String adminLevel = isAdmin ? "SUPER_ADMIN" : "NONE";
        setUserAdminLevel(userId, adminLevel, null, callback);
    }

    // ======================== 로그아웃 ========================

    /**
     * 로그아웃
     */
    public void signOut() {
        auth.signOut();
    }

    /**
     * 로그아웃 (콜백 포함)
     */
    public void signOut(SimpleCallback callback) {
        try {
            auth.signOut();
            callback.onSuccess();
        } catch (Exception e) {
            callback.onFailure(e);
        }
    }
}
