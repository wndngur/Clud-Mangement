package com.example.clubmanagement.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.models.UserData;
import com.example.clubmanagement.utils.FirebaseManager;

public class ClubInfoActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private boolean isAdmin = false;
    private Club currentClub;
    private String clubId;
    private String clubName;

    // UI Components
    private ImageView ivBack;
    private TextView tvPurpose;
    private TextView tvSchedule;
    private TextView tvMembers;
    private TextView tvLocation;
    private ImageView ivEditPurpose;
    private ImageView ivEditSchedule;
    private ImageView ivEditMembers;
    private ImageView ivEditLocation;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_club_info);

        firebaseManager = FirebaseManager.getInstance();

        // Get club info from intent
        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");

        if (clubId == null) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        checkAdminStatus();
        loadClubInfo();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvPurpose = findViewById(R.id.tvPurpose);
        tvSchedule = findViewById(R.id.tvSchedule);
        tvMembers = findViewById(R.id.tvMembers);
        tvLocation = findViewById(R.id.tvLocation);
        ivEditPurpose = findViewById(R.id.ivEditPurpose);
        ivEditSchedule = findViewById(R.id.ivEditSchedule);
        ivEditMembers = findViewById(R.id.ivEditMembers);
        ivEditLocation = findViewById(R.id.ivEditLocation);
        progressBar = findViewById(R.id.progressBar);
    }

    private void checkAdminStatus() {
        firebaseManager.getUserData(firebaseManager.getCurrentUserId(), new FirebaseManager.UserDataCallback() {
            @Override
            public void onSuccess(UserData userData) {
                if (userData != null) {
                    // Check if user is club admin for THIS club or super admin
                    boolean isSuperAdmin = userData.isSuperAdmin();
                    boolean isThisClubAdmin = userData.isClubAdmin() &&
                                              userData.getClubId() != null &&
                                              userData.getClubId().equals(clubId);

                    isAdmin = isSuperAdmin || isThisClubAdmin;

                    if (isAdmin) {
                        ivEditPurpose.setVisibility(View.VISIBLE);
                        ivEditSchedule.setVisibility(View.VISIBLE);
                        ivEditMembers.setVisibility(View.VISIBLE);
                        ivEditLocation.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                isAdmin = false;
            }
        });
    }

    private void loadClubInfo() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getClub(clubId, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(Club club) {
                progressBar.setVisibility(View.GONE);

                if (club != null) {
                    currentClub = club;
                    displayClubInfo();
                } else {
                    // Club doesn't exist yet, create empty one
                    currentClub = new Club(clubId, clubName != null ? clubName : "동아리");
                    displayClubInfo();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubInfoActivity.this, "동아리 정보 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayClubInfo() {
        if (currentClub == null) return;

        tvPurpose.setText(currentClub.getPurpose() != null && !currentClub.getPurpose().isEmpty()
                ? currentClub.getPurpose()
                : "설립 목적을 입력해주세요");

        tvSchedule.setText(currentClub.getSchedule() != null && !currentClub.getSchedule().isEmpty()
                ? currentClub.getSchedule()
                : "행사 일정을 입력해주세요");

        tvMembers.setText(currentClub.getMembers() != null && !currentClub.getMembers().isEmpty()
                ? currentClub.getMembers()
                : "부원 명단을 입력해주세요");

        tvLocation.setText(currentClub.getLocation() != null && !currentClub.getLocation().isEmpty()
                ? currentClub.getLocation()
                : "동아리방 위치를 입력해주세요");
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        ivEditPurpose.setOnClickListener(v -> showEditDialog("설립 목적", currentClub.getPurpose(), "purpose"));
        ivEditSchedule.setOnClickListener(v -> showEditDialog("행사 일정", currentClub.getSchedule(), "schedule"));
        ivEditMembers.setOnClickListener(v -> showEditDialog("부원 명단", currentClub.getMembers(), "members"));
        ivEditLocation.setOnClickListener(v -> showEditDialog("동아리방 위치", currentClub.getLocation(), "location"));
    }

    private void showEditDialog(String title, String currentValue, String fieldType) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_club_info, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etContent = dialogView.findViewById(R.id.etContent);

        tvDialogTitle.setText(title + " 편집");
        if (currentValue != null && !currentValue.isEmpty()) {
            etContent.setText(currentValue);
            etContent.setSelection(currentValue.length());
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("저장", (dialog, which) -> {
                    String newValue = etContent.getText().toString().trim();
                    updateClubInfo(fieldType, newValue);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void updateClubInfo(String fieldType, String value) {
        if (currentClub == null) {
            currentClub = new Club(clubId, clubName != null ? clubName : "동아리");
        }

        switch (fieldType) {
            case "purpose":
                currentClub.setPurpose(value);
                break;
            case "schedule":
                currentClub.setSchedule(value);
                break;
            case "members":
                currentClub.setMembers(value);
                break;
            case "location":
                currentClub.setLocation(value);
                break;
        }

        saveClubInfo();
    }

    private void saveClubInfo() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.saveClub(currentClub, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(Club club) {
                progressBar.setVisibility(View.GONE);
                currentClub = club;
                displayClubInfo();
                Toast.makeText(ClubInfoActivity.this, "저장 완료", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubInfoActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
