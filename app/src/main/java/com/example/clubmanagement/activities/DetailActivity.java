package com.example.clubmanagement.activities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.adapters.DetailImageAdapter;
import com.example.clubmanagement.models.CarouselItem;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;

import androidx.cardview.widget.CardView;

import java.util.Date;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.viewpager2.widget.ViewPager2;

public class DetailActivity extends BaseActivity {

    private ImageView ivBack;
    private ImageView ivSuperAdminSettings;
    private TextView tvDetailTitle;
    private TextView tvDetailDescription;
    private LinearLayout llFeatureList;
    private MaterialButton btnAction;
    private MaterialButton btnAdminManage;
    private FloatingActionButton fabEdit;
    private ProgressBar progressBar;
    private boolean isSuperAdminMode = false;

    // Club Info UI
    private LinearLayout llClubInfo;
    private LinearLayout llProfessor;
    private LinearLayout llDepartment;
    private LinearLayout llLocation;
    private LinearLayout llFoundedAt;
    private LinearLayout llMemberCount;
    private LinearLayout llPurpose;
    private LinearLayout llSchedule;
    private TextView tvInfoProfessor;
    private TextView tvInfoDepartment;
    private TextView tvInfoLocation;
    private TextView tvInfoFoundedAt;
    private TextView tvInfoMemberCount;
    private TextView tvInfoPurpose;
    private TextView tvInfoSchedule;

    // ViewPager2 for image carousel
    private ViewPager2 vpDetailImages;
    private DetailImageAdapter detailImageAdapter;
    private LinearLayout llIndicator;

    private int pageIndex;
    private boolean isAdmin = false;
    private FirebaseManager firebaseManager;
    private CarouselItem currentItem;
    private String clubName;
    private boolean fromClubList = false;

    // 사용자의 중앙동아리 가입 정보
    private String userCentralClubId = null;
    private String userCentralClubName = null;
    private java.util.List<String> userGeneralClubIds = null;  // 사용자의 일반동아리 목록
    private boolean isMyClub = false;  // 현재 보고 있는 동아리가 내 동아리인지

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Intent에서 데이터 받기
        pageIndex = getIntent().getIntExtra("page_index", 0);
        clubName = getIntent().getStringExtra("club_name");
        fromClubList = getIntent().getBooleanExtra("from_club_list", false);

        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupImagePickerLauncher();
        checkAdminStatus();
        checkSuperAdminMode();

        // 사용자의 중앙동아리 가입 정보 확인 후 데이터 로드
        checkUserCentralClubMembership();

