package com.example.clubmanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clubmanagement.utils.ChatNotificationManager;
import com.example.clubmanagement.utils.ThemeHelper;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Base activity class that applies theme to all activities
 * Extend this class instead of AppCompatActivity to have automatic theme support
 */
public abstract class BaseActivity extends AppCompatActivity {

    private ChatNotificationManager chatNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatNotificationManager = ChatNotificationManager.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Apply theme when activity resumes (in case theme was changed in settings)
        ThemeHelper.applyTheme(this);

        // 채팅 뱃지 업데이트
        setupChatBadgeListener();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        // Apply theme after content view is set
        ThemeHelper.applyTheme(this);
    }

    /**
     * 채팅 뱃지 리스너 설정
     */
    protected void setupChatBadgeListener() {
        chatNotificationManager.setOnUnreadCountChangeListener(count -> {
            runOnUiThread(() -> updateChatBadge(count));
        });
    }

    /**
     * 네비게이션 바의 채팅 뱃지 업데이트
     */
    protected void updateChatBadge(int unreadCount) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav == null) return;

        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_chat);

        if (unreadCount > 0) {
            badge.setVisible(true);
            badge.setNumber(unreadCount);
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }
    }

    /**
     * 채팅 알림 매니저 반환
     */
    protected ChatNotificationManager getChatNotificationManager() {
        return chatNotificationManager;
    }
}
