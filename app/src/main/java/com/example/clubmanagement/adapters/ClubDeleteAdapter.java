package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Club;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ClubDeleteAdapter extends RecyclerView.Adapter<ClubDeleteAdapter.ViewHolder> {

    private List<Club> clubs;
    private Set<String> selectedClubIds = new HashSet<>();
    private Set<Integer> expandedPositions = new HashSet<>();
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public ClubDeleteAdapter(List<Club> clubs, OnSelectionChangedListener listener) {
        this.clubs = clubs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_club_delete, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Club club = clubs.get(position);
        String clubId = club.getId();

        // 동아리 이름
        holder.tvClubName.setText(club.getName() != null ? club.getName() : "이름 없음");

        // 중앙동아리 뱃지
        if (club.isCentralClub()) {
            holder.tvCentralBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvCentralBadge.setVisibility(View.GONE);
        }

        // 요약 정보
        int memberCount = club.getMemberCount();
        String foundingDateStr = formatFoundingDate(club.getFoundedAt());
        holder.tvClubSummary.setText("부원 " + memberCount + "명 | 설립일: " + foundingDateStr);

        // 상세 정보
        holder.tvFoundingDate.setText(foundingDateStr);
        holder.tvMemberCount.setText(memberCount + "명");
        holder.tvClubType.setText(club.isCentralClub() ? "중앙동아리" : "일반동아리");
        holder.tvDescription.setText(club.getDescription() != null && !club.getDescription().isEmpty()
                ? club.getDescription() : "-");

        // 체크박스 상태
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(selectedClubIds.contains(clubId));
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedClubIds.add(clubId);
            } else {
                selectedClubIds.remove(clubId);
            }
            if (listener != null) {
                listener.onSelectionChanged(selectedClubIds.size());
            }
        });

        // 펼침 상태
        boolean isExpanded = expandedPositions.contains(position);
        holder.layoutDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.ivExpand.setRotation(isExpanded ? 180 : 0);

        // 헤더 클릭 시 펼치기/접기
        holder.layoutHeader.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            if (expandedPositions.contains(adapterPosition)) {
                expandedPositions.remove(adapterPosition);
            } else {
                expandedPositions.add(adapterPosition);
            }
            notifyItemChanged(adapterPosition);
        });
    }

    @Override
    public int getItemCount() {
        return clubs.size();
    }

    private String formatFoundingDate(Timestamp foundingDate) {
        if (foundingDate == null) {
            return "-";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
        return sdf.format(foundingDate.toDate());
    }

    public List<Club> getSelectedClubs() {
        List<Club> selected = new ArrayList<>();
        for (Club club : clubs) {
            if (selectedClubIds.contains(club.getId())) {
                selected.add(club);
            }
        }
        return selected;
    }

    public void clearSelection() {
        selectedClubIds.clear();
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(0);
        }
    }

    public void updateClubs(List<Club> newClubs) {
        this.clubs = newClubs;
        selectedClubIds.clear();
        expandedPositions.clear();
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(0);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutHeader;
        CheckBox cbSelect;
        TextView tvClubName;
        TextView tvCentralBadge;
        TextView tvClubSummary;
        ImageView ivExpand;
        LinearLayout layoutDetails;
        TextView tvFoundingDate;
        TextView tvMemberCount;
        TextView tvClubType;
        TextView tvDescription;

        ViewHolder(View itemView) {
            super(itemView);
            layoutHeader = itemView.findViewById(R.id.layoutHeader);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            tvClubName = itemView.findViewById(R.id.tvClubName);
            tvCentralBadge = itemView.findViewById(R.id.tvCentralBadge);
            tvClubSummary = itemView.findViewById(R.id.tvClubSummary);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            layoutDetails = itemView.findViewById(R.id.layoutDetails);
            tvFoundingDate = itemView.findViewById(R.id.tvFoundingDate);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            tvClubType = itemView.findViewById(R.id.tvClubType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}