        setupListeners();
    }

    private void checkUserCentralClubMembership() {
        // 최고 관리자 모드면 바로 데이터 로드
        if (isSuperAdminMode) {
            loadContentData();
            return;
        }

        // 로그인 안 된 경우 바로 데이터 로드
        if (firebaseManager.getCurrentUserId() == null) {
            loadContentData();
            return;
        }

        // 사용자의 중앙동아리 가입 정보 확인
        firebaseManager.getCurrentUser(new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.User user) {
                if (user != null) {
                    if (user.hasJoinedCentralClub()) {
                        userCentralClubId = user.getCentralClubId();
                        userCentralClubName = user.getCentralClubName();
                    }
                    // 일반동아리 목록도 저장 (중앙동아리로 전환된 경우 확인용)
                    userGeneralClubIds = user.getGeneralClubIds();
                }
                loadContentData();
            }

            @Override
            public void onFailure(Exception e) {
                loadContentData();
            }
        });
    }

    private void loadContentData() {
        if (fromClubList && clubName != null) {
            // ClubListActivity에서 온 경우
            setupClubListContent();
        } else {
            // 메인 화면 캐러셀에서 온 경우
            loadCarouselData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSuperAdminMode();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        ivSuperAdminSettings = findViewById(R.id.ivSuperAdminSettings);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        llFeatureList = findViewById(R.id.llFeatureList);
        btnAction = findViewById(R.id.btnAction);
        btnAdminManage = findViewById(R.id.btnAdminManage);
        fabEdit = findViewById(R.id.fabEdit);
        progressBar = findViewById(R.id.progressBar);

        // ViewPager2 for image carousel
        vpDetailImages = findViewById(R.id.vpDetailImages);
        llIndicator = findViewById(R.id.llIndicator);

        // Club Info UI
        llClubInfo = findViewById(R.id.llClubInfo);
        llProfessor = findViewById(R.id.llProfessor);
        llDepartment = findViewById(R.id.llDepartment);
        llLocation = findViewById(R.id.llLocation);
        llFoundedAt = findViewById(R.id.llFoundedAt);
        llMemberCount = findViewById(R.id.llMemberCount);
        llPurpose = findViewById(R.id.llPurpose);
        llSchedule = findViewById(R.id.llSchedule);
        tvInfoProfessor = findViewById(R.id.tvInfoProfessor);
        tvInfoDepartment = findViewById(R.id.tvInfoDepartment);
        tvInfoLocation = findViewById(R.id.tvInfoLocation);
        tvInfoFoundedAt = findViewById(R.id.tvInfoFoundedAt);
        tvInfoMemberCount = findViewById(R.id.tvInfoMemberCount);
        tvInfoPurpose = findViewById(R.id.tvInfoPurpose);
        tvInfoSchedule = findViewById(R.id.tvInfoSchedule);

        // Initially hide buttons
        fabEdit.setVisibility(View.GONE);
        ivSuperAdminSettings.setVisibility(View.GONE);
        btnAdminManage.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        // Setup default ViewPager2 adapter to prevent crash
        setupDefaultImages();
    }

    private void checkSuperAdminMode() {
        // 최고 관리자 모드인지 확인
        isSuperAdminMode = com.example.clubmanagement.SettingsActivity.isSuperAdminMode(this);

        if (isSuperAdminMode) {
            // 최고 관리자 모드일 때 동아리 관리 버튼 표시, 가입 신청 버튼 숨김
            btnAdminManage.setVisibility(View.VISIBLE);
            btnAction.setVisibility(View.GONE);

            // 캐러셀에서 온 경우에만 설정 버튼 표시
            if (!fromClubList) {
                ivSuperAdminSettings.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupDefaultImages() {
        List<Object> defaultImages = new ArrayList<>();
        // 기본 배경색 사용
        defaultImages.add(0xFF6200EA); // 보라색
        detailImageAdapter = new DetailImageAdapter(defaultImages);
        vpDetailImages.setAdapter(detailImageAdapter);
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadImage(imageUri);
                        }
                    }
                }
        );
    }

    private void checkAdminStatus() {
        firebaseManager.isCurrentUserAdmin(new FirebaseManager.AdminCheckCallback() {
            @Override
            public void onResult(boolean admin) {
                isAdmin = admin;
                if (isAdmin) {
                    fabEdit.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Not admin or error
                isAdmin = false;
            }
        });
    }

    private void loadCarouselData() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getCarouselItemByPosition(pageIndex, new FirebaseManager.CarouselCallback() {
            @Override
            public void onSuccess(CarouselItem item) {
                progressBar.setVisibility(View.GONE);
                currentItem = item;

                if (item != null) {
                    // Load from Firebase
                    displayCarouselItem(item);
                } else {
                    // Load default content
                    setupDefaultContent();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DetailActivity.this, "데이터 로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                setupDefaultContent();
            }
        });
    }

    private void displayCarouselItem(CarouselItem item) {
        // Set title
        tvDetailTitle.setText(item.getTitle());

        // Set description
        tvDetailDescription.setText(item.getDescription());

        // Load images from CarouselItem
        loadCarouselImages(item);

        // clubId로 버튼 상태 결정
        String clubId = item.getClubId();
        android.util.Log.d("DetailActivity", "displayCarouselItem - clubId: " + clubId);

        // 동아리 정보 로드 및 표시
        loadAndDisplayClubInfo(clubId);

        // 사용자의 중앙동아리 가입 상태에 따라 버튼 설정
        updateButtonForCentralClub(clubId);
    }

    private void loadCarouselImages(CarouselItem item) {
        List<Object> images = new ArrayList<>();

        // 상세보기 이미지 (imageUrls - 최대 3장)
        List<String> detailUrls = item.getImageUrls();
        if (detailUrls != null && !detailUrls.isEmpty()) {
            images.addAll(detailUrls);
        } else if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            // 상세보기 이미지가 없으면 포스터 이미지를 대신 표시
            images.add(item.getImageUrl());
        } else if (item.getImageRes() != 0) {
            // Use drawable resource for backward compatibility
            images.add(item.getImageRes());
        } else {
            // Default color background
            images.add(0xFF6200EA);
        }

        detailImageAdapter.updateImages(images);
        setupImageIndicators(images.size());
    }

    private void setupImageIndicators(int count) {
        llIndicator.removeAllViews();
        if (count <= 1) {
            llIndicator.setVisibility(View.GONE);
            return;
        }

        llIndicator.setVisibility(View.VISIBLE);
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);

            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(i == 0 ? Color.WHITE : Color.parseColor("#80FFFFFF"));
            dot.setBackground(drawable);

            llIndicator.addView(dot);
        }

        // Add page change callback for updating indicators
        vpDetailImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateImageIndicators(position);
            }
        });
    }

    private void updateImageIndicators(int selectedPosition) {
        for (int i = 0; i < llIndicator.getChildCount(); i++) {
            View dot = llIndicator.getChildAt(i);
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(i == selectedPosition ? Color.WHITE : Color.parseColor("#80FFFFFF"));
            dot.setBackground(drawable);
        }
    }

    private void loadAndDisplayClubInfo(String clubId) {
        if (clubId == null || clubId.isEmpty()) {
            llClubInfo.setVisibility(View.GONE);
            return;
        }

        firebaseManager.getClub(clubId, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(com.example.clubmanagement.models.Club club) {
                if (club != null) {
                    displayClubInfoSection(club);
                } else {
                    llClubInfo.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                llClubInfo.setVisibility(View.GONE);
            }
        });
    }

    private void displayClubInfoSection(com.example.clubmanagement.models.Club club) {
        boolean hasAnyInfo = false;

        // 지도교수
        String professor = club.getProfessor();
        if (professor != null && !professor.isEmpty()) {
            tvInfoProfessor.setText(professor);
            llProfessor.setVisibility(View.VISIBLE);
            hasAnyInfo = true;
        } else {
            llProfessor.setVisibility(View.GONE);
        }

        // 학과
        String department = club.getDepartment();
        if (department != null && !department.isEmpty()) {
            tvInfoDepartment.setText(department);
            llDepartment.setVisibility(View.VISIBLE);
            hasAnyInfo = true;
        } else {
            llDepartment.setVisibility(View.GONE);
        }

        // 동아리방 위치
        String location = club.getLocation();
        if (location != null && !location.isEmpty()) {
            tvInfoLocation.setText(location);
            llLocation.setVisibility(View.VISIBLE);
            hasAnyInfo = true;
        } else {
            llLocation.setVisibility(View.GONE);
        }

        // 설립일
        Timestamp foundedAt = club.getFoundedAt();
        if (foundedAt != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy년 MM월 dd일", java.util.Locale.KOREA);
            tvInfoFoundedAt.setText(sdf.format(foundedAt.toDate()));
            llFoundedAt.setVisibility(View.VISIBLE);
            hasAnyInfo = true;
        } else {
            llFoundedAt.setVisibility(View.GONE);
        }

        // 인원 수
        int memberCount = club.getMemberCount();
        if (memberCount > 0) {
            tvInfoMemberCount.setText(memberCount + "명");
            llMemberCount.setVisibility(View.VISIBLE);
            hasAnyInfo = true;
        } else {
            llMemberCount.setVisibility(View.GONE);
        }

        // 설립 목적
        String purpose = club.getPurpose();
        if (purpose != null && !purpose.isEmpty()) {
            tvInfoPurpose.setText(purpose);
            llPurpose.setVisibility(View.VISIBLE);
            hasAnyInfo = true;
        } else {
            llPurpose.setVisibility(View.GONE);
        }

        // 행사 일정 (월별 일정 우선, 없으면 일반 일정)
        String schedule = club.getMonthlyScheduleAsString();
        if (schedule == null || schedule.isEmpty()) {
            schedule = club.getSchedule();
        }
        if (schedule != null && !schedule.isEmpty()) {
            tvInfoSchedule.setText(schedule);
            llSchedule.setVisibility(View.VISIBLE);
            hasAnyInfo = true;
        } else {
            llSchedule.setVisibility(View.GONE);
        }

        // 정보가 하나라도 있으면 섹션 표시
        llClubInfo.setVisibility(hasAnyInfo ? View.VISIBLE : View.GONE);
    }

    private void updateButtonForCentralClub(String clubId) {
        // 최고 관리자 모드면 버튼 숨김
        if (isSuperAdminMode) {
            btnAction.setVisibility(View.GONE);
            return;
        }

        // clubId가 없으면 버튼 숨김
        if (clubId == null || clubId.isEmpty()) {
            btnAction.setVisibility(View.GONE);
            return;
        }

        // 직접 members 컬렉션에서 멤버십 확인 (가장 확실한 방법)
        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) {
            // 로그인 안 됨 - 가입 신청 버튼
            btnAction.setText("가입 신청하기");
            btnAction.setVisibility(View.VISIBLE);
            return;
        }

        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .get()
                .addOnSuccessListener(memberDoc -> {
                    if (memberDoc.exists()) {
                        // 이 동아리의 멤버임 - "내 동아리로 가기"
                        isMyClub = true;
                        userCentralClubId = clubId;
                        btnAction.setText("내 동아리로 가기");
                        btnAction.setVisibility(View.VISIBLE);
                    } else {
                        // 멤버 아님 - 다른 중앙동아리 가입 여부 확인
                        if (userCentralClubId != null && !userCentralClubId.isEmpty()) {
                            // 이미 다른 중앙동아리에 가입됨 - 버튼 숨김
                            isMyClub = false;
                            btnAction.setVisibility(View.GONE);
                        } else {
                            // 미가입 - 가입 신청 버튼
                            isMyClub = false;
                            btnAction.setText("가입 신청하기");
                            btnAction.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // 에러 시 가입 신청 버튼 표시
                    isMyClub = false;
                    btnAction.setText("가입 신청하기");
                    btnAction.setVisibility(View.VISIBLE);
                });
    }

    private void setAsMyClub(String clubId, String clubNameToSet) {
        isMyClub = true;
        userCentralClubId = clubId;
        if (clubNameToSet != null) {
            userCentralClubName = clubNameToSet;
        }
        btnAction.setText("내 동아리로 가기");
        btnAction.setVisibility(View.VISIBLE);
    }

    private void checkIfClubIsCentralAndSetButton(String clubId) {
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isCentralClub = documentSnapshot.getBoolean("centralClub");
                        String clubNameFromDoc = documentSnapshot.getString("name");

                        if (isCentralClub != null && isCentralClub) {
                            // 실제로 중앙동아리 - 내 동아리로 가기 버튼 표시
                            android.util.Log.d("DetailActivity", "Club is central club, showing 내 동아리로 가기 button");
                            setAsMyClub(clubId, clubNameFromDoc);

                            // 사용자 문서도 업데이트
                            updateUserMembershipToCentral(clubId, clubNameFromDoc);
                        } else {
                            // 일반동아리 - 가입 신청 버튼 (이미 가입됨이므로 숨김)
                            isMyClub = false;
                            btnAction.setVisibility(View.GONE);
                        }
                    } else {
                        isMyClub = false;
                        btnAction.setText("가입 신청하기");
                        btnAction.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    isMyClub = false;
                    btnAction.setText("가입 신청하기");
                    btnAction.setVisibility(View.VISIBLE);
                });
    }

    private void updateUserMembershipToCentral(String clubId, String clubNameToUpdate) {
        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) return;

        firebaseManager.getDb().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        java.util.List<String> generalIds = (java.util.List<String>) userDoc.get("generalClubIds");
                        java.util.List<String> generalNames = (java.util.List<String>) userDoc.get("generalClubNames");

                        if (generalIds != null && generalIds.contains(clubId)) {
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

                            java.util.Map<String, Object> updates = new java.util.HashMap<>();
                            updates.put("generalClubIds", newIds);
                            updates.put("generalClubNames", newNames);
                            updates.put("centralClubId", clubId);
                            updates.put("centralClubName", clubNameToUpdate);

                            firebaseManager.getDb().collection("users")
                                    .document(userId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        android.util.Log.d("DetailActivity", "User membership updated to central club");
                                    });
                        }
                    }
                });
    }

    private void loadApplicationOpenSetting(String clubId) {
        android.util.Log.d("DetailActivity", "loadApplicationOpenSetting - clubId: " + clubId);

        // 최고 관리자 모드면 버튼 숨김
        if (isSuperAdminMode) {
            android.util.Log.d("DetailActivity", "Super admin mode - hiding button");
            btnAction.setVisibility(View.GONE);
            return;
        }

        // 먼저 사용자가 이미 이 동아리의 멤버인지 확인
        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) {
            // 로그인 안 된 경우 - 가입 신청 버튼 표시 (applicationOpen 확인)
            checkApplicationOpenAndShowButton(clubId);
            return;
        }

        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .collection("members")
                .document(userId)
                .get()
                .addOnSuccessListener(memberDoc -> {
                    if (memberDoc.exists()) {
                        // 이미 멤버임 - "내 동아리로 가기" 버튼 표시
                        android.util.Log.d("DetailActivity", "User is already a member of this club");
                        String clubNameFromMember = memberDoc.getString("clubName");
                        setAsMyClub(clubId, clubNameFromMember != null ? clubNameFromMember : clubName);
                    } else {
                        // 멤버가 아님 - applicationOpen 확인
                        checkApplicationOpenAndShowButton(clubId);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DetailActivity", "Member check error: " + e.getMessage());
                    // 에러 시 applicationOpen 확인
                    checkApplicationOpenAndShowButton(clubId);
                });
    }

    private void checkApplicationOpenAndShowButton(String clubId) {
        firebaseManager.getDb().collection("clubs")
                .document(clubId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    android.util.Log.d("DetailActivity", "checkApplicationOpenAndShowButton - exists: " + documentSnapshot.exists());
                    if (documentSnapshot.exists()) {
                        Boolean applicationOpen = documentSnapshot.getBoolean("applicationOpen");
                        android.util.Log.d("DetailActivity", "applicationOpen: " + applicationOpen);
                        // applicationOpen이 명시적으로 false인 경우에만 숨김
                        if (applicationOpen != null && !applicationOpen) {
                            android.util.Log.d("DetailActivity", "Hiding button - applicationOpen is false");
                            btnAction.setVisibility(View.GONE);
                        } else {
                            // applicationOpen이 null이거나 true면 버튼 표시
                            android.util.Log.d("DetailActivity", "Showing button");
                            btnAction.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // 문서가 없으면 기본으로 버튼 표시 (테스트용)
                        android.util.Log.d("DetailActivity", "Document not exists, showing button by default");
                        btnAction.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DetailActivity", "checkApplicationOpenAndShowButton error: " + e.getMessage());
                    // 에러 시에도 버튼 표시
                    btnAction.setVisibility(View.VISIBLE);
                });
    }

    private void setupDefaultContent() {
        // 데이터가 없을 때 빈 상태 표시
        showEmptyState();
    }

    private void showEmptyState() {
        tvDetailTitle.setText("동아리 정보 없음");
        tvDetailDescription.setText("해당 동아리 정보를 찾을 수 없습니다.");
        llFeatureList.removeAllViews();

        // 최고 관리자 모드면 버튼 숨김
        if (isSuperAdminMode) {
            btnAction.setVisibility(View.GONE);
            return;
        }

        // 이미 중앙동아리에 가입된 경우 버튼 숨김
        if (userCentralClubId != null && !userCentralClubId.isEmpty()) {
            btnAction.setVisibility(View.GONE);
        } else {
            // 가입 신청 버튼 표시
            btnAction.setText("가입 신청하기");
            btnAction.setVisibility(View.VISIBLE);
        }
    }

    private void setupDefaultFeatures() {
        // 기본 기능 목록 (동아리 정보에서 로드)
        // 하드코딩 제거
    }

    private void addFeature(String featureText) {
        TextView featureItem = new TextView(this);
        featureItem.setText(featureText);
        featureItem.setTextSize(16);
        featureItem.setTextColor(getResources().getColor(android.R.color.darker_gray, null));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        featureItem.setLayoutParams(params);

        llFeatureList.addView(featureItem);
    }

    private void setupListeners() {
        // 뒤로가기 버튼
        ivBack.setOnClickListener(v -> finish());

        // 최고 관리자 설정 버튼
        ivSuperAdminSettings.setOnClickListener(v -> {
            openSuperAdminSettings();
        });

        // 액션 버튼 - 내 동아리로 가기 또는 회원가입 화면으로 이동
        btnAction.setOnClickListener(v -> {
            // 내 동아리로 가기 버튼인 경우
            if (isMyClub && userCentralClubId != null) {
                Intent intent = new Intent(DetailActivity.this, ClubMainActivity.class);
                intent.putExtra("club_id", userCentralClubId);
                intent.putExtra("club_name", userCentralClubName);
                intent.putExtra("isCentralClub", true);
                startActivity(intent);
                finish();
                return;
            }

            if (fromClubList) {
                // 일반 동아리 - 회원가입 화면으로 이동하여 정보 입력
                String clubId = getIntent().getStringExtra("club_id");
                if (clubId == null) {
                    Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(DetailActivity.this, MemberRegistrationActivity.class);
                intent.putExtra("club_name", clubName);
                intent.putExtra("central_club_id", clubId);
                intent.putExtra("is_central_club", false);  // 일반동아리
                startActivity(intent);
            } else {
                // 중앙 동아리 - 회원가입 화면으로 이동
                Intent intent = new Intent(DetailActivity.this, MemberRegistrationActivity.class);
                String clubNameToUse = getClubName(pageIndex);
                intent.putExtra("club_name", clubNameToUse);
                intent.putExtra("is_central_club", true);

                // Intent에서 받은 club_id 사용, 없으면 currentItem에서 가져오기
                String centralClubId = getIntent().getStringExtra("club_id");
                if (centralClubId == null || centralClubId.isEmpty()) {
                    if (currentItem != null && currentItem.getClubId() != null) {
                        centralClubId = currentItem.getClubId();
                    }
                }

                if (centralClubId == null || centralClubId.isEmpty()) {
                    Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                intent.putExtra("central_club_id", centralClubId);
                startActivity(intent);
            }
        });

        // 편집 버튼 (관리자만)
        fabEdit.setOnClickListener(v -> {
            if (isAdmin) {
                showEditDialog();
            }
        });

        // 동아리 관리 버튼 (관리자 모드에서만 표시)
        btnAdminManage.setOnClickListener(v -> {
            String clubId = getIntent().getStringExtra("club_id");
            String clubNameToUse = getIntent().getStringExtra("club_name");

            // clubId가 없으면 currentItem에서 가져오기
            if (clubId == null || clubId.isEmpty()) {
                if (currentItem != null && currentItem.getClubId() != null) {
                    clubId = currentItem.getClubId();
                }
            }
            if (clubNameToUse == null || clubNameToUse.isEmpty()) {
                clubNameToUse = getClubName(pageIndex);
            }

            if (clubId == null || clubId.isEmpty()) {
                Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // 최고 관리자가 동아리 관리하기 버튼을 누르면 자동으로 관리자 모드 활성화
            if (isSuperAdminMode) {
                ClubSettingsActivity.setClubAdminModeStatic(DetailActivity.this, true);
            }

            // 동아리 메인 페이지로 이동
            Intent intent = new Intent(DetailActivity.this, ClubMainActivity.class);
            intent.putExtra("club_id", clubId);
            intent.putExtra("club_name", clubNameToUse);
            // 캐러셀에서 오는 동아리는 중앙동아리
            intent.putExtra("isCentralClub", true);
            startActivity(intent);
        });
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_carousel, null);

        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        MaterialButton btnChangeImage = dialogView.findViewById(R.id.btnChangeImage);
        ImageView ivPreview = dialogView.findViewById(R.id.ivPreview);

        // Set current values
        if (currentItem != null) {
            etTitle.setText(currentItem.getTitle());
            etDescription.setText(currentItem.getDescription());

            if (currentItem.hasFirebaseImage()) {
                Glide.with(this)
                        .load(currentItem.getImageUrl())
                        .centerCrop()
                        .into(ivPreview);
            }
        }

        btnChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        builder.setView(dialogView)
                .setTitle("캐러셀 수정")
                .setPositiveButton("저장", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (title.isEmpty() || description.isEmpty()) {
                        Toast.makeText(this, "제목과 설명을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveCarouselItem(title, description);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void uploadImage(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] imageData = baos.toByteArray();

            firebaseManager.uploadCarouselImage(imageData, pageIndex, new FirebaseManager.SignatureCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(DetailActivity.this, "이미지 업로드 성공", Toast.LENGTH_SHORT).show();

                    // Update current item with new image URL
                    if (currentItem == null) {
                        currentItem = new CarouselItem();
                        currentItem.setPosition(pageIndex);
                    }
                    currentItem.setImageUrl(downloadUrl);

                    // TODO: Display updated image in ViewPager2
                    // Glide.with(DetailActivity.this)
                    //         .load(downloadUrl)
                    //         .centerCrop()
                    //         .into(ivDetailImage);
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(DetailActivity.this, "이미지 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCarouselItem(String title, String description) {
        progressBar.setVisibility(View.VISIBLE);

        if (currentItem == null) {
            currentItem = new CarouselItem();
            currentItem.setPosition(pageIndex);
        }

        currentItem.setTitle(title);
        currentItem.setDescription(description);

        firebaseManager.saveCarouselItem(currentItem, new FirebaseManager.CarouselCallback() {
            @Override
            public void onSuccess(CarouselItem item) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DetailActivity.this, "저장 완료", Toast.LENGTH_SHORT).show();
                currentItem = item;
                displayCarouselItem(item);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DetailActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClubListContent() {
        // 동아리 목록에서 온 경우의 콘텐츠 설정
        tvDetailTitle.setText(clubName);

        // 동아리 설명 (기본 템플릿)
        String description = clubName + "에 오신 것을 환영합니다! " +
                "우리 동아리에 가입하시려면 아래 정보를 입력해주세요.";
        tvDetailDescription.setText(description);

        // 기능 목록
        llFeatureList.removeAllViews();
        addFeature("📝 회원 가입 신청");
        addFeature("✅ 관리자 승인 대기");
        addFeature("📧 가입 완료 알림");
        addFeature("👥 동아리 활동 시작");

        // 버튼 - 기본으로 표시 (최고 관리자 모드가 아닐 때)
        btnAction.setText("가입 신청하기");
        btnAction.setVisibility(isSuperAdminMode ? View.GONE : View.VISIBLE);

        // 편집 버튼 숨김 (동아리 목록에서 온 경우)
        fabEdit.setVisibility(View.GONE);
    }

    private String getClubName(int index) {
        // fromClubList가 true이면 전달받은 clubName 사용
        if (fromClubList && clubName != null && !clubName.isEmpty()) {
            return clubName;
        }

        // Intent에서 전달받은 club_name 사용
        String intentClubName = getIntent().getStringExtra("club_name");
        if (intentClubName != null && !intentClubName.isEmpty()) {
            return intentClubName;
        }

        // currentItem에서 가져오기
        if (currentItem != null && currentItem.getClubName() != null) {
            return currentItem.getClubName();
        }
        if (currentItem != null && currentItem.getTitle() != null) {
            return currentItem.getTitle();
        }

        return "동아리";
    }

    private void openSuperAdminSettings() {
        Intent intent = new Intent(this, SuperAdminSettingsActivity.class);
        intent.putExtra("page_index", pageIndex);
        intent.putExtra("club_name", getClubName(pageIndex));

        // Intent에서 club_id 가져오기, 없으면 currentItem에서 가져오기
        String clubId = getIntent().getStringExtra("club_id");
        if (clubId == null || clubId.isEmpty()) {
            if (currentItem != null && currentItem.getClubId() != null) {
                clubId = currentItem.getClubId();
            }
        }
        intent.putExtra("club_id", clubId);

        if (currentItem != null) {
            intent.putExtra("carousel_title", currentItem.getTitle());
            intent.putExtra("carousel_description", currentItem.getDescription());
            intent.putExtra("carousel_image_url", currentItem.getImageUrl());
        }
        startActivity(intent);
    }
}
