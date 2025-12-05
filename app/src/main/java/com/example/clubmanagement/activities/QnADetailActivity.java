package com.example.clubmanagement.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.example.clubmanagement.adapters.QnACommentAdapter;
import com.example.clubmanagement.models.QnAComment;
import com.example.clubmanagement.models.QnAItem;
import com.example.clubmanagement.utils.DateHelper;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class QnADetailActivity extends BaseActivity {

    private ImageView ivBack;
    private TextView tvType;
    private TextView tvPrivateBadge;
    private TextView tvAskerName;
    private TextView tvDate;
    private TextView tvQuestion;
    private TextView tvCommentCount;
    private RecyclerView rvComments;
    private LinearLayout llEmptyComments;
    private LinearLayout llCommentInput;
    private EditText etComment;
    private MaterialButton btnSendComment;
    private ProgressBar progressBar;

    private QnACommentAdapter commentAdapter;
    private FirebaseManager firebaseManager;

    private String clubId;
    private String qnaId;
    private QnAItem currentQnA;
    private String userId;
    private String userName;
    private boolean isMember = false;
    private boolean isAdmin = false;
    private boolean isSuperAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qna_detail);

        // Get data from intent
        clubId = getIntent().getStringExtra("club_id");
        qnaId = getIntent().getStringExtra("qna_id");

        if (clubId == null || qnaId == null) {
            Toast.makeText(this, "잘못된 접근입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
        userId = firebaseManager.getCurrentUserId();

        initViews();
        setupRecyclerView();
        checkMembershipAndLoadData();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvType = findViewById(R.id.tvType);
        tvPrivateBadge = findViewById(R.id.tvPrivateBadge);
        tvAskerName = findViewById(R.id.tvAskerName);
        tvDate = findViewById(R.id.tvDate);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvCommentCount = findViewById(R.id.tvCommentCount);
        rvComments = findViewById(R.id.rvComments);
        llEmptyComments = findViewById(R.id.llEmptyComments);
        llCommentInput = findViewById(R.id.llCommentInput);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        commentAdapter = new QnACommentAdapter();
        commentAdapter.setEditClickListener(this::showEditCommentDialog);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
    }

    private void updateAdapterUserInfo() {
        // 어댑터에 사용자 정보 전달 (수정 버튼 표시 여부 결정)
        commentAdapter.setUserInfo(userId, isAdmin, isSuperAdmin);
    }

    private void checkMembershipAndLoadData() {
        // 최고 관리자 모드 체크
        isSuperAdmin = SettingsActivity.isSuperAdminMode(this);

        if (userId == null) {
            // Not logged in - load Q&A and check if private
            loadQnAItem();
            return;
        }

        // Check if user is member or admin
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isMember = documentSnapshot.exists();

                    // Check if club admin
                    firebaseManager.isCurrentUserAdmin(new FirebaseManager.AdminCheckCallback() {
                        @Override
                        public void onResult(boolean admin) {
                            isAdmin = admin;
                            updateAdapterUserInfo();
                            loadQnAItem();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            isAdmin = false;
                            updateAdapterUserInfo();
                            loadQnAItem();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    isMember = false;
                    isAdmin = false;
                    updateAdapterUserInfo();
                    loadQnAItem();
                });

        // Load user name
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

    private void loadQnAItem() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document(qnaId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentQnA = documentSnapshot.toObject(QnAItem.class);
                        if (currentQnA != null) {
                            currentQnA.setId(documentSnapshot.getId());

                            // Check if private and access control
                            // 비밀 질문은 작성자, 동아리 회원, 동아리 관리자, 최고 관리자만 볼 수 있음
                            if (currentQnA.isPrivate()) {
                                boolean isAuthor = userId != null && userId.equals(currentQnA.getAskerId());
                                boolean canAccess = isAuthor || isMember || isAdmin || isSuperAdmin;

                                if (!canAccess) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(this, "비밀 질문입니다. 작성자, 동아리 회원, 관리자만 볼 수 있습니다.", Toast.LENGTH_SHORT).show();
                                    finish();
                                    return;
                                }
                            }

                            displayQnA(currentQnA);
                            loadComments();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "질문을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "질문 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayQnA(QnAItem qna) {
        // Type badge
        if (qna.isQnA()) {
            tvType.setText("Q&A");
        } else {
            tvType.setText("FAQ");
        }

        // Private badge
        if (qna.isPrivate()) {
            tvPrivateBadge.setVisibility(View.VISIBLE);
        } else {
            tvPrivateBadge.setVisibility(View.GONE);
        }

        // Asker name
        if (qna.getAskerName() != null && !qna.getAskerName().isEmpty()) {
            tvAskerName.setText(qna.getAskerName());
        } else {
            tvAskerName.setText("익명");
        }

        // Date
        if (qna.getCreatedAt() != null) {
            String dateStr = DateHelper.formatDate(qna.getCreatedAt().toDate());
            tvDate.setText(dateStr);
        }

        // Question
        tvQuestion.setText(qna.getQuestion());

        // Comment input visibility (members, club admin, and super admin only)
        if (isMember || isAdmin || isSuperAdmin) {
            llCommentInput.setVisibility(View.VISIBLE);
        } else {
            llCommentInput.setVisibility(View.GONE);
        }
    }

    private void loadComments() {
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document(qnaId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(this, "댓글 로드 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<QnAComment> comments = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            QnAComment comment = document.toObject(QnAComment.class);
                            comment.setId(document.getId());
                            comments.add(comment);
                        }

                        tvCommentCount.setText(String.valueOf(comments.size()));

                        if (comments.isEmpty()) {
                            llEmptyComments.setVisibility(View.VISIBLE);
                            rvComments.setVisibility(View.GONE);
                            // 답변이 없으면 입력란 표시 (권한 있는 경우)
                            if (isMember || isAdmin || isSuperAdmin) {
                                llCommentInput.setVisibility(View.VISIBLE);
                            }
                        } else {
                            llEmptyComments.setVisibility(View.GONE);
                            rvComments.setVisibility(View.VISIBLE);
                            commentAdapter.setComments(comments);
                            // 답변이 있으면 입력란 숨기기 (더 이상 답변 불가)
                            llCommentInput.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnSendComment.setOnClickListener(v -> {
            String content = etComment.getText().toString().trim();

            if (content.isEmpty()) {
                Toast.makeText(this, "답변을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            postComment(content);
        });
    }

    private void postComment(String content) {
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isMember && !isAdmin && !isSuperAdmin) {
            Toast.makeText(this, "동아리 회원, 관리자만 답변할 수 있습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendComment.setEnabled(false);

        // Generate comment ID
        String commentId = firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document(qnaId)
                .collection("comments")
                .document().getId();

        // Create comment (isAdmin 또는 isSuperAdmin이면 관리자 표시)
        QnAComment comment = new QnAComment(commentId, qnaId, clubId, content,
                                            userId, userName, isAdmin || isSuperAdmin);
        comment.setCreatedAt(Timestamp.now());

        // Save to Firestore
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document(qnaId)
                .collection("comments")
                .document(commentId)
                .set(comment)
                .addOnSuccessListener(aVoid -> {
                    btnSendComment.setEnabled(true);
                    etComment.setText("");
                    Toast.makeText(this, "답변이 등록되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSendComment.setEnabled(true);
                    Toast.makeText(this, "답변 등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditCommentDialog(QnAComment comment) {
        // 수정 권한 체크
        boolean canEdit = false;
        if (isAdmin || isSuperAdmin) {
            canEdit = true;
        } else if (userId != null && userId.equals(comment.getAuthorId())) {
            canEdit = true;
        }

        if (!canEdit) {
            Toast.makeText(this, "수정 권한이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // EditText 생성
        EditText editText = new EditText(this);
        editText.setText(comment.getContent());
        editText.setMinLines(3);
        editText.setPadding(48, 32, 48, 32);

        // 다이얼로그 표시
        new AlertDialog.Builder(this)
                .setTitle("답변 수정")
                .setView(editText)
                .setPositiveButton("수정", (dialog, which) -> {
                    String newContent = editText.getText().toString().trim();
                    if (newContent.isEmpty()) {
                        Toast.makeText(this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateComment(comment, newContent);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void updateComment(QnAComment comment, String newContent) {
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document(qnaId)
                .collection("comments")
                .document(comment.getId())
                .update("content", newContent)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "답변이 수정되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
