package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Club;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DemotableCentralClubAdapter extends RecyclerView.Adapter<DemotableCentralClubAdapter.ViewHolder> {

    private List<Club> clubs = new ArrayList<>();
    private Set<String> selectedClubIds = new HashSet<>();

    public void setClubs(List<Club> clubs) {
        this.clubs = clubs;
        this.selectedClubIds.clear();
        notifyDataSetChanged();
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_demotable_central_club, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Club club = clubs.get(position);
        holder.bind(club, selectedClubIds.contains(club.getId()));
    }

    @Override
    public int getItemCount() {
        return clubs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cbSelect;
        private TextView tvClubName;
        private TextView tvFoundedAt;
        private TextView tvMemberCount;
        private TextView tvMemberStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            tvClubName = itemView.findViewById(R.id.tvClubName);
            tvFoundedAt = itemView.findViewById(R.id.tvFoundedAt);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            tvMemberStatus = itemView.findViewById(R.id.tvMemberStatus);
        }

        public void bind(Club club, boolean isSelected) {
            tvClubName.setText(club.getName());

            // 설립일
            if (club.getFoundedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);
                String foundedDate = sdf.format(club.getFoundedAt().toDate());
                long daysSinceFounding = club.getDaysSinceFounding();
                tvFoundedAt.setText("설립: " + foundedDate + " (" + daysSinceFounding + "일 경과)");
            } else {
                tvFoundedAt.setText("설립일: 미등록");
            }

            // 인원 수
            int memberCount = club.getMemberCount();
            int minMembers = Club.CENTRAL_CLUB_MAINTAIN_MIN_MEMBERS;
            tvMemberCount.setText("인원: " + memberCount + "명 / " + minMembers + "명");

            // 유지 가능 여부
            if (memberCount >= minMembers) {
                tvMemberStatus.setText("✓ 중앙동아리 유지 가능");
                tvMemberStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
            } else {
                int needed = minMembers - memberCount;
                tvMemberStatus.setText("⚠ " + needed + "명 부족 (유지 불가)");
                tvMemberStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
            }

            // 체크박스 설정
            cbSelect.setOnCheckedChangeListener(null);  // 리스너 제거 (무한 루프 방지)
            cbSelect.setChecked(isSelected);

            // 체크박스 리스너
            cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedClubIds.add(club.getId());
                } else {
                    selectedClubIds.remove(club.getId());
                }
            });

            // 아이템 전체 클릭 시 체크박스 토글
            itemView.setOnClickListener(v -> {
                cbSelect.setChecked(!cbSelect.isChecked());
            });
        }
    }
}
