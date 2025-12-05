package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.models.QnAItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

public class AskQuestionActivity extends BaseActivity {

    private ImageView ivBack;
    private TextView tvClubName;
    private EditText etQuestion;
    private CheckBox cbPrivate;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private String clubId;
    private String clubName;
    private String userId;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_question);

        // Get club info from intent
        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");

        if (clubId == null || clubId.isEmpty()) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();

        // Get user info
        userId = firebaseManager.getCurrentUserId();
        android.util.Log.d("AskQuestionActivity", "userId: " + userId);

        initViews();
        if (userId != null) {
            loadUserInfo();
        } else {
            userName = "익명";
        }
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvClubName = findViewById(R.id.tvClubName);
        etQuestion = findViewById(R.id.etQuestion);
        cbPrivate = findViewById(R.id.cbPrivate);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        tvClubName.setText(clubName);
    }

    private void loadUserInfo() {
        firebaseManager.getCurrentUser(new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.User user) {
                if (user != null) {
                    userName = user.getName();
                    if (userName == null || userName.isEmpty()) {
                        userName = user.getEmail();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                userName = "익명";
            }
        });
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            String question = etQuestion.getText().toString().trim();

            if (question.isEmpty()) {
                Toast.makeText(this, "질문을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            submitQuestion(question, cbPrivate.isChecked());
        });
    }

    private void submitQuestion(String question, boolean isPrivate) {
        // Check if user is logged in
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        // Generate question ID
        String questionId = firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document().getId();

        // Create QnAItem
        QnAItem qnaItem = new QnAItem(questionId, clubId, question, userName, userId);
        qnaItem.setPrivate(isPrivate);
        qnaItem.setCreatedAt(Timestamp.now());

        // Save to Firestore
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document(questionId)
                .set(qnaItem)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "질문이 등록되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "질문 등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
