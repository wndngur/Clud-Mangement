package com.example.clubmanagement.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.ClubApplication;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClubApprovalAdapter extends RecyclerView.Adapter<ClubApprovalAdapter.ViewHolder> {

    private Context context;
    private List<ClubApplication> applications;
    private OnApprovalActionListener listener;
    private Set<Integer> expandedPositions;

    public interface OnApprovalActionListener {
        void onApprove(ClubApplication application);
        void onReject(ClubApplication application);
    }

    public ClubApprovalAdapter(Context context, OnApprovalActionListener listener) {
        this.context = context;
        this.applications = new ArrayList<>();
        this.listener = listener;
        this.expandedPositions = new HashSet<>();
    }

    public void setApplications(List<ClubApplication> applications) {
        this.applications = applications != null ? applications : new ArrayList<>();
        this.expandedPositions.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_club_approval, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClubApplication application = applications.get(position);
        boolean isExpanded = expandedPositions.contains(position);

        // 기본 정보 표시
        holder.tvClubName.setText(application.getClubName());
        holder.tvApplicantInfo.setText("신청자: " + application.getApplicantEmail());
        holder.tvDate.setText(application.getFormattedDate());

        // 상세 정보 표시
        holder.tvDescription.setText(application.getDescription());
        holder.tvPurpose.setText(application.getPurpose());
        holder.tvActivityPlan.setText(application.getActivityPlan());

        // 드릴다운 상태 설정
        holder.layoutDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.ivExpand.setRotation(isExpanded ? 180f : 0f);

        // 카드 클릭 시 드릴다운 토글
        holder.cardView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            if (expandedPositions.contains(adapterPosition)) {
                expandedPositions.remove(adapterPosition);
            } else {
                expandedPositions.add(adapterPosition);
            }
            notifyItemChanged(adapterPosition);
        });

        // 승인 버튼
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(application);
            }
        });

        // 불가 버튼
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(application);
            }
        });
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvClubName;
        TextView tvApplicantInfo;
        TextView tvDate;
        View ivExpand;
        LinearLayout layoutDetails;
        TextView tvDescription;
        TextView tvPurpose;
        TextView tvActivityPlan;
        MaterialButton btnApprove;
        MaterialButton btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvClubName = itemView.findViewById(R.id.tvClubName);
            tvApplicantInfo = itemView.findViewById(R.id.tvApplicantInfo);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            layoutDetails = itemView.findViewById(R.id.layoutDetails);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvActivityPlan = itemView.findViewById(R.id.tvActivityPlan);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
