package com.example.clubmanagement.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Club;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class RecommendedClubAdapter extends RecyclerView.Adapter<RecommendedClubAdapter.ViewHolder> {

    private List<Club> clubs;
    private List<Integer> scores;
    private Context context;
    private OnClubClickListener listener;

    public interface OnClubClickListener {
        void onClubClick(Club club);
    }

    public RecommendedClubAdapter(Context context, OnClubClickListener listener) {
        this.context = context;
        this.clubs = new ArrayList<>();
        this.scores = new ArrayList<>();
        this.listener = listener;
    }

    public void setClubs(List<Club> clubs, List<Integer> scores) {
        this.clubs = clubs != null ? clubs : new ArrayList<>();
        this.scores = scores != null ? scores : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommended_club, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Club club = clubs.get(position);
        int score = position < scores.size() ? scores.get(position) : 0;

        holder.tvClubName.setText(club.getName());

        // Description
        String description = club.getDescription();
        if (description != null && !description.isEmpty()) {
            holder.tvClubDescription.setText(description);
            holder.tvClubDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvClubDescription.setVisibility(View.GONE);
        }

        // Match score
        int matchPercent = Math.min(score, 100);
        holder.tvMatchScore.setText("매칭 " + matchPercent + "%");

        // Tags
        holder.chipGroupTags.removeAllViews();

        // 기독교 동아리 태그
        if (club.isChristian()) {
            addChip(holder.chipGroupTags, "기독교");
        }

        // 분위기 태그
        String atmosphere = club.getAtmosphere();
        if (atmosphere != null) {
            if ("lively".equals(atmosphere)) {
                addChip(holder.chipGroupTags, "활기찬");
            } else if ("quiet".equals(atmosphere)) {
                addChip(holder.chipGroupTags, "조용한");
            }
        }

        // 활동 유형 태그
        List<String> activityTypes = club.getActivityTypes();
        if (activityTypes != null) {
            for (String type : activityTypes) {
                addChip(holder.chipGroupTags, getActivityTypeLabel(type));
            }
        }

        // 목적 태그
        List<String> purposes = club.getPurposes();
        if (purposes != null) {
            for (String purpose : purposes) {
                addChip(holder.chipGroupTags, getPurposeLabel(purpose));
            }
        }

        // 중앙동아리 태그
        if (club.isCentralClub()) {
            addChip(holder.chipGroupTags, "중앙동아리");
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClubClick(club);
            }
        });
    }

    private void addChip(ChipGroup chipGroup, String text) {
        Chip chip = new Chip(context);
        chip.setText(text);
        chip.setTextSize(12);
        chip.setClickable(false);
        chip.setCheckable(false);
        chipGroup.addView(chip);
    }

    private String getActivityTypeLabel(String type) {
        switch (type) {
            case "volunteer": return "봉사";
            case "sports": return "운동";
            case "outdoor": return "야외활동";
            default: return type;
        }
    }

    private String getPurposeLabel(String purpose) {
        switch (purpose) {
            case "career": return "취업/스펙";
            case "academic": return "학술/연구";
            case "art": return "예술/문화";
            default: return purpose;
        }
    }

    @Override
    public int getItemCount() {
        return clubs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvClubName;
        TextView tvClubDescription;
        TextView tvMatchScore;
        ChipGroup chipGroupTags;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClubName = itemView.findViewById(R.id.tvClubName);
            tvClubDescription = itemView.findViewById(R.id.tvClubDescription);
            tvMatchScore = itemView.findViewById(R.id.tvMatchScore);
            chipGroupTags = itemView.findViewById(R.id.chipGroupTags);
        }
    }
}
