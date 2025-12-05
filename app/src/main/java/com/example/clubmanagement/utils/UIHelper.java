package com.example.clubmanagement.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.snackbar.Snackbar;

/**
 * UI 관련 공통 유틸리티 클래스
 * Toast, Dialog, ProgressBar 등의 일관된 처리를 제공합니다.
 */
public class UIHelper {

    private static ProgressDialog progressDialog;

    // ======================== Toast 메서드 ========================

    /**
     * 짧은 Toast 메시지 표시
     */
    public static void showToast(@NonNull Context context, @NonNull String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 긴 Toast 메시지 표시
     */
    public static void showLongToast(@NonNull Context context, @NonNull String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Toast 메시지 표시 (duration 지정)
     */
    public static void showToast(@NonNull Context context, @NonNull String message, int duration) {
        Toast.makeText(context, message, duration).show();
    }

    // ======================== Snackbar 메서드 ========================

    /**
     * Snackbar 표시
     */
    public static void showSnackbar(@NonNull View view, @NonNull String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * 액션이 있는 Snackbar 표시
     */
    public static void showSnackbarWithAction(@NonNull View view, @NonNull String message,
                                               @NonNull String actionText, @NonNull View.OnClickListener action) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction(actionText, action)
                .show();
    }

    // ======================== AlertDialog 메서드 ========================

    /**
     * 간단한 알림 다이얼로그 표시
     */
    public static void showAlert(@NonNull Context context, @NonNull String title, @NonNull String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();
    }

    /**
     * 확인/취소 다이얼로그 표시
     */
    public static void showConfirmDialog(@NonNull Context context, @NonNull String title,
                                          @NonNull String message, @NonNull Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", (dialog, which) -> onConfirm.run())
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 확인/취소 다이얼로그 표시 (커스텀 버튼 텍스트)
     */
    public static void showConfirmDialog(@NonNull Context context, @NonNull String title,
                                          @NonNull String message,
                                          @NonNull String positiveText, @NonNull String negativeText,
                                          @NonNull Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, (dialog, which) -> onConfirm.run())
                .setNegativeButton(negativeText, null)
                .show();
    }

    /**
     * 확인/취소 다이얼로그 표시 (취소 콜백 포함)
     */
    public static void showConfirmDialog(@NonNull Context context, @NonNull String title,
                                          @NonNull String message,
                                          @NonNull Runnable onConfirm, @Nullable Runnable onCancel) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", (dialog, which) -> onConfirm.run())
                .setNegativeButton("취소", (dialog, which) -> {
                    if (onCancel != null) onCancel.run();
                })
                .show();
    }

    /**
     * 삭제 확인 다이얼로그 (빨간색 강조)
     */
    public static void showDeleteConfirmDialog(@NonNull Context context, @NonNull String itemName,
                                                @NonNull Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle("삭제 확인")
                .setMessage(itemName + "을(를) 삭제하시겠습니까?\n이 작업은 취소할 수 없습니다.")
                .setPositiveButton("삭제", (dialog, which) -> onConfirm.run())
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 입력 다이얼로그 표시
     */
    public static void showInputDialog(@NonNull Context context, @NonNull String title,
                                        @Nullable String hint, @Nullable String defaultValue,
                                        @NonNull InputCallback callback) {
        EditText editText = new EditText(context);
        editText.setHint(hint);
        if (defaultValue != null) {
            editText.setText(defaultValue);
            editText.setSelection(defaultValue.length());
        }

        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        editText.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(editText)
                .setPositiveButton("확인", (dialog, which) -> {
                    String input = editText.getText().toString().trim();
                    callback.onInput(input);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 선택 다이얼로그 표시
     */
    public static void showSelectionDialog(@NonNull Context context, @NonNull String title,
                                            @NonNull String[] items, @NonNull SelectionCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(items, (dialog, which) -> callback.onSelected(which, items[which]))
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 단일 선택 다이얼로그 (라디오 버튼)
     */
    public static void showSingleChoiceDialog(@NonNull Context context, @NonNull String title,
                                               @NonNull String[] items, int checkedItem,
                                               @NonNull SelectionCallback callback) {
        final int[] selectedIndex = {checkedItem};

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    selectedIndex[0] = which;
                })
                .setPositiveButton("확인", (dialog, which) -> {
                    callback.onSelected(selectedIndex[0], items[selectedIndex[0]]);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // ======================== ProgressDialog 메서드 ========================

    /**
     * 로딩 다이얼로그 표시
     */
    public static void showLoading(@NonNull Context context) {
        showLoading(context, "로딩 중...");
    }

    /**
     * 로딩 다이얼로그 표시 (커스텀 메시지)
     */
    public static void showLoading(@NonNull Context context, @NonNull String message) {
        hideLoading();
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    /**
     * 로딩 다이얼로그 숨기기
     */
    public static void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                // Context가 이미 destroy된 경우 무시
            }
            progressDialog = null;
        }
    }

    /**
     * 로딩 다이얼로그 메시지 업데이트
     */
    public static void updateLoadingMessage(@NonNull String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
        }
    }

    // ======================== ProgressBar 메서드 ========================

    /**
     * ProgressBar 표시
     */
    public static void showProgress(@Nullable ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * ProgressBar 숨기기
     */
    public static void hideProgress(@Nullable ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    // ======================== 키보드 메서드 ========================

    /**
     * 키보드 숨기기
     */
    public static void hideKeyboard(@NonNull Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    /**
     * 키보드 표시
     */
    public static void showKeyboard(@NonNull Context context, @NonNull View view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // ======================== 콜백 인터페이스 ========================

    /**
     * 입력 다이얼로그 콜백
     */
    public interface InputCallback {
        void onInput(String input);
    }

    /**
     * 선택 다이얼로그 콜백
     */
    public interface SelectionCallback {
        void onSelected(int index, String item);
    }
}
