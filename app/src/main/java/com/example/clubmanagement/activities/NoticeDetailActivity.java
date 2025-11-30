package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.example.clubmanagement.adapters.NoticeCommentAdapter;
import com.example.clubmanagement.models.ClubNotice;
import com.example.clubmanagement.models.NoticeComment;
import com.example.clubmanagement.utils.FirebaseManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoticeDetailActivity extends AppCompatActivity implements NoticeCommentAdapter.OnCommentActionListener {

    private ImageView ivBack;
    private ImageView ivMore;
    private TextView tvPinnedBadge;
    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvDate;
    private TextView tvViewCount;
    private TextView tvContent;
    private TextView tvCommentHeader;
    private RecyclerView rvComments;
    private TextView tvNoComments;
    private EditText etComment;
    private ImageView ivSendComment;
    private ProgressBar progressBar;

    private String clubId;
    private String clubName;
    private String noticeId;
    private boolean isAdmin;

    private FirebaseManager firebaseManager;
    private NoticeCommentAdapter commentAdapter;
    private ClubNotice currentNotice;
    private String currentUserId;
    private String currentUserName;

    // 권한 관리용
    private boolean isSuperAdminMode = false;
    private boolean isClubAdminMode = false;
    private Set<String> clubMemberIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_notice_detail);

        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");
        noticeId = getIntent().getStringExtra("notice_id");
        isAdmin = getIntent().getBooleanExtra("is_admin", false);

        if (clubId == null || noticeId == null) {
            Toast.makeText(this, "공지 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
        currentUserId = firebaseManager.getCurrentUserId();

        // 관리자 모드 체크
        isSuperAdminMode = SettingsActivity.isSuperAdminMode(this);
        isClubAdminMode = ClubSettingsActivity.isClubAdminMode(this);

        initViews();
        setupListeners();
        loadNotice();
        loadComments();
        loadCurrentUserName();
        loadClubMembers();

        // 조회수 증가
        firebaseManager.incrementNoticeViewCount(clubId, noticeId);
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        ivMore = findViewById(R.id.ivMore);
        tvPinnedBadge = findViewById(R.id.tvPinnedBadge);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDate = findViewById(R.id.tvDate);
        tvViewCount = findViewById(R.id.tvViewCount);
        tvContent = findViewById(R.id.tvContent);
        tvCommentHeader = findViewById(R.id.tvCommentHeader);
        rvComments = findViewById(R.id.rvComments);
        tvNoComments = findViewById(R.id.tvNoComments);
        etComment = findViewById(R.id.etComment);
        ivSendComment = findViewById(R.id.ivSendComment);
        progressBar = findViewById(R.id.progressBar);

        commentAdapter = new NoticeCommentAdapter(currentUserId, this);
        commentAdapter.setSuperAdminMode(isSuperAdminMode);
        commentAdapter.setClubAdminMode(isClubAdminMode);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        // 관리자이고 관리자 모드가 활성화되어 있을 때만 더보기 버튼 표시
        if (isAdmin && (isClubAdminMode || isSuperAdminMode)) {
            ivMore.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        ivMore.setOnClickListener(v -> showMoreMenu());

        ivSendComment.setOnClickListener(v -> submitComment());
    }

    private void loadNotice() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getClubNotice(clubId, noticeId, new FirebaseManager.ClubNoticeCallback() {
            @Override
            public void onSuccess(ClubNotice notice) {
                progressBar.setVisibility(View.GONE);
                currentNotice = notice;
                displayNotice(notice);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NoticeDetailActivity.this, "공지 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayNotice(ClubNotice notice) {
        tvTitle.setText(notice.getTitle());
        tvAuthor.setText(notice.getAuthorName());
        tvDate.setText(notice.getFormattedDate());
        tvViewCount.setText("조회 " + (notice.getViewCount() + 1)); // 현재 조회 포함
        tvContent.setText(notice.getContent());

        if (notice.isPinned()) {
            tvPinnedBadge.setVisibility(View.VISIBLE);
        } else {
            tvPinnedBadge.setVisibility(View.GONE);
        }
    }

    private void loadComments() {
        firebaseManager.getNoticeComments(clubId, noticeId, new FirebaseManager.NoticeCommentListCallback() {
            @Override
            public void onSuccess(List<NoticeComment> comments) {
                tvCommentHeader.setText("댓글 " + comments.size());

                if (comments.isEmpty()) {
                    tvNoComments.setVisibility(View.VISIBLE);
                    rvComments.setVisibility(View.GONE);
                } else {
                    tvNoComments.setVisibility(View.GONE);
                    rvComments.setVisibility(View.VISIBLE);
                    commentAdapter.setComments(comments);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(NoticeDetailActivity.this, "댓글 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCurrentUserName() {
        if (currentUserId == null) return;

        firebaseManager.getDb().collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserName = doc.getString("name");
                        if (currentUserName == null || currentUserName.isEmpty()) {
                            currentUserName = doc.getString("email");
                        }
                    }
                });
    }

    private void loadClubMembers() {
        if (clubId == null) return;

        // 동아리 부원 목록 로드 (동아리 관리자의 댓글 수정/삭제 권한 체크용)
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("members")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    clubMemberIds.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String memberId = doc.getString("userId");
                        if (memberId != null) {
                            clubMemberIds.add(memberId);
                        }
                    }
                    // 어댑터에 부원 ID 목록 전달
                    commentAdapter.setClubMemberIds(clubMemberIds);
                });
    }

    private void submitComment() {
        String content = etComment.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "댓글을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String authorName = currentUserName != null ? currentUserName : "익명";
        NoticeComment comment = new NoticeComment(noticeId, clubId, content, currentUserId, authorName);

        ivSendComment.setEnabled(false);

        firebaseManager.createNoticeComment(comment, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                ivSendComment.setEnabled(true);
                etComment.setText("");
                loadComments();
                Toast.makeText(NoticeDetailActivity.this, "댓글이 등록되었습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                ivSendComment.setEnabled(true);
                Toast.makeText(NoticeDetailActivity.this, "댓글 등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMoreMenu() {
        PopupMenu popup = new PopupMenu(this, ivMore);
        popup.getMenu().add("수정");
        popup.getMenu().add("삭제");

        if (currentNotice != null) {
            if (currentNotice.isPinned()) {
                popup.getMenu().add("고정 해제");
            } else {
                popup.getMenu().add("상단 고정");
            }
        }

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            switch (title) {
                case "수정":
                    editNotice();
                    return true;
                case "삭제":
                    confirmDeleteNotice();
                    return true;
                case "상단 고정":
                case "고정 해제":
                    togglePinned();
                    return true;
            }
            return false;
        });

        popup.show();
    }

    private void editNotice() {
        Intent intent = new Intent(this, NoticeWriteActivity.class);
        intent.putExtra("club_id", clubId);
        intent.putExtra("club_name", clubName);
        intent.putExtra("notice_id", noticeId);
        intent.putExtra("edit_mode", true);
        startActivity(intent);
        finish();
    }

    private void confirmDeleteNotice() {
        new AlertDialog.Builder(this)
                .setTitle("공지 삭제")
                .setMessage("이 공지를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteNotice())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteNotice() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.deleteClubNotice(clubId, noticeId, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NoticeDetailActivity.this, "공지가 삭제되었습니다", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NoticeDetailActivity.this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void togglePinned() {
        if (currentNotice == null) return;

        currentNotice.setPinned(!currentNotice.isPinned());

        firebaseManager.updateClubNotice(currentNotice, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                String message = currentNotice.isPinned() ? "공지가 상단에 고정되었습니다" : "고정이 해제되었습니다";
                Toast.makeText(NoticeDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                displayNotice(currentNotice);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(NoticeDetailActivity.this, "변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEditComment(NoticeComment comment) {
        EditText editText = new EditText(this);
        editText.setText(comment.getContent());

        new AlertDialog.Builder(this)
                .setTitle("댓글 수정")
                .setView(editText)
                .setPositiveButton("수정", (dialog, which) -> {
                    String newContent = editText.getText().toString().trim();
                    if (!TextUtils.isEmpty(newContent)) {
                        comment.setContent(newContent);
                        firebaseManager.updateNoticeComment(comment, new FirebaseManager.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                loadComments();
                                Toast.makeText(NoticeDetailActivity.this, "댓글이 수정되었습니다", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(NoticeDetailActivity.this, "수정 실패", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    @Override
    public void onDeleteComment(NoticeComment comment) {
        new AlertDialog.Builder(this)
                .setTitle("댓글 삭제")
                .setMessage("이 댓글을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    firebaseManager.deleteNoticeComment(clubId, noticeId, comment.getId(), new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            loadComments();
                            Toast.makeText(NoticeDetailActivity.this, "댓글이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(NoticeDetailActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
