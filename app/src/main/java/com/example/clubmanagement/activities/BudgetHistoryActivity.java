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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.clubmanagement.R;
import com.example.clubmanagement.adapters.BudgetTransactionAdapter;
import com.example.clubmanagement.models.BudgetTransaction;
import com.example.clubmanagement.models.Club;
import com.example.clubmanagement.models.UserData;
import com.example.clubmanagement.utils.FirebaseManager;
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

public class BudgetHistoryActivity extends AppCompatActivity {

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
        fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());
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
    }

    private void checkAdminStatus() {
        firebaseManager.getUserData(firebaseManager.getCurrentUserId(), new FirebaseManager.UserDataCallback() {
            @Override
            public void onSuccess(UserData userData) {
                if (userData != null) {
                    boolean isSuperAdmin = userData.isSuperAdmin();
                    boolean isThisClubAdmin = userData.isClubAdmin() &&
                            userData.getClubId() != null &&
                            userData.getClubId().equals(clubId);

                    isAdmin = isSuperAdmin || isThisClubAdmin;

                    if (isAdmin) {
                        fabAddExpense.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                isAdmin = false;
            }
        });
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

                        // Extract amount from receipt
                        long extractedAmount = extractAmountFromText(recognizedText);

                        if (extractedAmount > 0 && dialogAmountInput != null) {
                            dialogAmountInput.setText(String.valueOf(extractedAmount));
                            Toast.makeText(this, "영수증에서 " + extractedAmount + "원 인식됨", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "금액을 인식하지 못했습니다. 직접 입력해주세요.", Toast.LENGTH_SHORT).show();
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
        // Common patterns in Korean receipts for total amount
        // 합계, 총액, 결제금액, 총 결제, 카드결제, 받을금액 등
        String[] keywords = {"합계", "총액", "결제금액", "총 결제", "카드결제", "받을금액", "판매금액", "TOTAL", "Total"};

        String[] lines = text.split("\n");

        for (String keyword : keywords) {
            for (String line : lines) {
                if (line.contains(keyword)) {
                    // Extract number from this line
                    long amount = extractNumberFromLine(line);
                    if (amount > 0) {
                        return amount;
                    }
                }
            }
        }

        // If no keyword found, find the largest number (likely the total)
        long maxAmount = 0;
        Pattern pattern = Pattern.compile("[0-9,]+");

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                try {
                    String numStr = matcher.group().replaceAll(",", "");
                    long num = Long.parseLong(numStr);
                    // Filter reasonable amounts (100 ~ 10,000,000)
                    if (num >= 100 && num <= 10000000 && num > maxAmount) {
                        maxAmount = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return maxAmount;
    }

    private long extractNumberFromLine(String line) {
        Pattern pattern = Pattern.compile("[0-9,]+");
        Matcher matcher = pattern.matcher(line);

        long maxNum = 0;
        while (matcher.find()) {
            try {
                String numStr = matcher.group().replaceAll(",", "");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}
