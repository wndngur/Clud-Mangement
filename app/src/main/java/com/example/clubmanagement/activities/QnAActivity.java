package com.example.clubmanagement.activities;

import android.app.AlertDialog;
import android.content.Intent;
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
import com.example.clubmanagement.adapters.QnAAdapter;
import com.example.clubmanagement.models.QnAItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QnAActivity extends BaseActivity {

    private ImageView ivBack;
    private MaterialButton btnQnA;
    private MaterialButton btnFAQ;
    private RecyclerView rvQnAList;
    private LinearLayout llEmptyState;
    private TextView tvEmptyMessage;
    private ProgressBar progressBar;
    private MaterialButton btnAskQuestion;
    private FloatingActionButton fabAdd;

    private QnAAdapter adapter;
    private FirebaseManager firebaseManager;

    private String clubId;
    private String clubName;
    private String currentTab = "qna";  // "qna" or "faq"
    private boolean isAdmin = false;
    private boolean isMember = false;
    private boolean isSuperAdmin = false;
    private boolean isAdminModeFromIntent = false;  // 관리자 모드로 열렸는지
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qna);

        // Get club info from intent
        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");
        isAdminModeFromIntent = getIntent().getBooleanExtra("admin_mode", false);

        // Debug log
        android.util.Log.d("QnAActivity", "onCreate - clubId: " + clubId + ", clubName: " + clubName + ", adminMode: " + isAdminModeFromIntent);

        if (clubId == null || clubId.isEmpty()) {
            android.util.Log.e("QnAActivity", "clubId is null or empty, finishing activity");
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
        currentUserId = firebaseManager.getCurrentUserId();
        isSuperAdmin = SettingsActivity.isSuperAdminMode(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        checkAdminStatus();
        loadQnAData();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        btnQnA = findViewById(R.id.btnQnA);
        btnFAQ = findViewById(R.id.btnFAQ);
        rvQnAList = findViewById(R.id.rvQnAList);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        progressBar = findViewById(R.id.progressBar);
        btnAskQuestion = findViewById(R.id.btnAskQuestion);
        fabAdd = findViewById(R.id.fabAdd);
    }

    private void setupRecyclerView() {
        adapter = new QnAAdapter(item -> {
            // Check if private and user can access
            // 비밀 질문은 작성자, 동아리 회원, 동아리 관리자, 최고 관리자만 볼 수 있음
            if (item.isPrivate()) {
                boolean isAuthor = currentUserId != null && currentUserId.equals(item.getAskerId());
                boolean canAccess = isAuthor || isMember || isAdmin || isSuperAdmin;

                if (!canAccess) {
                    Toast.makeText(this, "비밀 질문입니다. 작성자, 동아리 회원, 관리자만 볼 수 있습니다.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Open detail activity
            Intent intent = new Intent(QnAActivity.this, QnADetailActivity.class);
            intent.putExtra("club_id", clubId);
            intent.putExtra("qna_id", item.getId());
            startActivity(intent);
        });

        // 관리자 모드로 열렸으면 설정 버튼 표시
        if (isAdminModeFromIntent) {
            adapter.setAdminMode(true);
            adapter.setSettingsClickListener(this::showQnASettingsDialog);
        }

        rvQnAList.setLayoutManager(new LinearLayoutManager(this));
        rvQnAList.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnQnA.setOnClickListener(v -> {
            if (!currentTab.equals("qna")) {
                currentTab = "qna";
                updateTabUI();
                loadQnAData();
            }
        });

        btnFAQ.setOnClickListener(v -> {
            if (!currentTab.equals("faq")) {
                currentTab = "faq";
                updateTabUI();
                loadFAQData();
            }
        });

        fabAdd.setOnClickListener(v -> {
            // TODO: Open add dialog
            Toast.makeText(this, "Q&A/FAQ 추가 기능은 곧 제공됩니다", Toast.LENGTH_SHORT).show();
        });

        btnAskQuestion.setOnClickListener(v -> {
            // Check if user is logged in
            String userId = firebaseManager.getCurrentUserId();
            if (userId == null) {
                Toast.makeText(this, "질문을 작성하려면 로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                Intent loginIntent = new Intent(QnAActivity.this, com.example.clubmanagement.LoginActivity.class);
                startActivity(loginIntent);
                return;
            }

            // Validate data before opening AskQuestionActivity
            if (clubId == null || clubId.isEmpty()) {
                Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // Open ask question activity
            Intent intent = new Intent(QnAActivity.this, AskQuestionActivity.class);
            intent.putExtra("club_id", clubId);
            intent.putExtra("club_name", clubName != null ? clubName : "동아리");
            startActivity(intent);
        });
    }

    private void updateTabUI() {
        if (currentTab.equals("qna")) {
            // Q&A tab selected
            btnQnA.setBackgroundColor(getColor(R.color.purple_500));
            btnQnA.setTextColor(getColor(android.R.color.white));
            btnFAQ.setBackgroundColor(getColor(android.R.color.transparent));
            btnFAQ.setTextColor(getColor(R.color.purple_500));

            tvEmptyMessage.setText("아직 등록된 Q&A가 없습니다");
        } else {
            // FAQ tab selected
            btnFAQ.setBackgroundColor(getColor(R.color.purple_500));
            btnFAQ.setTextColor(getColor(android.R.color.white));
            btnQnA.setBackgroundColor(getColor(android.R.color.transparent));
            btnQnA.setTextColor(getColor(R.color.purple_500));

            tvEmptyMessage.setText("아직 등록된 FAQ가 없습니다");
        }
    }

    private void checkAdminStatus() {
        firebaseManager.isCurrentUserAdmin(new FirebaseManager.AdminCheckCallback() {
            @Override
            public void onResult(boolean admin) {
                isAdmin = admin;
                fabAdd.setVisibility(admin ? View.VISIBLE : View.GONE);
                checkMembership();
            }

            @Override
            public void onFailure(Exception e) {
                isAdmin = false;
                fabAdd.setVisibility(View.GONE);
                checkMembership();
            }
        });
    }

    private void checkMembership() {
        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) {
            // Not logged in - still show ask question button (will redirect to login when clicked)
            isMember = false;
            btnAskQuestion.setVisibility(View.VISIBLE);
            return;
        }

        // Check if user is member
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isMember = documentSnapshot.exists();
                    // 모든 로그인 사용자에게 질문하기 버튼 표시
                    btnAskQuestion.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    isMember = false;
                    btnAskQuestion.setVisibility(View.VISIBLE);
                });
    }

    private void loadQnAData() {
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
        rvQnAList.setVisibility(View.GONE);

        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    List<QnAItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        QnAItem item = document.toObject(QnAItem.class);
                        item.setId(document.getId());

                        // 클라이언트에서 type 필터링
                        if ("qna".equals(item.getType())) {
                            items.add(item);
                        }
                    }

                    if (items.isEmpty()) {
                        llEmptyState.setVisibility(View.VISIBLE);
                        rvQnAList.setVisibility(View.GONE);
                    } else {
                        llEmptyState.setVisibility(View.GONE);
                        rvQnAList.setVisibility(View.VISIBLE);
                        adapter.setItems(items);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Q&A 목록 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    llEmptyState.setVisibility(View.VISIBLE);
                    rvQnAList.setVisibility(View.GONE);
                });
    }

    private void loadFAQData() {
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
        rvQnAList.setVisibility(View.GONE);

        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .orderBy("position", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    List<QnAItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        QnAItem item = document.toObject(QnAItem.class);
                        item.setId(document.getId());

                        // 클라이언트에서 type 필터링
                        if ("faq".equals(item.getType())) {
                            items.add(item);
                        }
                    }

                    if (items.isEmpty()) {
                        llEmptyState.setVisibility(View.VISIBLE);
                        rvQnAList.setVisibility(View.GONE);
                    } else {
                        llEmptyState.setVisibility(View.GONE);
                        rvQnAList.setVisibility(View.VISIBLE);
                        adapter.setItems(items);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "FAQ 목록 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    llEmptyState.setVisibility(View.VISIBLE);
                    rvQnAList.setVisibility(View.GONE);
                });
    }

    // ========== Q&A 관리 기능 ==========

    private void showQnASettingsDialog(QnAItem item) {
        String[] options;
        if (item.isQnA()) {
            // 비밀 질문은 FAQ로 전환 불가
            if (item.isPrivate()) {
                options = new String[]{"수정", "삭제"};
            } else {
                options = new String[]{"수정", "삭제", "FAQ로 이동"};
            }
        } else {
            options = new String[]{"수정", "삭제", "Q&A로 이동"};
        }

        new AlertDialog.Builder(this)
                .setTitle("질문 관리")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:  // 수정
                            showEditQnADialog(item);
                            break;
                        case 1:  // 삭제
                            showDeleteConfirmDialog(item);
                            break;
                        case 2:  // FAQ/Q&A 전환 (비밀 질문인 경우 이 옵션 없음)
                            if (item.isQnA()) {
                                convertToFAQ(item);
                            } else {
                                convertToQnA(item);
                            }
                            break;
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showEditQnADialog(QnAItem item) {
        EditText editText = new EditText(this);
        editText.setText(item.getQuestion());
        editText.setMinLines(3);
        editText.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("질문 수정")
                .setView(editText)
                .setPositiveButton("수정", (dialog, which) -> {
                    String newQuestion = editText.getText().toString().trim();
                    if (newQuestion.isEmpty()) {
                        Toast.makeText(this, "질문을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateQnAQuestion(item, newQuestion);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void updateQnAQuestion(QnAItem item, String newQuestion) {
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document(item.getId())
                .update("question", newQuestion)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "질문이 수정되었습니다", Toast.LENGTH_SHORT).show();
                    // Reload data
                    if (currentTab.equals("qna")) {
                        loadQnAData();
                    } else {
                        loadFAQData();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmDialog(QnAItem item) {
        new AlertDialog.Builder(this)
                .setTitle("질문 삭제")
                .setMessage("정말로 이 질문을 삭제하시겠습니까?\n삭제된 질문은 복구할 수 없습니다.")
                .setPositiveButton("삭제", (dialog, which) -> deleteQnA(item))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteQnA(QnAItem item) {
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document(item.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "질문이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    // Reload data
                    if (currentTab.equals("qna")) {
                        loadQnAData();
                    } else {
                        loadFAQData();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void convertToFAQ(QnAItem item) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("type", "faq");
        updates.put("isPrivate", false);  // FAQ는 비밀 질문이 될 수 없음

        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document(item.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "FAQ로 이동되었습니다", Toast.LENGTH_SHORT).show();
                    // Reload data
                    if (currentTab.equals("qna")) {
                        loadQnAData();
                    } else {
                        loadFAQData();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "FAQ 전환 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void convertToQnA(QnAItem item) {
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("qna")
                .document(item.getId())
                .update("type", "qna")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Q&A로 이동되었습니다", Toast.LENGTH_SHORT).show();
                    // Reload data
                    if (currentTab.equals("qna")) {
                        loadQnAData();
                    } else {
                        loadFAQData();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Q&A 전환 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
