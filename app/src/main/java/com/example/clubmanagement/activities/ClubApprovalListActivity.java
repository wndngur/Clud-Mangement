package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.adapters.ClubApprovalAdapter;
import com.example.clubmanagement.models.ClubApplication;
import com.example.clubmanagement.utils.FirebaseManager;

import java.util.List;

public class ClubApprovalListActivity extends BaseActivity implements ClubApprovalAdapter.OnApprovalActionListener {

    private ImageView ivBack;
    private RecyclerView rvApplications;
    private TextView tvNoApplications;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private ClubApprovalAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ActionBar 숨기기
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_club_approval_list);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupRecyclerView();
        loadApplications();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        rvApplications = findViewById(R.id.rvApplications);
        tvNoApplications = findViewById(R.id.tvNoApplications);
        progressBar = findViewById(R.id.progressBar);

        ivBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ClubApprovalAdapter(this, this);
        rvApplications.setLayoutManager(new LinearLayoutManager(this));
        rvApplications.setAdapter(adapter);
    }

    private void loadApplications() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoApplications.setVisibility(View.GONE);

        Toast.makeText(this, "데이터 로딩 중...", Toast.LENGTH_SHORT).show();

        firebaseManager.getPendingClubApplications(new FirebaseManager.ClubApplicationListCallback() {
            @Override
            public void onSuccess(List<ClubApplication> applications) {
                progressBar.setVisibility(View.GONE);

                Toast.makeText(ClubApprovalListActivity.this,
                        "로드 완료: " + (applications != null ? applications.size() : 0) + "건",
                        Toast.LENGTH_LONG).show();

                if (applications == null || applications.isEmpty()) {
                    tvNoApplications.setVisibility(View.VISIBLE);
                    rvApplications.setVisibility(View.GONE);
                } else {
                    tvNoApplications.setVisibility(View.GONE);
                    rvApplications.setVisibility(View.VISIBLE);
                    adapter.setApplications(applications);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                String errorMsg = e != null ? e.getMessage() : "알 수 없는 오류";
                Toast.makeText(ClubApprovalListActivity.this,
                        "로드 실패: " + errorMsg,
                        Toast.LENGTH_LONG).show();

                // AlertDialog로 상세 에러 표시
                new android.app.AlertDialog.Builder(ClubApprovalListActivity.this)
                        .setTitle("Firestore 에러")
                        .setMessage(errorMsg)
                        .setPositiveButton("확인", null)
                        .show();
            }
        });
    }

    @Override
    public void onApprove(ClubApplication application) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.approveClubApplication(application, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubApprovalListActivity.this,
                        "'" + application.getClubName() + "' 동아리가 승인되었습니다.",
                        Toast.LENGTH_SHORT).show();
                loadApplications(); // 목록 새로고침
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubApprovalListActivity.this,
                        "승인 실패: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReject(ClubApplication application) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.rejectClubApplication(application.getId(), new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubApprovalListActivity.this,
                        "'" + application.getClubName() + "' 동아리 신청이 거절되었습니다.",
                        Toast.LENGTH_SHORT).show();
                loadApplications(); // 목록 새로고침
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubApprovalListActivity.this,
                        "거절 처리 실패: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
