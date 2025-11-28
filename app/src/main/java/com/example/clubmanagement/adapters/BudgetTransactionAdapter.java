package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.BudgetTransaction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetTransactionAdapter extends RecyclerView.Adapter<BudgetTransactionAdapter.ViewHolder> {

    private List<BudgetTransaction> transactions = new ArrayList<>();
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(BudgetTransaction transaction);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    public void setTransactions(List<BudgetTransaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BudgetTransaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTypeIcon;
        private final TextView tvDescription;
        private final TextView tvDate;
        private final TextView tvCreatedBy;
        private final TextView tvAmount;
        private final TextView tvBalanceAfter;
        private final ImageView ivReceiptIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTypeIcon = itemView.findViewById(R.id.tvTypeIcon);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCreatedBy = itemView.findViewById(R.id.tvCreatedBy);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvBalanceAfter = itemView.findViewById(R.id.tvBalanceAfter);
            ivReceiptIndicator = itemView.findViewById(R.id.ivReceiptIndicator);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTransactionClick(transactions.get(pos));
                }
            });
        }

        public void bind(BudgetTransaction transaction) {
            // Icon based on type
            if (transaction.isExpense()) {
                tvTypeIcon.setText("💸");
            } else if (transaction.isIncome()) {
                tvTypeIcon.setText("💰");
            } else {
                tvTypeIcon.setText("📝");
            }

            // Description
            tvDescription.setText(transaction.getDescription());

            // Date
            tvDate.setText(transaction.getFormattedDate());

            // Created by
            String createdByName = transaction.getCreatedByName();
            if (createdByName != null && !createdByName.isEmpty()) {
                tvCreatedBy.setText(" · " + createdByName);
                tvCreatedBy.setVisibility(View.VISIBLE);
            } else {
                tvCreatedBy.setVisibility(View.GONE);
            }

            // Amount with color
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
            String amountText;
            int amountColor;

            if (transaction.isExpense()) {
                amountText = "-" + numberFormat.format(transaction.getAmount()) + "원";
                amountColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark);
            } else if (transaction.isIncome()) {
                amountText = "+" + numberFormat.format(transaction.getAmount()) + "원";
                amountColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark);
            } else {
                amountText = numberFormat.format(transaction.getAmount()) + "원";
                amountColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_blue_dark);
            }

            tvAmount.setText(amountText);
            tvAmount.setTextColor(amountColor);

            // Balance after
            String balanceText = "잔액: " + numberFormat.format(transaction.getBalanceAfter()) + "원";
            tvBalanceAfter.setText(balanceText);

            // Receipt indicator
            if (transaction.getReceiptImageUrl() != null && !transaction.getReceiptImageUrl().isEmpty()) {
                ivReceiptIndicator.setVisibility(View.VISIBLE);
            } else {
                ivReceiptIndicator.setVisibility(View.GONE);
            }
        }
    }
}
