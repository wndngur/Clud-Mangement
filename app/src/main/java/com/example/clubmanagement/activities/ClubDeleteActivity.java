package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.adapters.ClubDeleteAdapter;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ClubDeleteActivity extends AppCompatActivity implements ClubDeleteAdapter.OnSelectionChangedListener {

    private Toolbar toolbar;
    private RecyclerView rvClubs;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private MaterialButton btnDeleteSelected;

    private FirebaseManager firebaseManager;
    private ClubDeleteAdapter adapter;
    private List<Club> clubList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_delete);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadAllClubs();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvClubs = findViewById(R.id.rvClubs);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        progressBar = findViewById(R.id.progressBar);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);

        btnDeleteSelected.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("동아리 삭제");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvClubs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClubDeleteAdapter(clubList, this);
        rvClubs.setAdapter(adapter);
    }

    private void loadAllClubs() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvClubs.setVisibility(View.GONE);

        firebaseManager.getAllClubs(new FirebaseManager.ClubListCallback() {
            @Override
            public void onSuccess(List<Club> clubs) {
                progressBar.setVisibility(View.GONE);
                clubList.clear();
                if (clubs != null) {
                    clubList.addAll(clubs);
                }
                adapter.updateClubs(clubList);

                if (clubList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvClubs.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvClubs.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                rvClubs.setVisibility(View.GONE);
                Toast.makeText(ClubDeleteActivity.this,
                        "동아리 목록을 불러오는데 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSelectionChanged(int selectedCount) {
        btnDeleteSelected.setText("선택한 동아리 삭제 (" + selectedCount + "개)");
        btnDeleteSelected.setEnabled(selectedCount > 0);
    }

    private void showDeleteConfirmationDialog() {
        List<Club> selectedClubs = adapter.getSelectedClubs();
        if (selectedClubs.isEmpty()) {
            Toast.makeText(this, "삭제할 동아리를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 다이얼로그 레이아웃 인플레이트
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete, null);
        TextView tvDeleteMessage = dialogView.findViewById(R.id.tvDeleteMessage);
        TextView tvClubList = dialogView.findViewById(R.id.tvClubList);
        CheckBox cbConfirm = dialogView.findViewById(R.id.cbConfirm);

        tvDeleteMessage.setText("다음 " + selectedClubs.size() + "개의 동아리를 삭제하시겠습니까?");

        // 삭제될 동아리 목록
        StringBuilder clubNames = new StringBuilder();
        for (int i = 0; i < selectedClubs.size(); i++) {
            Club club = selectedClubs.get(i);
            if (i > 0) clubNames.append("\n");
            String type = club.isCentralClub() ? "[중앙] " : "[일반] ";
            clubNames.append(type).append(club.getName());
        }
        tvClubList.setText(clubNames.toString());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("삭제", null)
                .setNegativeButton("취소", null)
                .create();

        dialog.show();

        // 확인 버튼 커스텀 처리 (체크박스 체크 필요)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!cbConfirm.isChecked()) {
                Toast.makeText(this, "삭제에 동의하려면 체크박스를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            deleteSelectedClubs(selectedClubs);
        });
    }

    private void deleteSelectedClubs(List<Club> clubsToDelete) {
        progressBar.setVisibility(View.VISIBLE);
        btnDeleteSelected.setEnabled(false);

        final int[] successCount = {0};
        final int[] failCount = {0};
        final int total = clubsToDelete.size();

        for (Club club : clubsToDelete) {
            firebaseManager.deleteClubCompletely(club.getId(), club.getName(), new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    successCount[0]++;
                    checkDeleteComplete(successCount[0], failCount[0], total);
                }

                @Override
                public void onFailure(Exception e) {
                    failCount[0]++;
                    checkDeleteComplete(successCount[0], failCount[0], total);
                }
            });
        }
    }

    private void checkDeleteComplete(int success, int fail, int total) {
        if (success + fail >= total) {
            progressBar.setVisibility(View.GONE);
            btnDeleteSelected.setEnabled(true);

            if (fail == 0) {
                Toast.makeText(this, success + "개의 동아리가 삭제되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, success + "개 삭제 성공, " + fail + "개 삭제 실패", Toast.LENGTH_SHORT).show();
            }

            // 목록 새로고침
            adapter.clearSelection();
            loadAllClubs();
        }
    }
}
