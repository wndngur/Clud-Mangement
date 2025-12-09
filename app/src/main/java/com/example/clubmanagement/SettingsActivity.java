package com.example.clubmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.LinearLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.example.clubmanagement.activities.CentralApplicationsActivity;
import com.example.clubmanagement.activities.ChatActivity;
import com.example.clubmanagement.activities.ClubApprovalListActivity;
import com.example.clubmanagement.activities.ClubDeleteActivity;
import com.example.clubmanagement.activities.ClubEstablishActivity;
import com.example.clubmanagement.activities.ClubListActivity;
import com.example.clubmanagement.activities.ClubMainActivity;
import com.example.clubmanagement.activities.ClubRecommendActivity;
import com.example.clubmanagement.activities.DemoteCentralClubActivity;
import com.example.clubmanagement.models.User;
import com.example.clubmanagement.utils.FirebaseManager;
import com.example.clubmanagement.utils.TestDataInjector;
import com.example.clubmanagement.utils.ThemeHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.widget.RadioGroup;

public class SettingsActivity extends BaseActivity {

    public static final String PREFS_NAME = "SuperAdminPrefs";
    public static final String KEY_SUPER_ADMIN_MODE = "super_admin_mode_active";

    private BottomNavigationView bottomNavigation;
    private TextView tvUserEmail;
    private TextView tvUserName;
    private TextView tvUserDepartment;
    private TextView tvUserPhone;
    private MaterialCardView cardAccountInfo;
    private MaterialButton btnLogout;
    private MaterialButton btnCentralClubApplications;
    private MaterialCardView cardCentralClubApplications;
    private MaterialButton btnClubEstablish;
    private MaterialCardView cardClubEstablish;
    private MaterialButton btnClubApproval;
    private MaterialCardView cardClubApproval;
    private MaterialCardView cardClubDelete;
    private MaterialButton btnClubDelete;
    private MaterialCardView cardClubDemote;
    private MaterialButton btnClubDemote;
    private MaterialCardView cardMemberLimit;
    private MaterialButton btnEditMemberLimit;
    private TextView tvCurrentRegisterLimit;
    private TextView tvCurrentMaintainLimit;
    private MaterialCardView cardBannerManagement;
    private MaterialButton btnAddGlobalBanner;
    private MaterialButton btnClearGlobalBanners;
    private MaterialCardView cardMyClubs;
    private MaterialButton btnMyClubs;
    private TextView tvMyClubsDescription;
    private LinearLayout llMyClubsList;
    private LinearLayout llCentralClubSection;
    private TextView tvCentralClubName;
    private LinearLayout llGeneralClubsSection;
    private LinearLayout llGeneralClubsList;
    private MaterialCardView cardTestData;
    private MaterialButton btnInjectTestData;
    private MaterialButton btnDeleteTestData;
    private SwitchMaterial switchNotification;
    private RadioGroup rgTheme;
    private MaterialCardView cardGlobalNotice;
    private MaterialButton btnGlobalNotice;
    private FirebaseAuth firebaseAuth;
    private FirebaseManager firebaseManager;
    private User currentUser;

    // 현재 인원 제한 값
    private int currentRegisterLimit = 20;
    private int currentMaintainLimit = 15;

    // 배너 이미지 관련 변수
    private Uri selectedBannerImageUri = null;
    private ImageView currentBannerPreview = null;
    private LinearLayout currentSelectImageLayout = null;
    private ActivityResultLauncher<String> bannerImagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseManager = FirebaseManager.getInstance();

        // 이미지 선택 launcher 초기화
        initBannerImagePicker();

