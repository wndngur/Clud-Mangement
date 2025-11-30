package com.example.clubmanagement;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.clubmanagement.utils.ThemeHelper;

/**
 * Base activity class that applies theme to all activities
 * Extend this class instead of AppCompatActivity to have automatic theme support
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Apply theme when activity resumes (in case theme was changed in settings)
        ThemeHelper.applyTheme(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        // Apply theme after content view is set
        ThemeHelper.applyTheme(this);
    }
}
