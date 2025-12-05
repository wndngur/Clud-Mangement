package com.example.clubmanagement.utils;

import android.util.Log;

import com.example.clubmanagement.models.BudgetTransaction;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 예산/공금 관리 매니저 클래스
 * 예산 거래 내역 CRUD 및 잔액 관리를 담당합니다.
 */
public class BudgetManager {
    private static final String TAG = "BudgetManager";

    private static BudgetManager instance;
    private final FirebaseFirestore db;

    private BudgetManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized BudgetManager getInstance() {
        if (instance == null) {
            instance = new BudgetManager();
        }
        return instance;
    }

    // ======================== 콜백 인터페이스 ========================

    public interface TransactionCallback {
        void onSuccess(BudgetTransaction transaction);
        void onFailure(Exception e);
    }

    public interface TransactionListCallback {
        void onSuccess(List<BudgetTransaction> transactions);
        void onFailure(Exception e);
    }

    public interface BalanceCallback {
        void onSuccess(long balance);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // ======================== 거래 내역 조회 ========================

    /**
     * 동아리 거래 내역 조회 (최신순)
     */
    public void getTransactions(String clubId, TransactionListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("budgetTransactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<BudgetTransaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        BudgetTransaction transaction = doc.toObject(BudgetTransaction.class);
                        if (transaction != null) {
                            transaction.setId(doc.getId());
                            transactions.add(transaction);
                        }
                    }
                    callback.onSuccess(transactions);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 특정 기간 거래 내역 조회
     */
    public void getTransactionsByPeriod(String clubId, Timestamp startDate, Timestamp endDate,
                                         TransactionListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("budgetTransactions")
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .whereLessThanOrEqualTo("createdAt", endDate)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<BudgetTransaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        BudgetTransaction transaction = doc.toObject(BudgetTransaction.class);
                        if (transaction != null) {
                            transaction.setId(doc.getId());
                            transactions.add(transaction);
                        }
                    }
                    callback.onSuccess(transactions);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 거래 유형별 조회
     */
    public void getTransactionsByType(String clubId, String type, TransactionListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("budgetTransactions")
                .whereEqualTo("type", type)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<BudgetTransaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        BudgetTransaction transaction = doc.toObject(BudgetTransaction.class);
                        if (transaction != null) {
                            transaction.setId(doc.getId());
                            transactions.add(transaction);
                        }
                    }
                    callback.onSuccess(transactions);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 최근 N개 거래 내역 조회
     */
    public void getRecentTransactions(String clubId, int limit, TransactionListCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .collection("budgetTransactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<BudgetTransaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        BudgetTransaction transaction = doc.toObject(BudgetTransaction.class);
                        if (transaction != null) {
                            transaction.setId(doc.getId());
                            transactions.add(transaction);
                        }
                    }
                    callback.onSuccess(transactions);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 잔액 조회 ========================

    /**
     * 현재 잔액 조회
     */
    public void getCurrentBalance(String clubId, BalanceCallback callback) {
        db.collection("clubs")
                .document(clubId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long balance = doc.getLong("currentBudget");
                        callback.onSuccess(balance != null ? balance : 0L);
                    } else {
                        callback.onSuccess(0L);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 거래 추가 ========================

    /**
     * 거래 추가 (수입/지출/조정)
     */
    public void addTransaction(BudgetTransaction transaction, long newBalance, TransactionCallback callback) {
        String clubId = transaction.getClubId();

        // 거래 ID 생성
        String transactionId = db.collection("clubs")
                .document(clubId)
                .collection("budgetTransactions")
                .document()
                .getId();

        transaction.setId(transactionId);
        transaction.setCreatedAt(Timestamp.now());
        transaction.setBalanceAfter(newBalance);

        // 배치 작업으로 거래 추가 + 잔액 업데이트
        WriteBatch batch = db.batch();

        // 거래 문서 추가
        DocumentReference transactionRef = db.collection("clubs")
                .document(clubId)
                .collection("budgetTransactions")
                .document(transactionId);
        batch.set(transactionRef, createTransactionMap(transaction));

        // 동아리 잔액 업데이트
        DocumentReference clubRef = db.collection("clubs").document(clubId);
        batch.update(clubRef, "currentBudget", newBalance);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(transaction))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * 수입 추가 (편의 메서드)
     */
    public void addIncome(String clubId, long amount, String description, String userId, String userName,
                          long currentBalance, TransactionCallback callback) {
        BudgetTransaction transaction = new BudgetTransaction(clubId, BudgetTransaction.TYPE_INCOME, amount, description);
        transaction.setCreatedBy(userId);
        transaction.setCreatedByName(userName);

        long newBalance = currentBalance + amount;
        addTransaction(transaction, newBalance, callback);
    }

    /**
     * 지출 추가 (편의 메서드)
     */
    public void addExpense(String clubId, long amount, String description, String userId, String userName,
                           long currentBalance, TransactionCallback callback) {
        BudgetTransaction transaction = new BudgetTransaction(clubId, BudgetTransaction.TYPE_EXPENSE, amount, description);
        transaction.setCreatedBy(userId);
        transaction.setCreatedByName(userName);

        long newBalance = currentBalance - amount;
        addTransaction(transaction, newBalance, callback);
    }

    /**
     * 잔액 조정 (편의 메서드)
     */
    public void adjustBalance(String clubId, long newBalance, String description, String userId, String userName,
                              long currentBalance, TransactionCallback callback) {
        long difference = newBalance - currentBalance;
        BudgetTransaction transaction = new BudgetTransaction(clubId, BudgetTransaction.TYPE_ADJUSTMENT, Math.abs(difference), description);
        transaction.setCreatedBy(userId);
        transaction.setCreatedByName(userName);

        addTransaction(transaction, newBalance, callback);
    }

    // ======================== 거래 수정 ========================

    /**
     * 거래 수정
     */
    public void updateTransaction(BudgetTransaction transaction, long newBalance, SimpleCallback callback) {
        String clubId = transaction.getClubId();

        WriteBatch batch = db.batch();

        // 거래 업데이트
        DocumentReference transactionRef = db.collection("clubs")
                .document(clubId)
                .collection("budgetTransactions")
                .document(transaction.getId());
        batch.set(transactionRef, createTransactionMap(transaction));

        // 잔액 업데이트
        DocumentReference clubRef = db.collection("clubs").document(clubId);
        batch.update(clubRef, "currentBudget", newBalance);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 거래 삭제 ========================

    /**
     * 거래 삭제
     */
    public void deleteTransaction(String clubId, String transactionId, long newBalance, SimpleCallback callback) {
        WriteBatch batch = db.batch();

        // 거래 삭제
        DocumentReference transactionRef = db.collection("clubs")
                .document(clubId)
                .collection("budgetTransactions")
                .document(transactionId);
        batch.delete(transactionRef);

        // 잔액 업데이트
        DocumentReference clubRef = db.collection("clubs").document(clubId);
        batch.update(clubRef, "currentBudget", newBalance);

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== 통계 ========================

    /**
     * 총 수입 계산
     */
    public void calculateTotalIncome(String clubId, BalanceCallback callback) {
        getTransactionsByType(clubId, BudgetTransaction.TYPE_INCOME, new TransactionListCallback() {
            @Override
            public void onSuccess(List<BudgetTransaction> transactions) {
                long total = 0;
                for (BudgetTransaction t : transactions) {
                    total += t.getAmount();
                }
                callback.onSuccess(total);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * 총 지출 계산
     */
    public void calculateTotalExpense(String clubId, BalanceCallback callback) {
        getTransactionsByType(clubId, BudgetTransaction.TYPE_EXPENSE, new TransactionListCallback() {
            @Override
            public void onSuccess(List<BudgetTransaction> transactions) {
                long total = 0;
                for (BudgetTransaction t : transactions) {
                    total += t.getAmount();
                }
                callback.onSuccess(total);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    // ======================== 유틸리티 ========================

    private Map<String, Object> createTransactionMap(BudgetTransaction transaction) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", transaction.getId());
        data.put("clubId", transaction.getClubId());
        data.put("type", transaction.getType());
        data.put("amount", transaction.getAmount());
        data.put("description", transaction.getDescription());
        data.put("receiptImageUrl", transaction.getReceiptImageUrl());
        data.put("createdBy", transaction.getCreatedBy());
        data.put("createdByName", transaction.getCreatedByName());
        data.put("createdAt", transaction.getCreatedAt());
        data.put("balanceAfter", transaction.getBalanceAfter());
        return data;
    }
}