        initViews();
        loadUserInfo();
        loadMemberLimits();
        loadUserClubs();
        setupListeners();
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserDepartment = findViewById(R.id.tvUserDepartment);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        cardAccountInfo = findViewById(R.id.cardAccountInfo);
        btnLogout = findViewById(R.id.btnLogout);
        btnCentralClubApplications = findViewById(R.id.btnCentralClubApplications);
        cardCentralClubApplications = findViewById(R.id.cardCentralClubApplications);
        btnClubEstablish = findViewById(R.id.btnClubEstablish);
        cardClubEstablish = findViewById(R.id.cardClubEstablish);
        btnClubApproval = findViewById(R.id.btnClubApproval);
        cardClubApproval = findViewById(R.id.cardClubApproval);
        cardClubDelete = findViewById(R.id.cardClubDelete);
        btnClubDelete = findViewById(R.id.btnClubDelete);
        cardClubDemote = findViewById(R.id.cardClubDemote);
        btnClubDemote = findViewById(R.id.btnClubDemote);
        cardMemberLimit = findViewById(R.id.cardMemberLimit);
        btnEditMemberLimit = findViewById(R.id.btnEditMemberLimit);
        tvCurrentRegisterLimit = findViewById(R.id.tvCurrentRegisterLimit);
        tvCurrentMaintainLimit = findViewById(R.id.tvCurrentMaintainLimit);
        cardBannerManagement = findViewById(R.id.cardBannerManagement);
        btnAddGlobalBanner = findViewById(R.id.btnAddGlobalBanner);
        btnClearGlobalBanners = findViewById(R.id.btnClearGlobalBanners);
        cardMyClubs = findViewById(R.id.cardMyClubs);
        btnMyClubs = findViewById(R.id.btnMyClubs);
        tvMyClubsDescription = findViewById(R.id.tvMyClubsDescription);
        llMyClubsList = findViewById(R.id.llMyClubsList);
        llCentralClubSection = findViewById(R.id.llCentralClubSection);
        tvCentralClubName = findViewById(R.id.tvCentralClubName);
        llGeneralClubsSection = findViewById(R.id.llGeneralClubsSection);
        llGeneralClubsList = findViewById(R.id.llGeneralClubsList);
        cardTestData = findViewById(R.id.cardTestData);
        btnInjectTestData = findViewById(R.id.btnInjectTestData);
        btnDeleteTestData = findViewById(R.id.btnDeleteTestData);
        switchNotification = findViewById(R.id.switchNotification);
        rgTheme = findViewById(R.id.rgTheme);
        cardGlobalNotice = findViewById(R.id.cardGlobalNotice);
        btnGlobalNotice = findViewById(R.id.btnGlobalNotice);

        // 내정보 탭 선택 상태로 설정
        bottomNavigation.setSelectedItemId(R.id.nav_myinfo);

        // 테마 및 알림 설정 초기화
        initThemeAndNotificationSettings();

        // 최고 관리자 전용 UI 업데이트
        updateSuperAdminUI();

