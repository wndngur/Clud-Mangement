package com.example.clubmanagement.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Helper class for managing app themes and color filters
 */
public class ThemeHelper {

    private static final String PREFS_NAME = "ThemePrefs";
    private static final String KEY_THEME = "selected_theme";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";

    // Theme constants
    public static final int THEME_ORIGINAL = 0;
    public static final int THEME_BLACK_WHITE = 1;
    public static final int THEME_GRAYSCALE = 2;

    /**
     * Save selected theme (로컬 + Firebase)
     */
    public static void setTheme(Context context, int theme) {
        // 로컬에 저장
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, theme).apply();

        // 로그인된 사용자가 있으면 Firebase에도 저장
        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        String userId = firebaseManager.getCurrentUserId();
        if (userId != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("themePreference", theme)
                    .addOnFailureListener(e -> {
                        android.util.Log.e("ThemeHelper", "Failed to save theme to Firebase: " + e.getMessage());
                    });
        }
    }

    /**
     * Get current theme (로컬에서 가져옴)
     */
    public static int getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_ORIGINAL);
    }

    /**
     * Firebase에서 테마 설정을 로드하여 로컬에 동기화 (로그인 시 호출)
     */
    public static void syncThemeFromFirebase(Context context, ThemeSyncCallback callback) {
        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        String userId = firebaseManager.getCurrentUserId();

        if (userId == null) {
            if (callback != null) callback.onComplete();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long themePref = documentSnapshot.getLong("themePreference");
                        if (themePref != null) {
                            // Firebase에서 가져온 테마를 로컬에 저장
                            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                            prefs.edit().putInt(KEY_THEME, themePref.intValue()).apply();
                        }
                    }
                    if (callback != null) callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ThemeHelper", "Failed to sync theme from Firebase: " + e.getMessage());
                    if (callback != null) callback.onComplete();
                });
    }

    /**
     * 로그아웃 시 로컬 테마 설정 초기화
     */
    public static void clearLocalTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, THEME_ORIGINAL).apply();
    }

    public interface ThemeSyncCallback {
        void onComplete();
    }

    /**
     * Save notification setting
     */
    public static void setNotificationsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    /**
     * Check if notifications are enabled
     */
    public static boolean isNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATIONS, true);
    }

    /**
     * Apply theme to activity
     */
    public static void applyTheme(Activity activity) {
        int theme = getTheme(activity);
        View decorView = activity.getWindow().getDecorView();

        switch (theme) {
            case THEME_BLACK_WHITE:
                applyBlackWhiteTheme(decorView);
                break;
            case THEME_GRAYSCALE:
                applyGrayscaleTheme(decorView);
                break;
            case THEME_ORIGINAL:
            default:
                removeColorFilter(decorView);
                break;
        }
    }

    /**
     * Apply black and white theme (UI only, images stay colored)
     * This applies a saturation filter to the root view layer
     */
    private static void applyBlackWhiteTheme(View view) {
        // Create a grayscale color matrix
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);

        // Apply the filter to the view's layer
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));

        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * Apply grayscale theme (everything including images)
     */
    private static void applyGrayscaleTheme(View view) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));

        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * Remove color filter (restore original colors)
     */
    private static void removeColorFilter(View view) {
        view.setLayerType(View.LAYER_TYPE_NONE, null);
    }

    /**
     * Check if current theme is grayscale (for image processing)
     */
    public static boolean isGrayscaleTheme(Context context) {
        return getTheme(context) == THEME_GRAYSCALE;
    }

    /**
     * Check if current theme applies any color filter
     */
    public static boolean hasColorFilter(Context context) {
        int theme = getTheme(context);
        return theme == THEME_BLACK_WHITE || theme == THEME_GRAYSCALE;
    }

    /**
     * Get grayscale color filter for ImageViews (for grayscale theme only)
     */
    public static ColorMatrixColorFilter getGrayscaleFilter() {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        return new ColorMatrixColorFilter(colorMatrix);
    }

    /**
     * Get theme name for display
     */
    public static String getThemeName(Context context) {
        int theme = getTheme(context);
        switch (theme) {
            case THEME_BLACK_WHITE:
                return "블랙 앤 화이트";
            case THEME_GRAYSCALE:
                return "흑백";
            case THEME_ORIGINAL:
            default:
                return "오리지널";
        }
    }
}
