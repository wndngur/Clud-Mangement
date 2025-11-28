package com.example.clubmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clubmanagement.activities.DocumentGenerateActivity;
import com.example.clubmanagement.activities.SignaturePadActivity;
import com.example.clubmanagement.activities.SignatureUploadActivity;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseManager = FirebaseManager.getInstance();

        // Firebase 익명 인증 (테스트용)
        authenticateAnonymously();

        setupClickListeners();
    }

    private void setupClickListeners() {
        MaterialCardView cardSignaturePad = findViewById(R.id.cardSignaturePad);
        MaterialCardView cardSignatureUpload = findViewById(R.id.cardSignatureUpload);
        MaterialCardView cardDocumentGenerate = findViewById(R.id.cardDocumentGenerate);
        MaterialCardView cardSignatureManage = findViewById(R.id.cardSignatureManage);

        cardSignaturePad.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignaturePadActivity.class);
            startActivity(intent);
        });

        cardSignatureUpload.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignatureUploadActivity.class);
            startActivity(intent);
        });

        cardDocumentGenerate.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DocumentGenerateActivity.class);
            startActivity(intent);
        });

        cardSignatureManage.setOnClickListener(v -> {
            Toast.makeText(this, "관리자 기능 - 개발 중", Toast.LENGTH_SHORT).show();
            // TODO: SignatureManageActivity 구현
        });
    }

    /**
     * Firebase 익명 인증 (개발/테스트용)
     * 실제 앱에서는 이메일/비밀번호 또는 소셜 로그인 사용
     */
    private void authenticateAnonymously() {
        FirebaseAuth auth = firebaseManager.getAuth();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            Toast.makeText(this, "로그인됨: " + currentUser.getUid(), Toast.LENGTH_SHORT).show();
        } else {
            auth.signInAnonymously()
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        if (user != null) {
                            Toast.makeText(MainActivity.this,
                                    "익명 로그인 성공: " + user.getUid(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this,
                                "로그인 실패: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        }
    }
}