        // 최고 관리자에게 동아리 추천 메뉴 숨기기
        updateNavigationForSuperAdmin();
    }

    private void initThemeAndNotificationSettings() {
        // 알림 설정 로드
        switchNotification.setChecked(ThemeHelper.isNotificationsEnabled(this));

        // 알림 스위치 리스너
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ThemeHelper.setNotificationsEnabled(this, isChecked);
            if (isChecked) {
                Toast.makeText(this, "알림이 활성화되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "알림이 비활성화되었습니다", Toast.LENGTH_SHORT).show();
            }
        });

        // 테마 설정 로드
        int currentTheme = ThemeHelper.getTheme(this);
        switch (currentTheme) {
            case ThemeHelper.THEME_BLACK_WHITE:
                rgTheme.check(R.id.rbThemeBW);
                break;
            case ThemeHelper.THEME_GRAYSCALE:
                rgTheme.check(R.id.rbThemeGrayscale);
                break;
            case ThemeHelper.THEME_ORIGINAL:
            default:
                rgTheme.check(R.id.rbThemeOriginal);
                break;
        }

        // 테마 선택 리스너
        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int newTheme;
            String themeName;

            if (checkedId == R.id.rbThemeBW) {
                newTheme = ThemeHelper.THEME_BLACK_WHITE;
                themeName = "블랙 앤 화이트";
            } else if (checkedId == R.id.rbThemeGrayscale) {
                newTheme = ThemeHelper.THEME_GRAYSCALE;
                themeName = "흑백";
            } else {
                newTheme = ThemeHelper.THEME_ORIGINAL;
                themeName = "오리지널";
            }

            ThemeHelper.setTheme(this, newTheme);
            ThemeHelper.applyTheme(this);
            Toast.makeText(this, themeName + " 테마가 적용되었습니다", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSuperAdminUI() {
        boolean isSuperAdmin = isSuperAdminMode(this);
        cardCentralClubApplications.setVisibility(isSuperAdmin ? View.VISIBLE : View.GONE);
        cardGlobalNotice.setVisibility(isSuperAdmin ? View.VISIBLE : View.GONE);
        cardClubApproval.setVisibility(isSuperAdmin ? View.VISIBLE : View.GONE);
        cardClubDelete.setVisibility(isSuperAdmin ? View.VISIBLE : View.GONE);
        cardClubDemote.setVisibility(isSuperAdmin ? View.VISIBLE : View.GONE);
        cardMemberLimit.setVisibility(isSuperAdmin ? View.VISIBLE : View.GONE);
        cardBannerManagement.setVisibility(isSuperAdmin ? View.VISIBLE : View.GONE);
        cardTestData.setVisibility(isSuperAdmin ? View.VISIBLE : View.GONE);
        // 최고 관리자는 동아리 설립 신청 카드와 내 동아리 카드 숨김
        cardClubEstablish.setVisibility(isSuperAdmin ? View.GONE : View.VISIBLE);
        cardMyClubs.setVisibility(isSuperAdmin ? View.GONE : View.VISIBLE);
    }

    private void loadMemberLimits() {
        firebaseManager.getMemberLimits(new FirebaseManager.MemberLimitsCallback() {
            @Override
            public void onSuccess(int registerLimit, int maintainLimit) {
                currentRegisterLimit = registerLimit;
                currentMaintainLimit = maintainLimit;
                updateMemberLimitDisplay();
            }

            @Override
            public void onFailure(Exception e) {
                // 기본값 사용
                updateMemberLimitDisplay();
            }
        });
    }

    private void updateMemberLimitDisplay() {
        tvCurrentRegisterLimit.setText(currentRegisterLimit + "명");
        tvCurrentMaintainLimit.setText(currentMaintainLimit + "명");
    }

    private void loadUserClubs() {
        // 최고 관리자는 내 동아리 카드 숨김
        if (isSuperAdminMode(this)) {
            cardMyClubs.setVisibility(View.GONE);
            return;
        }

        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) {
            cardMyClubs.setVisibility(View.GONE);
            return;
        }

        firebaseManager.getCurrentUser(new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                updateMyClubsUI();
            }

            @Override
            public void onFailure(Exception e) {
                cardMyClubs.setVisibility(View.GONE);
            }
        });
    }

    private void updateMyClubsUI() {
        if (currentUser == null) {
            cardMyClubs.setVisibility(View.GONE);
            return;
        }

        boolean hasCentralClub = currentUser.hasJoinedCentralClub();
        int generalClubCount = currentUser.getGeneralClubCount();
        int totalClubs = (hasCentralClub ? 1 : 0) + generalClubCount;

        if (totalClubs == 0) {
            // 가입한 동아리가 없음
            cardMyClubs.setVisibility(View.VISIBLE);
            tvMyClubsDescription.setText("가입한 동아리가 없습니다.");
            llCentralClubSection.setVisibility(View.GONE);
            llGeneralClubsSection.setVisibility(View.GONE);
            llMyClubsList.setVisibility(View.GONE);
            btnMyClubs.setVisibility(View.GONE);
            return;
        }

        // 가입한 동아리가 있음
        cardMyClubs.setVisibility(View.VISIBLE);
        tvMyClubsDescription.setText("총 " + totalClubs + "개 동아리에 가입되어 있습니다.");

        // 기존 llMyClubsList는 숨김
        llMyClubsList.setVisibility(View.GONE);

        // 중앙동아리 섹션 표시
        if (hasCentralClub) {
            llCentralClubSection.setVisibility(View.VISIBLE);
            String centralClubName = currentUser.getCentralClubName();
            tvCentralClubName.setText(centralClubName != null ? centralClubName : "중앙동아리");

            // 중앙동아리 클릭 리스너
            tvCentralClubName.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, ClubMainActivity.class);
                intent.putExtra("clubId", currentUser.getCentralClubId());
                intent.putExtra("club_name", currentUser.getCentralClubName());
                intent.putExtra("isCentralClub", true);
                startActivity(intent);
            });
        } else {
            llCentralClubSection.setVisibility(View.GONE);
        }

        // 일반동아리 섹션 표시 (실제로 중앙동아리인지 확인)
        java.util.List<String> generalClubIds = currentUser.getGeneralClubIds();
        java.util.List<String> generalClubNames = currentUser.getGeneralClubNames();

        if (generalClubIds != null && !generalClubIds.isEmpty()) {
            llGeneralClubsList.removeAllViews();
            checkAndDisplayGeneralClubs(generalClubIds, generalClubNames, hasCentralClub);
        } else {
            llGeneralClubsSection.setVisibility(View.GONE);
        }

        // 버튼 숨김 (이미 목록으로 표시됨)
        btnMyClubs.setVisibility(View.GONE);
    }

    private void checkAndDisplayGeneralClubs(java.util.List<String> generalClubIds, java.util.List<String> generalClubNames, boolean hasCentralClub) {
        // 동아리가 실제로 중앙동아리인지 확인하고 올바른 섹션에 표시
        java.util.concurrent.atomic.AtomicInteger pendingChecks = new java.util.concurrent.atomic.AtomicInteger(generalClubIds.size());
        java.util.List<String[]> generalClubs = new java.util.ArrayList<>();
        java.util.List<String[]> centralClubs = new java.util.ArrayList<>();

        for (int i = 0; i < generalClubIds.size(); i++) {
            final String clubId = generalClubIds.get(i);
            final String clubName = (generalClubNames != null && i < generalClubNames.size()) ?
                    generalClubNames.get(i) : "동아리";

            firebaseManager.getDb().collection("clubs")
                    .document(clubId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isCentralClub = documentSnapshot.getBoolean("centralClub");
                            String actualClubName = documentSnapshot.getString("name");
                            String displayName = actualClubName != null ? actualClubName : clubName;

                            if (isCentralClub != null && isCentralClub) {
                                // 실제로 중앙동아리인 경우
                                centralClubs.add(new String[]{clubId, displayName});

                                // 사용자 문서 업데이트 (generalClubIds에서 제거, centralClubId로 이동)
                                updateUserMembershipToCentral(clubId, displayName);
                            } else {
                                // 일반동아리
                                generalClubs.add(new String[]{clubId, displayName});
                            }
                        } else {
                            // 문서가 없으면 일반동아리로 표시
                            generalClubs.add(new String[]{clubId, clubName});
                        }

                        // 모든 체크가 완료되면 UI 업데이트
                        if (pendingChecks.decrementAndGet() == 0) {
                            runOnUiThread(() -> displayClubLists(generalClubs, centralClubs, hasCentralClub));
                        }
                    })
                    .addOnFailureListener(e -> {
                        // 에러 시 일반동아리로 표시
                        generalClubs.add(new String[]{clubId, clubName});

                        if (pendingChecks.decrementAndGet() == 0) {
                            runOnUiThread(() -> displayClubLists(generalClubs, centralClubs, hasCentralClub));
                        }
                    });
        }
    }

    private void displayClubLists(java.util.List<String[]> generalClubs, java.util.List<String[]> centralClubs, boolean hasCentralClubAlready) {
        // 중앙동아리가 발견되었고 아직 표시되지 않은 경우
        if (!hasCentralClubAlready && !centralClubs.isEmpty()) {
            String[] firstCentralClub = centralClubs.get(0);
            llCentralClubSection.setVisibility(View.VISIBLE);
            tvCentralClubName.setText(firstCentralClub[1]);

            // 클릭 리스너
            tvCentralClubName.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, ClubMainActivity.class);
                intent.putExtra("clubId", firstCentralClub[0]);
                intent.putExtra("club_name", firstCentralClub[1]);
                intent.putExtra("isCentralClub", true);
                startActivity(intent);
            });
        }

        // 일반동아리 표시
        if (!generalClubs.isEmpty()) {
            llGeneralClubsSection.setVisibility(View.VISIBLE);
            for (String[] club : generalClubs) {
                addGeneralClubItem(club[0], club[1]);
            }
        } else {
            llGeneralClubsSection.setVisibility(View.GONE);
        }
    }

    private void updateUserMembershipToCentral(String clubId, String clubName) {
        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) return;

        // 사용자 문서 업데이트 - generalClubIds에서 제거하고 centralClubId로 설정
        firebaseManager.getDb().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        java.util.List<String> generalIds = (java.util.List<String>) userDoc.get("generalClubIds");
                        java.util.List<String> generalNames = (java.util.List<String>) userDoc.get("generalClubNames");

                        if (generalIds != null && generalIds.contains(clubId)) {
                            // generalClubIds에서 제거
                            java.util.List<String> newIds = new java.util.ArrayList<>(generalIds);
                            newIds.remove(clubId);

                            java.util.List<String> newNames = new java.util.ArrayList<>();
                            if (generalNames != null) {
                                int idx = generalIds.indexOf(clubId);
                                for (int i = 0; i < generalNames.size(); i++) {
                                    if (i != idx) {
                                        newNames.add(generalNames.get(i));
                                    }
                                }
                            }

                            // 업데이트
                            java.util.Map<String, Object> updates = new java.util.HashMap<>();
                            updates.put("generalClubIds", newIds);
                            updates.put("generalClubNames", newNames);
                            updates.put("centralClubId", clubId);
                            updates.put("centralClubName", clubName);

                            firebaseManager.getDb().collection("users")
                                    .document(userId)
                                    .update(updates);
                        }
                    }
                });
    }

    private void addGeneralClubItem(String clubId, String clubName) {
        TextView clubTextView = new TextView(this);
        clubTextView.setText(clubName != null ? clubName : "일반동아리");
        clubTextView.setTextSize(16);
        clubTextView.setTextColor(getResources().getColor(android.R.color.black));
        clubTextView.setPadding(
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density)
        );
        clubTextView.setBackgroundColor(0xFFE3F2FD); // Light blue background

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        clubTextView.setLayoutParams(params);

        clubTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ClubMainActivity.class);
            intent.putExtra("clubId", clubId);
            intent.putExtra("club_name", clubName);
            intent.putExtra("isCentralClub", false);
            startActivity(intent);
        });

        llGeneralClubsList.addView(clubTextView);
    }

    private void addClubItem(String clubId, String clubName, boolean isCentralClub) {
        View clubItemView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, llMyClubsList, false);

        TextView tvText1 = clubItemView.findViewById(android.R.id.text1);
        TextView tvText2 = clubItemView.findViewById(android.R.id.text2);

        tvText1.setText(clubName != null ? clubName : "동아리");
        tvText1.setTextColor(getResources().getColor(android.R.color.black));
        tvText2.setText(isCentralClub ? "중앙동아리" : "일반동아리");
        tvText2.setTextColor(isCentralClub ? getResources().getColor(android.R.color.holo_purple) :
                getResources().getColor(android.R.color.holo_blue_dark));

        clubItemView.setPadding(0, 8, 0, 8);
        clubItemView.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ClubMainActivity.class);
            intent.putExtra("clubId", clubId);
            intent.putExtra("club_name", clubName);
            intent.putExtra("isCentralClub", isCentralClub);
            startActivity(intent);
        });

        llMyClubsList.addView(clubItemView);
    }

    private void updateNavigationForSuperAdmin() {
        if (bottomNavigation != null) {
            boolean isSuperAdmin = isSuperAdminMode(this);
            bottomNavigation.getMenu().findItem(R.id.nav_recommend).setVisible(!isSuperAdmin);
        }
    }

    private void loadUserInfo() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            tvUserEmail.setText(user.getEmail());

            // Load full user info from Firestore
            firebaseManager.getCurrentUser(new FirebaseManager.UserCallback() {
                @Override
                public void onSuccess(User loadedUser) {
                    if (loadedUser != null) {
                        currentUser = loadedUser;
                        updateAccountInfoDisplay();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // Keep showing just email
                }
            });
        } else {
            tvUserEmail.setText("로그인되지 않음");
        }
    }

    private void updateAccountInfoDisplay() {
        if (currentUser == null) return;

        // Show name if available
        if (currentUser.getName() != null && !currentUser.getName().isEmpty()) {
            tvUserName.setText("이름: " + currentUser.getName());
            tvUserName.setVisibility(View.VISIBLE);
        } else {
            tvUserName.setVisibility(View.GONE);
        }

        // Show department if available
        if (currentUser.getDepartment() != null && !currentUser.getDepartment().isEmpty()) {
            tvUserDepartment.setText("학과: " + currentUser.getDepartment());
            tvUserDepartment.setVisibility(View.VISIBLE);
        } else {
            tvUserDepartment.setVisibility(View.GONE);
        }

        // Show phone if available
        if (currentUser.getPhone() != null && !currentUser.getPhone().isEmpty()) {
            tvUserPhone.setText("전화번호: " + currentUser.getPhone());
            tvUserPhone.setVisibility(View.VISIBLE);
        } else {
            tvUserPhone.setVisibility(View.GONE);
        }
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);

        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        TextInputEditText etStudentId = dialogView.findViewById(R.id.etStudentId);
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etDepartment = dialogView.findViewById(R.id.etDepartment);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etPhone);
        android.widget.ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);

        // 현재 값 설정
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            etEmail.setText(firebaseUser.getEmail());
        }

        if (currentUser != null) {
            etStudentId.setText(currentUser.getStudentId() != null ? currentUser.getStudentId() : "");
            etName.setText(currentUser.getName() != null ? currentUser.getName() : "");
            etDepartment.setText(currentUser.getDepartment() != null ? currentUser.getDepartment() : "");
            etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();

        dialog.show();

        // 저장 버튼 커스텀 처리
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String department = etDepartment.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            // 유효성 검사
            if (name.isEmpty()) {
                Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

            // Firebase 업데이트
            firebaseManager.updateUserProfile(name, department, phone, new FirebaseManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                    dialog.dismiss();

                    // 로컬 사용자 정보 업데이트
                    if (currentUser != null) {
                        currentUser.setName(name);
                        currentUser.setDepartment(department);
                        currentUser.setPhone(phone);
                        updateAccountInfoDisplay();
                    }

                    Toast.makeText(SettingsActivity.this, "프로필이 수정되었습니다", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                    Toast.makeText(SettingsActivity.this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showGlobalNoticeDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_global_notice, null);

        TextInputEditText etTitle = dialogView.findViewById(R.id.etNoticeTitle);
        TextInputEditText etContent = dialogView.findViewById(R.id.etNoticeContent);
        android.widget.ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView tvProgress = dialogView.findViewById(R.id.tvProgress);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("전송", null)
                .setNegativeButton("취소", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (content.isEmpty()) {
                Toast.makeText(this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 확인 다이얼로그
            new AlertDialog.Builder(this)
                    .setTitle("전체 공지 확인")
                    .setMessage("모든 동아리에 공지를 전송하시겠습니까?\n\n제목: " + title)
                    .setPositiveButton("전송", (d, w) -> {
                        progressBar.setVisibility(View.VISIBLE);
                        tvProgress.setVisibility(View.VISIBLE);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

                        String authorId = firebaseManager.getCurrentUserId();
                        String authorName = "관리자";

                        firebaseManager.sendGlobalNotice(title, content, authorId, authorName,
                                new FirebaseManager.GlobalNoticeCallback() {
                                    @Override
                                    public void onProgress(int current, int total) {
                                        runOnUiThread(() -> {
                                            tvProgress.setText("전송 중... " + current + "/" + total);
                                        });
                                    }

                                    @Override
                                    public void onSuccess(int totalSent) {
                                        runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            tvProgress.setVisibility(View.GONE);
                                            dialog.dismiss();
                                            Toast.makeText(SettingsActivity.this,
                                                    totalSent + "개 동아리에 공지가 전송되었습니다",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            tvProgress.setVisibility(View.GONE);
                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                                            Toast.makeText(SettingsActivity.this,
                                                    "전송 실패: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });
    }

    private void setupListeners() {
        // 계정 정보 카드 클릭 (프로필 수정)
        cardAccountInfo.setOnClickListener(v -> showEditProfileDialog());

        // 중앙동아리 신청 명단 버튼
        btnCentralClubApplications.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, CentralApplicationsActivity.class);
            startActivity(intent);
        });

        // 전체 공지 버튼
        btnGlobalNotice.setOnClickListener(v -> showGlobalNoticeDialog());

        // 동아리 설립 신청 버튼
        btnClubEstablish.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ClubEstablishActivity.class);
            startActivity(intent);
        });

        // 일반동아리 설립 승인 버튼 (최고 관리자용)
        btnClubApproval.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ClubApprovalListActivity.class);
            startActivity(intent);
        });

        // 동아리 삭제 버튼 (최고 관리자용)
        btnClubDelete.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ClubDeleteActivity.class);
            startActivity(intent);
        });

        // 동아리 강등 버튼 (최고 관리자용)
        btnClubDemote.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, DemoteCentralClubActivity.class);
            startActivity(intent);
        });

        // 인원 제한 수정 버튼 (최고 관리자용)
        btnEditMemberLimit.setOnClickListener(v -> showEditMemberLimitDialog());

        // 전체 배너 추가 버튼 (최고 관리자용)
        btnAddGlobalBanner.setOnClickListener(v -> showAddGlobalBannerDialog());

        // 전체 배너 초기화 버튼 (최고 관리자용)
        btnClearGlobalBanners.setOnClickListener(v -> showClearGlobalBannersDialog());

        // 테스트 데이터 생성 버튼 (최고 관리자용)
        btnInjectTestData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("테스트 데이터 생성")
                    .setMessage("테스트용 동아리(라이트 하우스)와 22명의 부원, 캐러셀 아이템을 생성합니다.\n\n계속하시겠습니까?")
                    .setPositiveButton("생성", (dialog, which) -> {
                        TestDataInjector.injectTestData(this);
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        // 테스트 데이터 삭제 버튼 (최고 관리자용)
        btnDeleteTestData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("테스트 데이터 삭제")
                    .setMessage("테스트용 동아리 데이터를 삭제합니다.\n\n이 작업은 되돌릴 수 없습니다.")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        TestDataInjector.deleteTestData(this);
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        // 로그아웃 버튼
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("로그아웃")
                    .setMessage("로그아웃 하시겠습니까?")
                    .setPositiveButton("로그아웃", (dialog, which) -> {
                        // Firebase 로그아웃
                        firebaseAuth.signOut();
                        // 최고 관리자 모드 해제
                        setSuperAdminModeStatic(this, false);
                        // 자동 로그인 정보 삭제
                        clearAutoLoginInfo();
                        // 테마 설정 초기화 (로그아웃 시 기본값으로)
                        ThemeHelper.clearLocalTheme(this);

                        Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();

                        // 로그인 화면으로 이동
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        // 하단 네비게이션 클릭
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                Intent intent;
                // 관리자 모드인지 확인하여 적절한 화면으로 이동
                if (isSuperAdminMode(this)) {
                    intent = new Intent(this, AdminMainActivity.class);
                } else {
                    intent = new Intent(this, MainActivityNew.class);
                }
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_chat) {
                Intent intent = new Intent(this, ChatActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_clubs) {
                Intent intent = new Intent(this, ClubListActivity.class);
                intent.putExtra("from_club_settings", false);  // 네비게이션 바 표시
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_recommend) {
                Intent intent = new Intent(this, ClubRecommendActivity.class);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_myinfo) {
                // 현재 화면이므로 아무것도 하지 않음
                return true;
            }

            return false;
        });
    }

    private void clearAutoLoginInfo() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("auto_login", false)
                .remove("saved_email")
                .remove("saved_password")
                .apply();
    }

    public static boolean isSuperAdminMode(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_SUPER_ADMIN_MODE, false);
    }

    public static void setSuperAdminModeStatic(android.content.Context context, boolean active) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SUPER_ADMIN_MODE, active).apply();
    }

    private void showEditMemberLimitDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_member_limit, null);

        TextView tvCurrentRegister = dialogView.findViewById(R.id.tvCurrentRegister);
        TextView tvCurrentMaintain = dialogView.findViewById(R.id.tvCurrentMaintain);
        TextInputEditText etRegisterLimit = dialogView.findViewById(R.id.etRegisterLimit);
        TextInputEditText etMaintainLimit = dialogView.findViewById(R.id.etMaintainLimit);

        // 현재 값 표시
        tvCurrentRegister.setText(currentRegisterLimit + "명");
        tvCurrentMaintain.setText(currentMaintainLimit + "명");

        // 현재 값을 입력 필드에 기본값으로 설정
        etRegisterLimit.setText(String.valueOf(currentRegisterLimit));
        etMaintainLimit.setText(String.valueOf(currentMaintainLimit));

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("확인", (dialog, which) -> {
                    String registerStr = etRegisterLimit.getText().toString().trim();
                    String maintainStr = etMaintainLimit.getText().toString().trim();

                    if (registerStr.isEmpty() || maintainStr.isEmpty()) {
                        Toast.makeText(this, "모든 값을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int newRegister = Integer.parseInt(registerStr);
                        int newMaintain = Integer.parseInt(maintainStr);

                        // 유효성 검사
                        if (newRegister < 1 || newMaintain < 1) {
                            Toast.makeText(this, "인원 수는 1 이상이어야 합니다", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (newMaintain > newRegister) {
                            Toast.makeText(this, "유지 인원은 등록 인원보다 클 수 없습니다", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 확인 다이얼로그 표시
                        showConfirmMemberLimitDialog(newRegister, newMaintain);

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "올바른 숫자를 입력해주세요", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showConfirmMemberLimitDialog(int newRegister, int newMaintain) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_member_limit, null);

        TextView tvOldRegister = dialogView.findViewById(R.id.tvOldRegister);
        TextView tvNewRegister = dialogView.findViewById(R.id.tvNewRegister);
        TextView tvOldMaintain = dialogView.findViewById(R.id.tvOldMaintain);
        TextView tvNewMaintain = dialogView.findViewById(R.id.tvNewMaintain);
        CheckBox cbConfirm = dialogView.findViewById(R.id.cbConfirm);

        tvOldRegister.setText(currentRegisterLimit + "명");
        tvNewRegister.setText(newRegister + "명");
        tvOldMaintain.setText(currentMaintainLimit + "명");
        tvNewMaintain.setText(newMaintain + "명");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("변경", null)
                .setNegativeButton("취소", null)
                .create();

        dialog.show();

        // 확인 버튼 커스텀 처리 (체크박스 체크 필요)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!cbConfirm.isChecked()) {
                Toast.makeText(this, "변경에 동의하려면 체크박스를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            saveMemberLimits(newRegister, newMaintain);
        });
    }

    private void saveMemberLimits(int registerLimit, int maintainLimit) {
        firebaseManager.saveMemberLimits(registerLimit, maintainLimit, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                currentRegisterLimit = registerLimit;
                currentMaintainLimit = maintainLimit;
                updateMemberLimitDisplay();
                Toast.makeText(SettingsActivity.this, "인원 제한이 변경되었습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SettingsActivity.this, "변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================
    // 전체 배너 관리 메서드
    // ========================================

    private void initBannerImagePicker() {
        bannerImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedBannerImageUri = uri;
                        if (currentBannerPreview != null && currentSelectImageLayout != null) {
                            currentBannerPreview.setImageURI(uri);
                            currentBannerPreview.setVisibility(View.VISIBLE);
                            currentSelectImageLayout.setVisibility(View.GONE);
                        }
                    }
                }
        );
    }

    private void showAddGlobalBannerDialog() {
        // 이미지 선택 초기화
        selectedBannerImageUri = null;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_global_banner, null);

        TextInputEditText etBannerTitle = dialogView.findViewById(R.id.etBannerTitle);
        TextInputEditText etBannerContent = dialogView.findViewById(R.id.etBannerContent);
        TextInputEditText etBannerLink = dialogView.findViewById(R.id.etBannerLink);
        android.widget.ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        ImageView ivBannerPreview = dialogView.findViewById(R.id.ivBannerPreview);
        LinearLayout layoutSelectImage = dialogView.findViewById(R.id.layoutSelectImage);
        FrameLayout imageContainer = (FrameLayout) ivBannerPreview.getParent();

        // 현재 미리보기 참조 저장
        currentBannerPreview = ivBannerPreview;
        currentSelectImageLayout = layoutSelectImage;

        // 이미지 선택 영역 클릭 리스너
        imageContainer.setOnClickListener(v -> {
            bannerImagePickerLauncher.launch("image/*");
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("추가", null)
                .setNegativeButton("취소", null)
                .create();

        dialog.show();

        // 추가 버튼 클릭 처리
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = etBannerTitle.getText().toString().trim();
            String content = etBannerContent.getText().toString().trim();
            String linkUrl = etBannerLink.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(this, "배너 제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (content.isEmpty()) {
                Toast.makeText(this, "배너 내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);

            // 이미지가 선택된 경우 먼저 업로드
            if (selectedBannerImageUri != null) {
                uploadBannerImageAndAdd(title, content, linkUrl, progressBar, dialog);
            } else {
                // 이미지 없이 바로 배너 추가
                addBannerToAllClubs(title, content, null, linkUrl, progressBar, dialog);
            }
        });
    }

    private void uploadBannerImageAndAdd(String title, String content, String linkUrl,
                                          android.widget.ProgressBar progressBar, AlertDialog dialog) {
        try {
            // 이미지를 바이트 배열로 변환
            InputStream inputStream = getContentResolver().openInputStream(selectedBannerImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            // 이미지 크기 조정 (최대 1024px)
            int maxSize = 1024;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = Math.min((float) maxSize / width, (float) maxSize / height);
            if (scale < 1) {
                width = Math.round(width * scale);
                height = Math.round(height * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            // JPEG로 압축
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();

            // Firebase Storage에 업로드
            firebaseManager.uploadBannerImage(imageData, new FirebaseManager.SignatureCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    // 이미지 업로드 성공 후 배너 추가
                    addBannerToAllClubs(title, content, imageUrl, linkUrl, progressBar, dialog);
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                    Toast.makeText(SettingsActivity.this, "이미지 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
            Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addBannerToAllClubs(String title, String content, String imageUrl, String linkUrl,
                                      android.widget.ProgressBar progressBar, AlertDialog dialog) {
        firebaseManager.addBannerToAllClubs(title, content, imageUrl, linkUrl, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                dialog.dismiss();
                selectedBannerImageUri = null;
                currentBannerPreview = null;
                currentSelectImageLayout = null;
                Toast.makeText(SettingsActivity.this, "모든 동아리에 배너가 추가되었습니다", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                Toast.makeText(SettingsActivity.this, "배너 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showClearGlobalBannersDialog() {
        new AlertDialog.Builder(this)
                .setTitle("전체 배너 초기화")
                .setMessage("모든 동아리에 추가된 전체 배너를 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.")
                .setPositiveButton("초기화", (dialog, which) -> {
                    Toast.makeText(this, "전체 배너 초기화 중...", Toast.LENGTH_SHORT).show();

                    firebaseManager.clearGlobalBannersFromAllClubs(new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(SettingsActivity.this, "모든 동아리의 전체 배너가 초기화되었습니다", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(SettingsActivity.this, "초기화 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
