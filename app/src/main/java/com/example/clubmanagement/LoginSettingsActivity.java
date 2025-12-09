package com.example.clubmanagement;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import com.example.clubmanagement.BaseActivity;
import com.google.android.material.button.MaterialButton;

public class LoginSettingsActivity extends BaseActivity {

    private MaterialButton btnAdminLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_settings);

        initViews();
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        btnAdminLogin = findViewById(R.id.btnAdminLogin);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnAdminLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminLoginActivity.class);
            startActivity(intent);
        });
    }
}
