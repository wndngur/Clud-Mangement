package com.example.clubmanagement.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.R;
import com.example.clubmanagement.SettingsActivity;
import com.example.clubmanagement.models.CarouselItem;
import com.example.clubmanagement.models.CentralClubApplication;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.models.Member;
import com.example.clubmanagement.models.UserData;
import com.example.clubmanagement.models.WithdrawalRequest;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClubSettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "ClubAdminPrefs";
    public static final String KEY_CLUB_ADMIN_MODE = "club_admin_mode_active";

    private FirebaseManager firebaseManager;
    private MaterialButton btnClubAdmin;
    private MaterialButton btnLogoutAdmin;
    private MaterialButton btnEditDescription;
    private MaterialButton btnMemberManagement;
    private MaterialButton btnEditClubInfo;
    private TextView tvAdminStatus;
    private TextView tvClubName;
    private ProgressBar progressBar;
    private MaterialCardView cardPosterSettings;
    private MaterialCardView cardDescriptionSettings;
    private MaterialCardView cardMemberManagement;
    private MaterialCardView cardClubInfoEdit;
    private MaterialCardView cardEditRequests;
    private MaterialButton btnViewEditRequests;
    private TextView tvUnreadBadge;
    private MaterialCardView cardCentralApplication;
    private MaterialButton btnApplyCentral;
    private TextView tvApplicationStatus;
    private TextView tvCentralApplicationDesc;
    private MaterialCardView cardWithdrawal;
    private MaterialButton btnWithdrawalRequest;
    private MaterialButton btnCancelWithdrawal;
    private TextView tvWithdrawalDesc;
    private MaterialCardView cardAdminRole;
    private UserData currentUserData;
    private Club currentClub;
    private String clubName;
    private String clubId;
    private WithdrawalRequest pendingWithdrawalRequest;

    // Poster image UI (캐러셀에 표시되는 1장)
    private ImageView ivPosterPreview;
    private LinearLayout llPosterEmpty;
    private MaterialButton btnChangePoster;
    private String posterImageUrl = null;
    private ActivityResultLauncher<Intent> posterImagePickerLauncher;

    // Detail images UI (상세보기에 표시되는 최대 3장)
    private ViewPager2 vpDetailPreview;
    private LinearLayout llDetailEmpty;
    private LinearLayout llDetailIndicator;
    private MaterialButton btnAddDetailImage;
    private MaterialButton btnRemoveDetailImage;
    private TextView tvImageCount;
    private List<String> detailImageUrls = new ArrayList<>();
    private DetailImageAdapter detailImageAdapter;
    private ActivityResultLauncher<Intent> detailImagePickerLauncher;
    private int currentDetailIndex = 0;

    private CarouselItem currentCarouselItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_settings);

        firebaseManager = FirebaseManager.getInstance();

        // Get club name and ID from intent
        clubName = getIntent().getStringExtra("club_name");
        clubId = getIntent().getStringExtra("club_id");

        android.util.Log.d("ClubSettingsActivity", "onCreate - received clubId: " + clubId + ", clubName: " + clubName);

        if (clubName == null) {
            clubName = "동아리";
        }
        if (clubId == null) {
            clubId = clubName.replaceAll("\\s+", "_").toLowerCase();
            android.util.Log.d("ClubSettingsActivity", "onCreate - generated clubId from name: " + clubId);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(clubName + " 설정");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        setupImagePicker();
        setupPosterViewPager();
        loadAdminStatus();
        setupListeners();
    }

    private void initViews() {
        btnClubAdmin = findViewById(R.id.btnClubAdmin);
        btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);
        btnEditDescription = findViewById(R.id.btnEditDescription);
        btnMemberManagement = findViewById(R.id.btnMemberManagement);
        btnEditClubInfo = findViewById(R.id.btnEditClubInfo);
        tvAdminStatus = findViewById(R.id.tvAdminStatus);
        tvClubName = findViewById(R.id.tvClubName);
        progressBar = findViewById(R.id.progressBarSettings);
        cardPosterSettings = findViewById(R.id.cardPosterSettings);
        cardDescriptionSettings = findViewById(R.id.cardDescriptionSettings);
        cardMemberManagement = findViewById(R.id.cardMemberManagement);
        cardClubInfoEdit = findViewById(R.id.cardClubInfoEdit);
        cardEditRequests = findViewById(R.id.cardEditRequests);
        btnViewEditRequests = findViewById(R.id.btnViewEditRequests);
        tvUnreadBadge = findViewById(R.id.tvUnreadBadge);
        cardCentralApplication = findViewById(R.id.cardCentralApplication);
        btnApplyCentral = findViewById(R.id.btnApplyCentral);
        tvApplicationStatus = findViewById(R.id.tvApplicationStatus);
        tvCentralApplicationDesc = findViewById(R.id.tvCentralApplicationDesc);
        cardWithdrawal = findViewById(R.id.cardWithdrawal);
        btnWithdrawalRequest = findViewById(R.id.btnWithdrawalRequest);
        btnCancelWithdrawal = findViewById(R.id.btnCancelWithdrawal);
        tvWithdrawalDesc = findViewById(R.id.tvWithdrawalDesc);
        cardAdminRole = findViewById(R.id.cardAdminRole);

        // Poster image views (1장)
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        llPosterEmpty = findViewById(R.id.llPosterEmpty);
        btnChangePoster = findViewById(R.id.btnChangePoster);

        // Detail images views (최대 3장)
        vpDetailPreview = findViewById(R.id.vpDetailPreview);
        llDetailEmpty = findViewById(R.id.llDetailEmpty);
        llDetailIndicator = findViewById(R.id.llDetailIndicator);
        btnAddDetailImage = findViewById(R.id.btnAddDetailImage);
        btnRemoveDetailImage = findViewById(R.id.btnRemoveDetailImage);
        tvImageCount = findViewById(R.id.tvImageCount);

        tvClubName.setText(clubName);

        // 최고 관리자인 경우 동아리 관리자 권한 카드 숨기기
        if (SettingsActivity.isSuperAdminMode(this)) {
            cardAdminRole.setVisibility(View.GONE);
        }
    }

    private void loadAdminStatus() {
        // SharedPreferences 기반으로 간단하게 상태 확인
        updateAdminStatusDisplay();
    }

    private void setupListeners() {
        btnClubAdmin.setOnClickListener(v -> toggleClubAdminMode());
        btnLogoutAdmin.setOnClickListener(v -> logoutAdmin());
        btnEditDescription.setOnClickListener(v -> openDescriptionEdit());
        btnMemberManagement.setOnClickListener(v -> openMemberManagement());
        btnEditClubInfo.setOnClickListener(v -> openClubInfoEdit());
        btnViewEditRequests.setOnClickListener(v -> openEditRequests());
        btnApplyCentral.setOnClickListener(v -> applyCentralClub());
        btnWithdrawalRequest.setOnClickListener(v -> showWithdrawalDialog());
        btnCancelWithdrawal.setOnClickListener(v -> cancelWithdrawalRequest());

        // Poster image management (1장)
        btnChangePoster.setOnClickListener(v -> openGalleryForPoster());

        // Detail images management (최대 3장)
        btnAddDetailImage.setOnClickListener(v -> openGalleryForDetailImage());
        btnRemoveDetailImage.setOnClickListener(v -> removeCurrentDetailImage());
    }

    private void toggleClubAdminMode() {
        boolean currentMode = isClubAdminMode(this);

        if (currentMode) {
            // 관리자 모드 비활성화 - 항상 허용
            setClubAdminModeStatic(this, false);
            Toast.makeText(this, "동아리 관리자 모드가 비활성화되었습니다", Toast.LENGTH_SHORT).show();
            updateAdminStatusDisplay();
        } else {
            // 관리자 모드 활성화 - 권한 확인 필요
            activateAdminModeWithCheck();
        }
    }

    private void activateAdminModeWithCheck() {
        // 최고 관리자인 경우 바로 활성화
        if (SettingsActivity.isSuperAdminMode(this)) {
            setClubAdminModeStatic(this, true);
            Toast.makeText(this, "동아리 관리자 모드가 활성화되었습니다 (최고관리자)", Toast.LENGTH_SHORT).show();
            updateAdminStatusDisplay();
            return;
        }

        // 일반 사용자인 경우 Firebase에서 관리자 권한 확인
        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        firebaseManager.checkMemberAdminPermission(clubId, userId, new FirebaseManager.AdminCheckCallback() {
            @Override
            public void onResult(boolean isAdmin) {
                progressBar.setVisibility(ProgressBar.GONE);
                if (isAdmin) {
                    setClubAdminModeStatic(ClubSettingsActivity.this, true);
                    Toast.makeText(ClubSettingsActivity.this, "동아리 관리자 모드가 활성화되었습니다", Toast.LENGTH_SHORT).show();
                    updateAdminStatusDisplay();
                } else {
                    Toast.makeText(ClubSettingsActivity.this, "관리자 권한이 없습니다. 동아리 관리자에게 문의하세요.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(ClubSettingsActivity.this, "권한 확인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static boolean isClubAdminMode(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_CLUB_ADMIN_MODE, false);
    }

    public static void setClubAdminModeStatic(android.content.Context context, boolean active) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_CLUB_ADMIN_MODE, active).apply();
    }

    private void setupImagePicker() {
        // 포스터 이미지 피커 (1장)
        posterImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            uploadPosterImage(selectedImageUri);
                        }
                    }
                }
        );

        // 상세보기 이미지 피커 (최대 3장)
        detailImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            uploadDetailImage(selectedImageUri);
                        }
                    }
                }
        );
    }

    private void setupPosterViewPager() {
        // 상세보기 이미지용 ViewPager2 설정
        detailImageAdapter = new DetailImageAdapter();
        vpDetailPreview.setAdapter(detailImageAdapter);

        vpDetailPreview.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentDetailIndex = position;
                updateDetailIndicators();
            }
        });
    }

    // ========================================
    // 포스터 이미지 (1장) 관련 메소드
    // ========================================

    private void openGalleryForPoster() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        posterImagePickerLauncher.launch(intent);
    }

    private void uploadPosterImage(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);
        btnChangePoster.setEnabled(false);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();

            String fileName = clubId + "_poster";
            firebaseManager.uploadCarouselImage(fileName, imageData, new FirebaseManager.SignatureCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    posterImageUrl = downloadUrl;
                    saveCarouselData();
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    btnChangePoster.setEnabled(true);
                    Toast.makeText(ClubSettingsActivity.this, "포스터 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            btnChangePoster.setEnabled(true);
            Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePosterUI() {
        if (posterImageUrl != null && !posterImageUrl.isEmpty()) {
            llPosterEmpty.setVisibility(View.GONE);
            ivPosterPreview.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(posterImageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.carousel_image_1)
                    .into(ivPosterPreview);
        } else {
            llPosterEmpty.setVisibility(View.VISIBLE);
            ivPosterPreview.setVisibility(View.INVISIBLE);
        }
    }

    // ========================================
    // 상세보기 이미지 (최대 3장) 관련 메소드
    // ========================================

    private void openGalleryForDetailImage() {
        if (detailImageUrls.size() >= CarouselItem.MAX_IMAGES) {
            Toast.makeText(this, "최대 " + CarouselItem.MAX_IMAGES + "장까지만 등록 가능합니다", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        detailImagePickerLauncher.launch(intent);
    }

    private void uploadDetailImage(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);
        btnAddDetailImage.setEnabled(false);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();

            String fileName = clubId + "_detail_" + System.currentTimeMillis();
            firebaseManager.uploadCarouselImage(fileName, imageData, new FirebaseManager.SignatureCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    detailImageUrls.add(downloadUrl);
                    saveCarouselData();
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    btnAddDetailImage.setEnabled(true);
                    Toast.makeText(ClubSettingsActivity.this, "이미지 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            btnAddDetailImage.setEnabled(true);
            Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void removeCurrentDetailImage() {
        if (detailImageUrls.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle("이미지 삭제")
                .setMessage("현재 이미지를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    if (currentDetailIndex >= 0 && currentDetailIndex < detailImageUrls.size()) {
                        detailImageUrls.remove(currentDetailIndex);
                        if (currentDetailIndex >= detailImageUrls.size() && currentDetailIndex > 0) {
                            currentDetailIndex--;
                        }
                        saveCarouselData();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void updateDetailUI() {
        tvImageCount.setText(detailImageUrls.size() + "/" + CarouselItem.MAX_IMAGES);

        if (detailImageUrls.isEmpty()) {
            llDetailEmpty.setVisibility(View.VISIBLE);
            vpDetailPreview.setVisibility(View.INVISIBLE);
            llDetailIndicator.setVisibility(View.GONE);
            btnRemoveDetailImage.setEnabled(false);
        } else {
            llDetailEmpty.setVisibility(View.GONE);
            vpDetailPreview.setVisibility(View.VISIBLE);
            detailImageAdapter.notifyDataSetChanged();
            btnRemoveDetailImage.setEnabled(true);

            if (detailImageUrls.size() > 1) {
                llDetailIndicator.setVisibility(View.VISIBLE);
                setupDetailIndicators();
            } else {
                llDetailIndicator.setVisibility(View.GONE);
            }
        }

        btnAddDetailImage.setEnabled(detailImageUrls.size() < CarouselItem.MAX_IMAGES);
    }

    private void setupDetailIndicators() {
        llDetailIndicator.removeAllViews();
        for (int i = 0; i < detailImageUrls.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(i == currentDetailIndex ? Color.WHITE : Color.parseColor("#80FFFFFF"));
            dot.setBackground(drawable);

            llDetailIndicator.addView(dot);
        }
    }

    private void updateDetailIndicators() {
        for (int i = 0; i < llDetailIndicator.getChildCount(); i++) {
            View dot = llDetailIndicator.getChildAt(i);
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(i == currentDetailIndex ? Color.WHITE : Color.parseColor("#80FFFFFF"));
            dot.setBackground(drawable);
        }
    }

    // ========================================
    // 데이터 로드/저장 메소드
    // ========================================

    private void loadPosterImages() {
        firebaseManager.getCarouselItems(new FirebaseManager.CarouselListCallback() {
            @Override
            public void onSuccess(List<CarouselItem> items) {
                if (items != null) {
                    for (CarouselItem item : items) {
                        if (clubId != null && clubId.equals(item.getClubId())) {
                            currentCarouselItem = item;
                            // 포스터 이미지 (imageUrl)
                            posterImageUrl = item.getImageUrl();
                            // 상세보기 이미지 (imageUrls)
                            detailImageUrls.clear();
                            if (item.getImageUrls() != null) {
                                detailImageUrls.addAll(item.getImageUrls());
                            }
                            updatePosterUI();
                            updateDetailUI();
                            return;
                        }
                    }
                }
                // No existing item found
                currentCarouselItem = null;
                posterImageUrl = null;
                detailImageUrls.clear();
                updatePosterUI();
                updateDetailUI();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ClubSettingsActivity.this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveCarouselData() {
        if (currentCarouselItem == null) {
            currentCarouselItem = new CarouselItem();
            currentCarouselItem.setClubId(clubId);
            currentCarouselItem.setClubName(clubName);
            currentCarouselItem.setTitle(clubName);
            currentCarouselItem.setDescription(clubName + " 동아리입니다.");
            currentCarouselItem.setPosition(0);
        }

        // 포스터 이미지 (캐러셀용)
        currentCarouselItem.setImageUrl(posterImageUrl);
        // 상세보기 이미지 (최대 3장)
        currentCarouselItem.setImageUrls(new ArrayList<>(detailImageUrls));

        firebaseManager.saveCarouselItem(currentCarouselItem, new FirebaseManager.CarouselCallback() {
            @Override
            public void onSuccess(CarouselItem item) {
                progressBar.setVisibility(View.GONE);
                btnChangePoster.setEnabled(true);
                btnAddDetailImage.setEnabled(detailImageUrls.size() < CarouselItem.MAX_IMAGES);
                currentCarouselItem = item;
                updatePosterUI();
                updateDetailUI();
                Toast.makeText(ClubSettingsActivity.this, "저장되었습니다", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnChangePoster.setEnabled(true);
                btnAddDetailImage.setEnabled(true);
                Toast.makeText(ClubSettingsActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Inner Adapter for detail images preview
    private class DetailImageAdapter extends RecyclerView.Adapter<DetailImageAdapter.ImageViewHolder> {

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            if (position < detailImageUrls.size()) {
                Glide.with(ClubSettingsActivity.this)
                        .load(detailImageUrls.get(position))
                        .centerCrop()
                        .placeholder(R.drawable.carousel_image_1)
                        .into(holder.imageView);
            }
        }

        @Override
        public int getItemCount() {
            return detailImageUrls.size();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ImageViewHolder(ImageView itemView) {
                super(itemView);
                this.imageView = itemView;
            }
        }
    }

    private void openDescriptionEdit() {
        Intent intent = new Intent(ClubSettingsActivity.this, ClubDescriptionEditActivity.class);
        intent.putExtra("club_id", clubId);
        intent.putExtra("club_name", clubName);
        startActivity(intent);
    }

    private void openMemberManagement() {
        Intent intent = new Intent(ClubSettingsActivity.this, MemberManagementActivity.class);
        intent.putExtra("club_id", clubId);
        intent.putExtra("club_name", clubName);
        startActivity(intent);
    }

    private void openClubInfoEdit() {
        Intent intent = new Intent(ClubSettingsActivity.this, ClubInfoEditActivity.class);
        intent.putExtra("club_id", clubId);
        intent.putExtra("club_name", clubName);
        startActivity(intent);
    }

    private void openEditRequests() {
        Intent intent = new Intent(ClubSettingsActivity.this, EditRequestsActivity.class);
        intent.putExtra("club_id", clubId);
        intent.putExtra("club_name", clubName);
        startActivity(intent);
    }

    private void loadUnreadEditRequestCount() {
        firebaseManager.getUnreadEditRequestCount(clubId, new FirebaseManager.CountCallback() {
            @Override
            public void onSuccess(int count) {
                if (count > 0) {
                    tvUnreadBadge.setText(String.valueOf(count));
                    tvUnreadBadge.setVisibility(View.VISIBLE);
                } else {
                    tvUnreadBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                tvUnreadBadge.setVisibility(View.GONE);
            }
        });
    }

    private void showPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_password, null);
        EditText etPassword = dialogView.findViewById(R.id.etAdminPassword);

        new AlertDialog.Builder(this)
                .setTitle("동아리 관리자 인증")
                .setMessage("동아리 관리자 비밀번호를 입력하세요\n(기본값: clubadmin123)")
                .setView(dialogView)
                .setPositiveButton("인증", (dialog, which) -> {
                    String password = etPassword.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                    } else {
                        verifyAndSetAdmin(password);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void verifyAndSetAdmin(String password) {
        progressBar.setVisibility(ProgressBar.VISIBLE);

        firebaseManager.verifyAdminPassword("CLUB_ADMIN", clubId, password, new FirebaseManager.PasswordVerifyCallback() {
            @Override
            public void onSuccess(boolean isValid, String level, String verifiedClubId) {
                if (isValid) {
                    setUserAdminLevel(level, verifiedClubId);
                } else {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(ClubSettingsActivity.this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(ClubSettingsActivity.this, "인증 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUserAdminLevel(String adminLevel, String verifiedClubId) {
        String userId = firebaseManager.getCurrentUserId();

        firebaseManager.setUserAdminLevel(userId, adminLevel, verifiedClubId, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(ClubSettingsActivity.this, "동아리 관리자 권한이 활성화되었습니다", Toast.LENGTH_SHORT).show();
                loadAdminStatus();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(ProgressBar.GONE);
                Toast.makeText(ClubSettingsActivity.this, "권한 설정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logoutAdmin() {
        new AlertDialog.Builder(this)
                .setTitle("관리자 로그아웃")
                .setMessage("관리자 권한을 해제하시겠습니까?")
                .setPositiveButton("해제", (dialog, which) -> {
                    String userId = firebaseManager.getCurrentUserId();
                    firebaseManager.setUserAdminLevel(userId, "NONE", null, new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(ClubSettingsActivity.this, "관리자 권한이 해제되었습니다", Toast.LENGTH_SHORT).show();
                            loadAdminStatus();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(ClubSettingsActivity.this, "권한 해제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void updateAdminStatusDisplay() {
        boolean isClubAdmin = isClubAdminMode(this);
        boolean isSuperAdmin = SettingsActivity.isSuperAdminMode(this);

        // 최고 관리자인 경우 동아리 관리자 권한 카드 숨기기
        if (cardAdminRole != null) {
            cardAdminRole.setVisibility(isSuperAdmin ? View.GONE : View.VISIBLE);
        }

        if (isClubAdmin) {
            tvAdminStatus.setText("현재 상태: 동아리 관리자 모드 실행 중\n\n이 동아리를 관리할 수 있습니다.");
            btnClubAdmin.setText("관리자 모드 끄기");
            btnClubAdmin.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light, null));
            btnLogoutAdmin.setVisibility(View.GONE);

            // 관리자 모드일 때 관리 카드뷰들 표시
            cardMemberManagement.setVisibility(View.VISIBLE);
            cardClubInfoEdit.setVisibility(View.VISIBLE);

            // 포스터/설명 편집은 중앙동아리만 가능하므로 일단 숨김 처리
            // loadClubAndApplicationStatus에서 중앙동아리 여부 확인 후 표시
            cardPosterSettings.setVisibility(View.GONE);
            cardDescriptionSettings.setVisibility(View.GONE);

            // 수정 요청 카드 표시 및 읽지 않은 개수 로드
            cardEditRequests.setVisibility(View.VISIBLE);
            loadUnreadEditRequestCount();

            // 중앙동아리 신청 카드 표시 (최고 관리자가 아닌 경우에만)
            if (!SettingsActivity.isSuperAdminMode(this)) {
                cardCentralApplication.setVisibility(View.VISIBLE);
                loadClubAndApplicationStatus();
            } else {
                cardCentralApplication.setVisibility(View.GONE);
                // 최고 관리자인 경우에도 동아리 정보 로드하여 포스터/설명 카드 표시 여부 결정
                loadClubInfoForPosterSettings();
            }

            // 관리자 모드일 때 탈퇴 신청 카드 숨기기
            cardWithdrawal.setVisibility(View.GONE);
        } else {
            tvAdminStatus.setText("현재 상태: 일반 사용자");
            btnClubAdmin.setText("관리자 모드 실행");
            btnClubAdmin.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, null));
            btnLogoutAdmin.setVisibility(View.GONE);

            // 일반 사용자일 때 관리 카드뷰들 숨기기
            cardPosterSettings.setVisibility(View.GONE);
            cardDescriptionSettings.setVisibility(View.GONE);
            cardMemberManagement.setVisibility(View.GONE);
            cardClubInfoEdit.setVisibility(View.GONE);
            cardEditRequests.setVisibility(View.GONE);
            cardCentralApplication.setVisibility(View.GONE);

            // 일반 사용자일 때 탈퇴 신청 카드 표시
            cardWithdrawal.setVisibility(View.VISIBLE);
            loadWithdrawalRequestStatus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면에 돌아올 때 관리자 상태 및 수정 요청 개수 새로고침
        updateAdminStatusDisplay();
    }

    // ========================================
    // Central Club Application Methods
    // ========================================

    private void loadClubAndApplicationStatus() {
        // 먼저 동아리 정보 로드
        firebaseManager.getClub(clubId, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(Club club) {
                currentClub = club;
                updateCentralApplicationUI();
            }

            @Override
            public void onFailure(Exception e) {
                // 동아리 정보 없으면 새로 생성된 것으로 간주
                currentClub = null;
                updateCentralApplicationUI();
            }
        });
    }

    private void loadClubInfoForPosterSettings() {
        // 최고 관리자 모드에서 포스터/설명 카드 표시 여부 결정
        firebaseManager.getClub(clubId, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(Club club) {
                currentClub = club;
                // 중앙동아리인 경우에만 포스터/설명 카드 표시
                if (club != null && club.isCentralClub()) {
                    cardPosterSettings.setVisibility(View.VISIBLE);
                    cardDescriptionSettings.setVisibility(View.VISIBLE);
                    loadPosterImages();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // 동아리 정보 없으면 포스터 카드 숨김 유지
            }
        });
    }

    private void updateCentralApplicationUI() {
        // 이미 중앙동아리인지 확인
        if (currentClub != null && currentClub.isCentralClub()) {
            tvCentralApplicationDesc.setText("이 동아리는 이미 중앙동아리입니다.");
            btnApplyCentral.setEnabled(false);
            btnApplyCentral.setText("이미 중앙동아리");
            tvApplicationStatus.setVisibility(View.GONE);

            // 중앙동아리인 경우 포스터/설명 편집 카드 표시
            cardPosterSettings.setVisibility(View.VISIBLE);
            cardDescriptionSettings.setVisibility(View.VISIBLE);
            loadPosterImages();
            return;
        }

        // 일반동아리인 경우 포스터/설명 카드 숨김 유지
        cardPosterSettings.setVisibility(View.GONE);
        cardDescriptionSettings.setVisibility(View.GONE);

        // 대기중인 신청이 있는지 확인
        firebaseManager.getPendingApplicationForClub(clubId, new FirebaseManager.CentralApplicationCallback() {
            @Override
            public void onSuccess(CentralClubApplication application) {
                if (application != null) {
                    // 이미 신청한 상태
                    tvCentralApplicationDesc.setText("중앙동아리 신청이 접수되었습니다.\n최고 관리자의 승인을 기다리고 있습니다.");
                    btnApplyCentral.setEnabled(false);
                    btnApplyCentral.setText("신청 완료");
                    tvApplicationStatus.setVisibility(View.VISIBLE);
                    tvApplicationStatus.setText("대기중");
                } else {
                    // 신청 가능
                    String desc = "중앙동아리로 승격하기 위해 신청할 수 있습니다.";
                    if (currentClub != null) {
                        int memberCount = currentClub.getMemberCount();
                        long days = currentClub.getDaysSinceFounding();
                        boolean memberOk = memberCount >= Club.CENTRAL_CLUB_REGISTER_MIN_MEMBERS;
                        boolean daysOk = currentClub.canApplyForCentralByDate();

                        desc += "\n\n현재 상태:";
                        desc += "\n• 부원 수: " + memberCount + "명 " + (memberOk ? "✓" : "(20명 필요)");
                        desc += "\n• 설립 후: " + days + "일 " + (daysOk ? "✓" : "(180일 필요)");

                        if (!memberOk || !daysOk) {
                            desc += "\n\n조건을 충족하지 못했지만 신청할 수 있습니다.";
                        }
                    } else {
                        desc += "\n(부원 20명 이상, 설립 후 6개월 이상 권장)";
                    }

                    tvCentralApplicationDesc.setText(desc);
                    btnApplyCentral.setEnabled(true);
                    btnApplyCentral.setText("중앙동아리 신청");
                    tvApplicationStatus.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                tvCentralApplicationDesc.setText("신청 상태를 확인할 수 없습니다.");
                btnApplyCentral.setEnabled(true);
            }
        });
    }

    // 서명 목록을 저장할 변수
    private List<String> fetchedSignatures = new ArrayList<>();

    private void applyCentralClub() {
        // 이미 중앙동아리인지 다시 확인
        if (currentClub != null && currentClub.isCentralClub()) {
            Toast.makeText(this, "이미 중앙동아리입니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 서명 목록 초기화
        fetchedSignatures.clear();

        // 신청 확인 다이얼로그
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_central_application, null);
        EditText etReason = dialogView.findViewById(R.id.etApplicationReason);
        TextView tvMemberInfo = dialogView.findViewById(R.id.tvMemberInfo);
        TextView tvDaysInfo = dialogView.findViewById(R.id.tvDaysInfo);
        EditText etExpectedBudget = dialogView.findViewById(R.id.etExpectedBudget);
        MaterialButton btnFetchSignatures = dialogView.findViewById(R.id.btnFetchSignatures);
        TextView tvSignatureStatus = dialogView.findViewById(R.id.tvSignatureStatus);

        // 월별 활동 계획 입력 필드
        EditText etMonth1 = dialogView.findViewById(R.id.etMonth1);
        EditText etMonth2 = dialogView.findViewById(R.id.etMonth2);
        EditText etMonth3 = dialogView.findViewById(R.id.etMonth3);
        EditText etMonth4 = dialogView.findViewById(R.id.etMonth4);
        EditText etMonth5 = dialogView.findViewById(R.id.etMonth5);
        EditText etMonth6 = dialogView.findViewById(R.id.etMonth6);
        EditText etMonth7 = dialogView.findViewById(R.id.etMonth7);
        EditText etMonth8 = dialogView.findViewById(R.id.etMonth8);
        EditText etMonth9 = dialogView.findViewById(R.id.etMonth9);
        EditText etMonth10 = dialogView.findViewById(R.id.etMonth10);
        EditText etMonth11 = dialogView.findViewById(R.id.etMonth11);
        EditText etMonth12 = dialogView.findViewById(R.id.etMonth12);

        // 현재 상태 표시
        if (currentClub != null) {
            int memberCount = currentClub.getMemberCount();
            long days = currentClub.getDaysSinceFounding();
            boolean memberOk = memberCount >= Club.CENTRAL_CLUB_REGISTER_MIN_MEMBERS;
            boolean daysOk = currentClub.canApplyForCentralByDate();

            tvMemberInfo.setText("부원 수: " + memberCount + "명 / 20명 " + (memberOk ? "✓" : "✗"));
            tvMemberInfo.setTextColor(getResources().getColor(memberOk ? android.R.color.holo_green_dark : android.R.color.holo_red_dark, null));

            tvDaysInfo.setText("설립 후: " + days + "일 / 180일 " + (daysOk ? "✓" : "✗"));
            tvDaysInfo.setTextColor(getResources().getColor(daysOk ? android.R.color.holo_green_dark : android.R.color.holo_red_dark, null));
        } else {
            tvMemberInfo.setText("부원 수: 정보 없음");
            tvDaysInfo.setText("설립일: 정보 없음");
        }

        // 서명 가져오기 버튼 리스너
        btnFetchSignatures.setOnClickListener(v -> {
            btnFetchSignatures.setEnabled(false);
            btnFetchSignatures.setText("가져오는 중...");

            firebaseManager.getClubMembersWithSignatures(clubId, new FirebaseManager.MembersCallback() {
                @Override
                public void onSuccess(List<Member> members) {
                    fetchedSignatures.clear();
                    int signatureCount = 0;

                    for (Member member : members) {
                        if (member.getSignatureUrl() != null && !member.getSignatureUrl().isEmpty()) {
                            fetchedSignatures.add(member.getSignatureUrl());
                            signatureCount++;
                        }
                    }

                    tvSignatureStatus.setText("서명: " + signatureCount + "명 / " + members.size() + "명");
                    btnFetchSignatures.setText("부원 서명 가져오기");
                    btnFetchSignatures.setEnabled(true);

                    if (signatureCount > 0) {
                        Toast.makeText(ClubSettingsActivity.this,
                                signatureCount + "명의 서명을 가져왔습니다", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ClubSettingsActivity.this,
                                "서명이 등록된 부원이 없습니다", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    btnFetchSignatures.setText("부원 서명 가져오기");
                    btnFetchSignatures.setEnabled(true);
                    Toast.makeText(ClubSettingsActivity.this,
                            "서명 가져오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("중앙동아리 신청")
                .setView(dialogView)
                .setPositiveButton("신청", null) // null로 설정하고 나중에 리스너 추가
                .setNegativeButton("취소", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String reason = etReason.getText().toString().trim();
                String budgetStr = etExpectedBudget.getText().toString().trim();

                // 예산 파싱
                long expectedBudget = 0;
                if (!budgetStr.isEmpty()) {
                    try {
                        expectedBudget = Long.parseLong(budgetStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(ClubSettingsActivity.this,
                                "예상 금액을 올바르게 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // 월별 활동 계획 수집
                Map<String, String> monthlyPlans = new HashMap<>();
                monthlyPlans.put("1", etMonth1.getText().toString().trim());
                monthlyPlans.put("2", etMonth2.getText().toString().trim());
                monthlyPlans.put("3", etMonth3.getText().toString().trim());
                monthlyPlans.put("4", etMonth4.getText().toString().trim());
                monthlyPlans.put("5", etMonth5.getText().toString().trim());
                monthlyPlans.put("6", etMonth6.getText().toString().trim());
                monthlyPlans.put("7", etMonth7.getText().toString().trim());
                monthlyPlans.put("8", etMonth8.getText().toString().trim());
                monthlyPlans.put("9", etMonth9.getText().toString().trim());
                monthlyPlans.put("10", etMonth10.getText().toString().trim());
                monthlyPlans.put("11", etMonth11.getText().toString().trim());
                monthlyPlans.put("12", etMonth12.getText().toString().trim());

                dialog.dismiss();
                submitCentralApplication(reason, expectedBudget, monthlyPlans, new ArrayList<>(fetchedSignatures));
            });
        });

        dialog.show();
    }

    private void submitCentralApplication(String reason, long expectedBudget,
                                          Map<String, String> monthlyPlans, List<String> signatures) {
        progressBar.setVisibility(View.VISIBLE);

        String applicantId = firebaseManager.getCurrentUserId();
        String applicantEmail = firebaseManager.getCurrentUser() != null ?
                firebaseManager.getCurrentUser().getEmail() : "알 수 없음";

        int memberCount = currentClub != null ? currentClub.getMemberCount() : 0;
        long daysSinceFounding = currentClub != null ? currentClub.getDaysSinceFounding() : 0;

        CentralClubApplication application = new CentralClubApplication(
                clubId, clubName, applicantId, applicantEmail, memberCount, daysSinceFounding
        );
        application.setReason(reason);
        application.setExpectedBudget(expectedBudget);
        application.setMonthlyPlans(monthlyPlans);
        application.setMemberSignatures(signatures);
        application.setSignatureCount(signatures.size());

        firebaseManager.submitCentralClubApplication(application, new FirebaseManager.CentralApplicationCallback() {
            @Override
            public void onSuccess(CentralClubApplication app) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubSettingsActivity.this, "중앙동아리 신청이 완료되었습니다", Toast.LENGTH_LONG).show();
                updateCentralApplicationUI();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubSettingsActivity.this, "신청 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ========================================
    // Withdrawal Request Methods
    // ========================================

    private void loadWithdrawalRequestStatus() {
        String userId = firebaseManager.getCurrentUserId();
        if (userId == null) {
            updateWithdrawalUI(null);
            return;
        }

        firebaseManager.getPendingWithdrawalRequest(clubId, userId, new FirebaseManager.WithdrawalRequestCallback() {
            @Override
            public void onSuccess(WithdrawalRequest request) {
                pendingWithdrawalRequest = request;
                updateWithdrawalUI(request);
            }

            @Override
            public void onFailure(Exception e) {
                pendingWithdrawalRequest = null;
                updateWithdrawalUI(null);
            }
        });
    }

    private void updateWithdrawalUI(WithdrawalRequest request) {
        if (request != null) {
            // 탈퇴 신청이 있는 경우
            tvWithdrawalDesc.setText("탈퇴 신청이 접수되었습니다.\n관리자의 승인을 기다리고 있습니다.\n\n사유: " + request.getReason());
            btnWithdrawalRequest.setVisibility(View.GONE);
            btnCancelWithdrawal.setVisibility(View.VISIBLE);
        } else {
            // 탈퇴 신청이 없는 경우
            tvWithdrawalDesc.setText("동아리 탈퇴를 신청할 수 있습니다.\n관리자의 승인 후 탈퇴가 완료됩니다.");
            btnWithdrawalRequest.setVisibility(View.VISIBLE);
            btnCancelWithdrawal.setVisibility(View.GONE);
        }
    }

    private void showWithdrawalDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_withdrawal_reason, null);
        EditText etReason = dialogView.findViewById(R.id.etWithdrawalReason);

        new AlertDialog.Builder(this)
                .setTitle("동아리 탈퇴 신청")
                .setView(dialogView)
                .setPositiveButton("신청", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "탈퇴 사유를 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitWithdrawalRequest(reason);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void submitWithdrawalRequest(String reason) {
        progressBar.setVisibility(View.VISIBLE);

        String userId = firebaseManager.getCurrentUserId();
        String userEmail = firebaseManager.getCurrentUser() != null ?
                firebaseManager.getCurrentUser().getEmail() : "알 수 없음";
        String userName = userEmail; // 이메일을 이름 대신 사용

        WithdrawalRequest request = new WithdrawalRequest(clubId, clubName, userId, userEmail, userName, reason);

        firebaseManager.submitWithdrawalRequest(request, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubSettingsActivity.this, "탈퇴 신청이 완료되었습니다", Toast.LENGTH_SHORT).show();
                loadWithdrawalRequestStatus();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ClubSettingsActivity.this, "신청 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelWithdrawalRequest() {
        if (pendingWithdrawalRequest == null) {
            Toast.makeText(this, "취소할 탈퇴 신청이 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("탈퇴 신청 취소")
                .setMessage("탈퇴 신청을 취소하시겠습니까?")
                .setPositiveButton("취소하기", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    firebaseManager.cancelWithdrawalRequest(pendingWithdrawalRequest.getId(), new FirebaseManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ClubSettingsActivity.this, "탈퇴 신청이 취소되었습니다", Toast.LENGTH_SHORT).show();
                            pendingWithdrawalRequest = null;
                            loadWithdrawalRequestStatus();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ClubSettingsActivity.this, "취소 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("아니오", null)
                .show();
    }
}
