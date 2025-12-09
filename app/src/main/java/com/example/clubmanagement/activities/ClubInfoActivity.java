package com.example.clubmanagement.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.models.UserData;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClubInfoActivity extends BaseActivity {

    private FirebaseManager firebaseManager;
    private boolean isAdmin = false;
    private boolean fromClubList = false;  // 일반동아리 목록에서 왔는지 여부
    private Club currentClub;
    private String clubId;
    private String clubName;

    // UI Components
    private ImageView ivBack;
    private TextView tvPurpose;
    private TextView tvProfessor;
    private TextView tvDepartment;
    private TextView tvSchedule;
    private TextView tvLocation;
    private ImageView ivEditPurpose;
    private ImageView ivEditProfessor;
    private ImageView ivEditSchedule;
    private ImageView ivEditLocation;
    private ProgressBar progressBar;
    private CardView cardKeywords;
    private ChipGroup chipGroupKeywords;
    private MaterialButton btnJoinClub;
    private CardView cardApplicationClosed;
    private TextView tvApplicationClosed;

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
        fromClubList = getIntent().getBooleanExtra("from_club_list", false);

        if (clubId == null) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        checkAdminStatus();
        loadClubInfo();
        setupListeners();
        setupJoinButton();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvPurpose = findViewById(R.id.tvPurpose);
        tvProfessor = findViewById(R.id.tvProfessor);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvSchedule = findViewById(R.id.tvSchedule);
        tvLocation = findViewById(R.id.tvLocation);
        ivEditPurpose = findViewById(R.id.ivEditPurpose);
        ivEditProfessor = findViewById(R.id.ivEditProfessor);
        ivEditSchedule = findViewById(R.id.ivEditSchedule);
        ivEditLocation = findViewById(R.id.ivEditLocation);
        progressBar = findViewById(R.id.progressBar);
        cardKeywords = findViewById(R.id.cardKeywords);
        chipGroupKeywords = findViewById(R.id.chipGroupKeywords);
        btnJoinClub = findViewById(R.id.btnJoinClub);
        cardApplicationClosed = findViewById(R.id.cardApplicationClosed);
        tvApplicationClosed = findViewById(R.id.tvApplicationClosed);
    }

    private void checkAdminStatus() {
        // 동아리 정보 페이지는 읽기 전용입니다.
        // 편집은 관리자 모드에서 설정 > 동아리 정보 수정을 통해 가능합니다.
        isAdmin = false;
        ivEditPurpose.setVisibility(View.GONE);
        ivEditProfessor.setVisibility(View.GONE);
        ivEditSchedule.setVisibility(View.GONE);
        ivEditLocation.setVisibility(View.GONE);
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

        tvProfessor.setText(currentClub.getProfessor() != null && !currentClub.getProfessor().isEmpty()
                ? currentClub.getProfessor()
                : "-");

        tvDepartment.setText(currentClub.getDepartment() != null && !currentClub.getDepartment().isEmpty()
                ? currentClub.getDepartment()
                : "-");

        tvSchedule.setText(currentClub.getSchedule() != null && !currentClub.getSchedule().isEmpty()
                ? currentClub.getSchedule()
                : "행사 일정을 입력해주세요");

        tvLocation.setText(currentClub.getLocation() != null && !currentClub.getLocation().isEmpty()
                ? currentClub.getLocation()
                : "동아리방 위치를 입력해주세요");

        // 키워드 표시
        displayKeywords();
    }

    private void displayKeywords() {
        if (currentClub == null || !currentClub.hasKeywords()) {
            cardKeywords.setVisibility(View.GONE);
            return;
        }

        cardKeywords.setVisibility(View.VISIBLE);
        chipGroupKeywords.removeAllViews();

        // 기독교 동아리
        if (currentClub.isChristian()) {
            addChip("⛪ 기독교");
        }

        // 분위기
        String atmosphere = currentClub.getAtmosphere();
        if (atmosphere != null) {
            if ("lively".equals(atmosphere)) {
                addChip("🎉 활기찬");
            } else if ("quiet".equals(atmosphere)) {
                addChip("📚 조용한");
            }
        }

        // 활동 유형
        List<String> activityTypes = currentClub.getActivityTypes();
        if (activityTypes != null) {
            for (String type : activityTypes) {
                switch (type) {
                    case "volunteer":
                        addChip("🤝 봉사활동");
                        break;
                    case "sports":
                        addChip("⚽ 운동");
                        break;
                    case "outdoor":
                        addChip("🌳 야외활동");
                        break;
                }
            }
        }

        // 목적
        List<String> purposes = currentClub.getPurposes();
        if (purposes != null) {
            for (String purpose : purposes) {
                switch (purpose) {
                    case "career":
                        addChip("💼 취업/스펙");
                        break;
                    case "academic":
                        addChip("🔬 학술/연구");
                        break;
                    case "art":
                        addChip("🎨 예술/문화");
                        break;
                }
            }
        }
    }

    private void addChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setClickable(false);
        chip.setCheckable(false);
        chipGroupKeywords.addView(chip);
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        ivEditPurpose.setOnClickListener(v -> showEditDialog("설립 목적", currentClub.getPurpose(), "purpose"));
        ivEditSchedule.setOnClickListener(v -> showEditDialog("행사 일정", currentClub.getSchedule(), "schedule"));
        ivEditLocation.setOnClickListener(v -> showEditDialog("동아리방 위치", currentClub.getLocation(), "location"));
    }

    private void setupJoinButton() {
        // 일반동아리 목록에서 온 경우에만 버튼 표시
        if (fromClubList) {
            // 최고 관리자 모드인지 확인
            boolean isSuperAdmin = SettingsActivity.isSuperAdminMode(this);

            if (isSuperAdmin) {
                // 최고 관리자: 동아리 관리하기 버튼 (항상 표시)
                btnJoinClub.setVisibility(View.VISIBLE);
                if (cardApplicationClosed != null) {
                    cardApplicationClosed.setVisibility(View.GONE);
                }
                btnJoinClub.setText("동아리 관리하기");
                btnJoinClub.setIconResource(android.R.drawable.ic_menu_manage);
                btnJoinClub.setOnClickListener(v -> {
                    // ClubMainActivity로 이동 (관리자 모드)
                    Intent intent = new Intent(ClubInfoActivity.this, ClubMainActivity.class);
                    intent.putExtra("club_name", clubName != null ? clubName : "동아리");
                    intent.putExtra("club_id", clubId);
                    startActivity(intent);
                });
            } else {
                // 일반 사용자: 가입 신청 설정 확인 후 버튼 표시
                checkApplicationSettingsAndShowButton();
            }
        } else {
            btnJoinClub.setVisibility(View.GONE);
            if (cardApplicationClosed != null) {
                cardApplicationClosed.setVisibility(View.GONE);
            }
        }
    }

    private void checkApplicationSettingsAndShowButton() {
        firebaseManager.getApplicationSettings(clubId, new FirebaseManager.ApplicationSettingsCallback() {
            @Override
            public void onResult(boolean isOpen, Timestamp endDate) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    // 마감일 확인 (마감일이 지났으면 자동으로 닫힘)
                    boolean isActuallyOpen = isOpen;
                    if (isOpen && endDate != null) {
                        Date now = new Date();
                        if (endDate.toDate().before(now)) {
                            isActuallyOpen = false;
                        }
                    }

                    if (isActuallyOpen) {
                        // 가입 신청 받는 중
                        btnJoinClub.setVisibility(View.VISIBLE);
                        if (cardApplicationClosed != null) {
                            cardApplicationClosed.setVisibility(View.GONE);
                        }
                        btnJoinClub.setText("가입하기");
                        btnJoinClub.setIconResource(android.R.drawable.ic_input_add);
                        btnJoinClub.setOnClickListener(v -> {
                            Intent intent = new Intent(ClubInfoActivity.this, MemberRegistrationActivity.class);
                            intent.putExtra("club_name", clubName != null ? clubName : "동아리");
                            intent.putExtra("central_club_id", clubId);
                            intent.putExtra("is_central_club", false);
                            startActivity(intent);
                        });
                    } else {
                        // 가입 신청 중단됨
                        btnJoinClub.setVisibility(View.GONE);
                        if (cardApplicationClosed != null) {
                            cardApplicationClosed.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
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
