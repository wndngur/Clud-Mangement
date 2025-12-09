package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.adapters.DemotableCentralClubAdapter;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DemoteCentralClubActivity extends BaseActivity {

    private ImageView ivBack;
    private RecyclerView rvCentralClubs;
    private LinearLayout llEmptyState;
    private MaterialButton btnDemote;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private DemotableCentralClubAdapter adapter;
    private List<Club> centralClubs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demote_central_club);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCentralClubs();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        rvCentralClubs = findViewById(R.id.rvCentralClubs);
        llEmptyState = findViewById(R.id.llEmptyState);
        btnDemote = findViewById(R.id.btnDemote);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new DemotableCentralClubAdapter();
        rvCentralClubs.setLayoutManager(new LinearLayoutManager(this));
        rvCentralClubs.setAdapter(adapter);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnDemote.setOnClickListener(v -> {
            List<Club> selectedClubs = adapter.getSelectedClubs();
            if (selectedClubs.isEmpty()) {
                Toast.makeText(this, "강등할 동아리를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            showDemoteConfirmationDialog(selectedClubs);
        });
    }

    private void loadCentralClubs() {
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);
        rvCentralClubs.setVisibility(View.GONE);

        firebaseManager.getAllClubs(new FirebaseManager.ClubListCallback() {
            @Override
            public void onSuccess(List<Club> clubs) {
                progressBar.setVisibility(View.GONE);

                // 중앙동아리만 필터링
                centralClubs.clear();
                for (Club club : clubs) {
                    if (club.isCentralClub()) {
                        centralClubs.add(club);
                    }
                }

                if (centralClubs.isEmpty()) {
                    llEmptyState.setVisibility(View.VISIBLE);
                    rvCentralClubs.setVisibility(View.GONE);
                } else {
                    llEmptyState.setVisibility(View.GONE);
                    rvCentralClubs.setVisibility(View.VISIBLE);
                    adapter.setClubs(centralClubs);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DemoteCentralClubActivity.this,
                        "동아리 목록 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                llEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showDemoteConfirmationDialog(List<Club> selectedClubs) {
        // 다이얼로그 내용 생성
        StringBuilder message = new StringBuilder();
        message.append("다음 동아리를 일반동아리로 강등하시겠습니까?\n\n");

        for (Club club : selectedClubs) {
            message.append("• ").append(club.getName()).append("\n");
        }

        message.append("\n⚠️ 이 작업은 되돌릴 수 없습니다.\n");
        message.append("계속하려면 아래 체크박스를 선택하세요.");

        // 커스텀 다이얼로그 뷰 생성
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_demote_confirmation, null);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        CheckBox cbConfirm = dialogView.findViewById(R.id.cbConfirm);

        tvMessage.setText(message.toString());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("동아리 강등 확인")
                .setView(dialogView)
                .setPositiveButton("강등하기", null)  // null로 설정하고 나중에 오버라이드
                .setNegativeButton("취소", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            MaterialButton positiveButton = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                if (!cbConfirm.isChecked()) {
                    Toast.makeText(DemoteCentralClubActivity.this,
                            "체크박스를 선택해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                demoteClubs(selectedClubs);
            });
        });

        dialog.show();
    }

    private void demoteClubs(List<Club> clubsToDemote) {
        progressBar.setVisibility(View.VISIBLE);
        btnDemote.setEnabled(false);

        final int totalClubs = clubsToDemote.size();
        final int[] completedCount = {0};
        final int[] successCount = {0};

        for (Club club : clubsToDemote) {
            firebaseManager.cancelCentralClubStatus(club.getId(), new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    successCount[0]++;
                    completedCount[0]++;

                    if (completedCount[0] == totalClubs) {
                        onDemoteComplete(totalClubs, successCount[0]);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    completedCount[0]++;

                    if (completedCount[0] == totalClubs) {
                        onDemoteComplete(totalClubs, successCount[0]);
                    }
                }
            });
        }
    }

    private void onDemoteComplete(int total, int success) {
        progressBar.setVisibility(View.GONE);
        btnDemote.setEnabled(true);

        String message;
        if (success == total) {
            message = total + "개 동아리가 강등되었습니다";
        } else {
            message = success + "/" + total + "개 동아리가 강등되었습니다";
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // 목록 새로고침
        loadCentralClubs();
    }
}
