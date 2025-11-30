package com.example.clubmanagement.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.view.View;

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
     * Save selected theme
     */
    public static void setTheme(Context context, int theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, theme).apply();
    }

    /**
     * Get current theme
     */
    public static int getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_ORIGINAL);
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
