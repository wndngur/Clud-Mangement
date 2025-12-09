package com.example.clubmanagement.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.BaseActivity;
import com.example.clubmanagement.R;
import com.example.clubmanagement.adapters.BudgetTransactionAdapter;
import com.example.clubmanagement.models.BudgetTransaction;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.models.UserData;
import com.example.clubmanagement.utils.FirebaseManager;
import com.example.clubmanagement.SettingsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BudgetHistoryActivity extends BaseActivity {

    private FirebaseManager firebaseManager;
    private String clubId;
    private String clubName;
    private Club currentClub;
    private boolean isAdmin = false;

    // UI Components
    private ImageView ivBack;
    private TextView tvCurrentBalance;
    private TextView tvTotalBudget;
    private TextView tvTotalExpense;
    private RecyclerView rvTransactions;
    private LinearLayout llEmptyState;
    private FloatingActionButton fabAddExpense;
    private ProgressBar progressBar;

    private BudgetTransactionAdapter adapter;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // For receipt scanning
    private Uri selectedReceiptUri;
    private ImageView dialogReceiptPreview;
    private EditText dialogAmountInput;
    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_budget_history);

        firebaseManager = FirebaseManager.getInstance();

        // Get club info from intent
        clubId = getIntent().getStringExtra("club_id");
        clubName = getIntent().getStringExtra("club_name");

        if (clubId == null || clubId.isEmpty()) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize ML Kit Text Recognizer
        textRecognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());

        initViews();
        setupImagePickerLauncher();
        setupRecyclerView();
        checkAdminStatus();
        loadData();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        tvTotalBudget = findViewById(R.id.tvTotalBudget);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        rvTransactions = findViewById(R.id.rvTransactions);
        llEmptyState = findViewById(R.id.llEmptyState);
        fabAddExpense = findViewById(R.id.fabAddExpense);
        progressBar = findViewById(R.id.progressBar);

        ivBack.setOnClickListener(v -> finish());
        fabAddExpense.setOnClickListener(v -> {
            if (isAdmin) {
                showAddExpenseDialog();
            } else {
                Toast.makeText(this, "관리자만 거래를 추가할 수 있습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            selectedReceiptUri = imageUri;
                            if (dialogReceiptPreview != null) {
                                dialogReceiptPreview.setVisibility(View.VISIBLE);
                                Glide.with(this).load(imageUri).into(dialogReceiptPreview);
                            }
                            // Scan receipt with OCR
                            scanReceiptWithOCR(imageUri);
                        }
                    }
                }
        );
    }

    private void setupRecyclerView() {
        adapter = new BudgetTransactionAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        adapter.setOnTransactionClickListener(transaction -> {
            showTransactionDetailDialog(transaction);
        });

        adapter.setOnTransactionLongClickListener(transaction -> {
            if (isAdmin) {
                showEditDeleteDialog(transaction);
            }
        });
    }

    private void checkAdminStatus() {
        // Check super admin mode from SharedPreferences
        boolean isSuperAdminMode = SettingsActivity.isSuperAdminMode(this);

        // Check club admin mode from SharedPreferences
        boolean isClubAdminMode = ClubSettingsActivity.isClubAdminMode(this);

        // If super admin mode is on, allow access
        if (isSuperAdminMode) {
            isAdmin = true;
            return;
        }

        // If club admin mode is on, allow access
        if (isClubAdminMode) {
            isAdmin = true;
        }
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);

        // Load club info
        firebaseManager.getClub(clubId, new FirebaseManager.ClubCallback() {
            @Override
            public void onSuccess(Club club) {
                currentClub = club;
                updateSummaryUI();
                loadTransactions();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BudgetHistoryActivity.this, "데이터 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTransactions() {
        firebaseManager.getBudgetTransactions(clubId, new FirebaseManager.BudgetTransactionListCallback() {
            @Override
            public void onSuccess(List<BudgetTransaction> transactions) {
                progressBar.setVisibility(View.GONE);
                adapter.setTransactions(transactions);

                if (transactions == null || transactions.isEmpty()) {
                    llEmptyState.setVisibility(View.VISIBLE);
                    rvTransactions.setVisibility(View.GONE);
                } else {
                    llEmptyState.setVisibility(View.GONE);
                    rvTransactions.setVisibility(View.VISIBLE);
                }

                // Calculate total expense
                long totalExpense = 0;
                if (transactions != null) {
                    for (BudgetTransaction t : transactions) {
                        if (t.isExpense()) {
                            totalExpense += t.getAmount();
                        }
                    }
                }
                updateTotalExpense(totalExpense);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                llEmptyState.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
            }
        });
    }

    private void updateSummaryUI() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

        if (currentClub != null) {
            tvCurrentBalance.setText(numberFormat.format(currentClub.getCurrentBudget()) + "원");
            tvTotalBudget.setText(numberFormat.format(currentClub.getTotalBudget()) + "원");
        } else {
            tvCurrentBalance.setText("0원");
            tvTotalBudget.setText("0원");
        }
    }

    private void updateTotalExpense(long totalExpense) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
        tvTotalExpense.setText(numberFormat.format(totalExpense) + "원");
    }

    private void showAddExpenseDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);

        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        dialogAmountInput = dialogView.findViewById(R.id.etAmount);
        RadioGroup rgType = dialogView.findViewById(R.id.rgTransactionType);
        MaterialButton btnScanReceipt = dialogView.findViewById(R.id.btnScanReceipt);
        dialogReceiptPreview = dialogView.findViewById(R.id.ivReceiptPreview);

        selectedReceiptUri = null;

        btnScanReceipt.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        new AlertDialog.Builder(this)
                .setTitle("거래 추가")
                .setView(dialogView)
                .setPositiveButton("저장", (dialog, which) -> {
                    String description = etDescription.getText().toString().trim();
                    String amountStr = dialogAmountInput.getText().toString().trim();
                    int selectedTypeId = rgType.getCheckedRadioButtonId();

                    if (description.isEmpty()) {
                        Toast.makeText(this, "내역을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "금액을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        long amount = Long.parseLong(amountStr.replaceAll("[^0-9]", ""));
                        String type = (selectedTypeId == R.id.rbIncome) ?
                                BudgetTransaction.TYPE_INCOME : BudgetTransaction.TYPE_EXPENSE;

                        saveTransaction(type, amount, description);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "올바른 금액을 입력해주세요", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void scanReceiptWithOCR(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);

        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);

            textRecognizer.process(image)
                    .addOnSuccessListener(text -> {
                        progressBar.setVisibility(View.GONE);
                        String recognizedText = text.getText();

                        // 인식된 텍스트가 없는 경우
                        if (recognizedText == null || recognizedText.trim().isEmpty()) {
                            Toast.makeText(this, "텍스트를 인식하지 못했습니다. 다른 이미지를 시도해주세요.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Extract amount from receipt
                        long extractedAmount = extractAmountFromText(recognizedText);

                        if (extractedAmount > 0 && dialogAmountInput != null) {
                            dialogAmountInput.setText(String.valueOf(extractedAmount));
                            Toast.makeText(this, "영수증에서 " + String.format("%,d", extractedAmount) + "원 인식됨", Toast.LENGTH_SHORT).show();
                        } else {
                            // 인식은 됐지만 금액을 찾지 못한 경우 - 인식된 텍스트 일부 표시
                            String preview = recognizedText.length() > 100 ?
                                recognizedText.substring(0, 100) + "..." : recognizedText;
                            Toast.makeText(this, "금액을 찾지 못했습니다.\n인식된 내용: " + preview, Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "영수증 인식 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
        }
    }

    private long extractAmountFromText(String text) {
        // 텍스트 전처리 - 공백 정규화
        text = text.replaceAll("\\s+", " ");

        // 우선순위가 높은 키워드 (결제 총액 관련)
        String[] highPriorityKeywords = {
            "결제금액", "결제 금액", "총결제", "총 결제", "승인금액", "승인 금액",
            "카드결제", "카드 결제", "실결제", "실 결제", "최종금액", "최종 금액",
            "합계금액", "합계 금액", "총합계", "총 합계", "받을금액", "받을 금액",
            "TOTAL", "Total", "total", "합 계", "합계"
        };

        // 일반 키워드
        String[] normalKeywords = {
            "총액", "총 액", "금액", "판매금액", "판매 금액", "매출", "청구금액",
            "결제", "지불", "Payment", "Amount", "SUM", "Sum"
        };

        String[] lines = text.split("\n");

        // 1단계: 우선순위 높은 키워드로 검색
        for (String keyword : highPriorityKeywords) {
            for (String line : lines) {
                if (line.contains(keyword)) {
                    long amount = extractNumberFromLine(line);
                    if (amount >= 100 && amount <= 100000000) {
                        return amount;
                    }
                }
            }
        }

        // 2단계: 일반 키워드로 검색
        for (String keyword : normalKeywords) {
            for (String line : lines) {
                if (line.contains(keyword)) {
                    long amount = extractNumberFromLine(line);
                    if (amount >= 100 && amount <= 100000000) {
                        return amount;
                    }
                }
            }
        }

        // 3단계: "원" 앞의 숫자 찾기
        Pattern wonPattern = Pattern.compile("([0-9,. ]+)\\s*원");
        Matcher wonMatcher = wonPattern.matcher(text);
        long maxWonAmount = 0;
        while (wonMatcher.find()) {
            try {
                String numStr = wonMatcher.group(1).replaceAll("[,. ]", "");
                long num = Long.parseLong(numStr);
                if (num >= 100 && num <= 100000000 && num > maxWonAmount) {
                    maxWonAmount = num;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (maxWonAmount > 0) {
            return maxWonAmount;
        }

        // 4단계: 가장 큰 숫자 찾기 (영수증 하단에 총액이 있는 경우가 많음)
        long maxAmount = 0;
        // 영수증 하단부터 검색 (총액이 보통 하단에 있음)
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            long lineMax = extractNumberFromLine(line);
            if (lineMax >= 1000 && lineMax <= 100000000 && lineMax > maxAmount) {
                maxAmount = lineMax;
            }
        }

        // 최소 금액 기준 (1000원 이상)
        if (maxAmount >= 1000) {
            return maxAmount;
        }

        // 5단계: 전체에서 가장 큰 숫자 (최후의 수단)
        Pattern pattern = Pattern.compile("[0-9][0-9,. ]*[0-9]|[0-9]+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                String numStr = matcher.group().replaceAll("[,. ]", "");
                long num = Long.parseLong(numStr);
                if (num >= 100 && num <= 100000000 && num > maxAmount) {
                    maxAmount = num;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return maxAmount;
    }

    private long extractNumberFromLine(String line) {
        // "원" 기호 앞의 숫자 우선 추출
        Pattern wonPattern = Pattern.compile("([0-9,. ]+)\\s*원");
        Matcher wonMatcher = wonPattern.matcher(line);
        if (wonMatcher.find()) {
            try {
                String numStr = wonMatcher.group(1).replaceAll("[,. ]", "");
                long num = Long.parseLong(numStr);
                if (num > 0) {
                    return num;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 일반 숫자 패턴 (쉼표, 점, 공백 포함 가능)
        Pattern pattern = Pattern.compile("[0-9][0-9,. ]*[0-9]|[0-9]+");
        Matcher matcher = pattern.matcher(line);

        long maxNum = 0;
        while (matcher.find()) {
            try {
                String numStr = matcher.group().replaceAll("[,. ]", "");
                long num = Long.parseLong(numStr);
                if (num > maxNum) {
                    maxNum = num;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return maxNum;
    }

    private void saveTransaction(String type, long amount, String description) {
        if (currentClub == null) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Calculate new balance
        long newBalance;
        if (BudgetTransaction.TYPE_EXPENSE.equals(type)) {
            newBalance = currentClub.getCurrentBudget() - amount;
            if (newBalance < 0) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "잔액이 부족합니다", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            newBalance = currentClub.getCurrentBudget() + amount;
        }

        // Create transaction
        BudgetTransaction transaction = new BudgetTransaction(clubId, type, amount, description);
        transaction.setBalanceAfter(newBalance);

        // If receipt image exists, upload it first
        if (selectedReceiptUri != null) {
            uploadReceiptAndSaveTransaction(transaction, newBalance);
        } else {
            saveTransactionToFirebase(transaction, newBalance, null);
        }
    }

    private void uploadReceiptAndSaveTransaction(BudgetTransaction transaction, long newBalance) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedReceiptUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();

            firebaseManager.uploadReceiptImage(clubId, imageData, new FirebaseManager.SignatureCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    saveTransactionToFirebase(transaction, newBalance, downloadUrl);
                }

                @Override
                public void onFailure(Exception e) {
                    // Save without receipt image
                    saveTransactionToFirebase(transaction, newBalance, null);
                }
            });
        } catch (IOException e) {
            saveTransactionToFirebase(transaction, newBalance, null);
        }
    }

    private void saveTransactionToFirebase(BudgetTransaction transaction, long newBalance, String receiptUrl) {
        if (receiptUrl != null) {
            transaction.setReceiptImageUrl(receiptUrl);
        }

        firebaseManager.saveBudgetTransaction(transaction, newBalance, new FirebaseManager.BudgetTransactionCallback() {
            @Override
            public void onSuccess(BudgetTransaction savedTransaction) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BudgetHistoryActivity.this, "저장 완료", Toast.LENGTH_SHORT).show();

                // Update local club data
                currentClub.setCurrentBudget(newBalance);
                updateSummaryUI();

                // Reload transactions
                loadTransactions();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BudgetHistoryActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTransactionDetailDialog(BudgetTransaction transaction) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_transaction_detail, null);

        TextView tvType = dialogView.findViewById(R.id.tvDetailType);
        TextView tvAmount = dialogView.findViewById(R.id.tvDetailAmount);
        TextView tvDescription = dialogView.findViewById(R.id.tvDetailDescription);
        TextView tvDate = dialogView.findViewById(R.id.tvDetailDate);
        TextView tvBalanceAfter = dialogView.findViewById(R.id.tvDetailBalanceAfter);
        ImageView ivReceipt = dialogView.findViewById(R.id.ivDetailReceipt);

        tvType.setText(transaction.getTypeDisplayName());
        tvAmount.setText(transaction.getFormattedAmount());
        tvDescription.setText(transaction.getDescription());
        tvDate.setText(transaction.getFormattedDate());

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
        tvBalanceAfter.setText("거래 후 잔액: " + numberFormat.format(transaction.getBalanceAfter()) + "원");

        // Set amount color
        if (transaction.isExpense()) {
            tvAmount.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
        } else {
            tvAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
        }

        // Show receipt image if exists
        if (transaction.getReceiptImageUrl() != null && !transaction.getReceiptImageUrl().isEmpty()) {
            ivReceipt.setVisibility(View.VISIBLE);
            Glide.with(this).load(transaction.getReceiptImageUrl()).into(ivReceipt);
        } else {
            ivReceipt.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(this)
                .setTitle("거래 상세")
                .setView(dialogView)
                .setPositiveButton("확인", null)
                .show();
    }

    private void showEditDeleteDialog(BudgetTransaction transaction) {
        String[] options = {"수정", "삭제"};

        new AlertDialog.Builder(this)
                .setTitle("거래 관리")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 수정
                        showEditTransactionDialog(transaction);
                    } else {
                        // 삭제
                        confirmDeleteTransaction(transaction);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showEditTransactionDialog(BudgetTransaction transaction) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);

        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        dialogAmountInput = dialogView.findViewById(R.id.etAmount);
        RadioGroup rgType = dialogView.findViewById(R.id.rgTransactionType);
        MaterialButton btnScanReceipt = dialogView.findViewById(R.id.btnScanReceipt);
        dialogReceiptPreview = dialogView.findViewById(R.id.ivReceiptPreview);

        // 기존 값 설정
        etDescription.setText(transaction.getDescription());
        dialogAmountInput.setText(String.valueOf(transaction.getAmount()));

        if (transaction.isIncome()) {
            rgType.check(R.id.rbIncome);
        } else {
            rgType.check(R.id.rbExpense);
        }

        // 기존 영수증 이미지 표시
        if (transaction.getReceiptImageUrl() != null && !transaction.getReceiptImageUrl().isEmpty()) {
            dialogReceiptPreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(transaction.getReceiptImageUrl()).into(dialogReceiptPreview);
        }

        selectedReceiptUri = null;

        btnScanReceipt.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        new AlertDialog.Builder(this)
                .setTitle("거래 수정")
                .setView(dialogView)
                .setPositiveButton("저장", (dialog, which) -> {
                    String description = etDescription.getText().toString().trim();
                    String amountStr = dialogAmountInput.getText().toString().trim();
                    int selectedTypeId = rgType.getCheckedRadioButtonId();

                    if (description.isEmpty()) {
                        Toast.makeText(this, "내역을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "금액을 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        long newAmount = Long.parseLong(amountStr.replaceAll("[^0-9]", ""));
                        String newType = (selectedTypeId == R.id.rbIncome) ?
                                BudgetTransaction.TYPE_INCOME : BudgetTransaction.TYPE_EXPENSE;

                        updateTransaction(transaction, newType, newAmount, description);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "올바른 금액을 입력해주세요", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void updateTransaction(BudgetTransaction transaction, String newType, long newAmount, String newDescription) {
        if (currentClub == null) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // 잔액 재계산
        long oldEffect = transaction.isExpense() ? -transaction.getAmount() : transaction.getAmount();
        long newEffect = BudgetTransaction.TYPE_EXPENSE.equals(newType) ? -newAmount : newAmount;
        long balanceDiff = newEffect - oldEffect;
        long newClubBalance = currentClub.getCurrentBudget() + balanceDiff;

        if (newClubBalance < 0) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "잔액이 부족합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 거래 정보 업데이트
        transaction.setType(newType);
        transaction.setAmount(newAmount);
        transaction.setDescription(newDescription);
        transaction.setBalanceAfter(newClubBalance);

        // 새 영수증 이미지가 있으면 업로드
        if (selectedReceiptUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedReceiptUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageData = baos.toByteArray();

                firebaseManager.uploadReceiptImage(clubId, imageData, new FirebaseManager.SignatureCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        transaction.setReceiptImageUrl(downloadUrl);
                        saveUpdatedTransaction(transaction, newClubBalance);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        saveUpdatedTransaction(transaction, newClubBalance);
                    }
                });
            } catch (IOException e) {
                saveUpdatedTransaction(transaction, newClubBalance);
            }
        } else {
            saveUpdatedTransaction(transaction, newClubBalance);
        }
    }

    private void saveUpdatedTransaction(BudgetTransaction transaction, long newClubBalance) {
        firebaseManager.updateBudgetTransaction(transaction, newClubBalance, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BudgetHistoryActivity.this, "수정 완료", Toast.LENGTH_SHORT).show();

                currentClub.setCurrentBudget(newClubBalance);
                updateSummaryUI();
                loadTransactions();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BudgetHistoryActivity.this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteTransaction(BudgetTransaction transaction) {
        new AlertDialog.Builder(this)
                .setTitle("거래 삭제")
                .setMessage("이 거래를 삭제하시겠습니까?\n삭제 후에는 되돌릴 수 없습니다.")
                .setPositiveButton("삭제", (dialog, which) -> deleteTransaction(transaction))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteTransaction(BudgetTransaction transaction) {
        if (currentClub == null) {
            Toast.makeText(this, "동아리 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // 잔액 복원
        long balanceAdjustment = transaction.isExpense() ? transaction.getAmount() : -transaction.getAmount();
        long newClubBalance = currentClub.getCurrentBudget() + balanceAdjustment;

        firebaseManager.deleteBudgetTransaction(clubId, transaction.getId(), newClubBalance, new FirebaseManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BudgetHistoryActivity.this, "삭제 완료", Toast.LENGTH_SHORT).show();

                currentClub.setCurrentBudget(newClubBalance);
                updateSummaryUI();
                loadTransactions();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(BudgetHistoryActivity.this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}
