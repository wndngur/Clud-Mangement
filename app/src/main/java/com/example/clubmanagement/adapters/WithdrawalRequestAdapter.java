package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.WithdrawalRequest;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class WithdrawalRequestAdapter extends RecyclerView.Adapter<WithdrawalRequestAdapter.ViewHolder> {

    private List<WithdrawalRequest> requests;
    private OnWithdrawalActionListener listener;
    private int expandedPosition = -1;

    public interface OnWithdrawalActionListener {
        void onApprove(WithdrawalRequest request);
        void onReject(WithdrawalRequest request);
    }

    public WithdrawalRequestAdapter(List<WithdrawalRequest> requests, OnWithdrawalActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_withdrawal_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WithdrawalRequest request = requests.get(position);
        boolean isExpanded = position == expandedPosition;

        holder.tvUserName.setText(request.getUserName());
        holder.tvUserEmail.setText(request.getUserEmail());
        holder.tvRequestDate.setText("신청일: " + request.getFormattedDate());
        holder.tvReason.setText(request.getReason());

        // Accordion expand/collapse
        holder.llExpandedContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.ivExpandIcon.setRotation(isExpanded ? 180 : 0);

        holder.llHeader.setOnClickListener(v -> {
            int previousExpanded = expandedPosition;
            if (isExpanded) {
                expandedPosition = -1;
            } else {
                expandedPosition = holder.getAdapterPosition();
            }

            if (previousExpanded != -1) {
                notifyItemChanged(previousExpanded);
            }
            notifyItemChanged(holder.getAdapterPosition());
        });

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(request);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    public void updateData(List<WithdrawalRequest> newRequests) {
        this.requests = newRequests;
        this.expandedPosition = -1;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llHeader;
        LinearLayout llExpandedContent;
        TextView tvUserName;
        TextView tvUserEmail;
        TextView tvRequestDate;
        TextView tvReason;
        ImageView ivExpandIcon;
        MaterialButton btnApprove;
        MaterialButton btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            llHeader = itemView.findViewById(R.id.llHeader);
            llExpandedContent = itemView.findViewById(R.id.llExpandedContent);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            tvReason = itemView.findViewById(R.id.tvReason);
            ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
