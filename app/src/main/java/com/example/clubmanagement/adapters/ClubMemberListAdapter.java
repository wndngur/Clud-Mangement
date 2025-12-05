package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Member;

import java.util.ArrayList;
import java.util.List;

public class ClubMemberListAdapter extends RecyclerView.Adapter<ClubMemberListAdapter.ClubViewHolder> {

    private List<ClubWithMembers> clubList = new ArrayList<>();
    private OnMemberClickListener memberClickListener;

    public interface OnMemberClickListener {
        void onMemberClick(Member member, String clubId, String clubName);
    }

    public static class ClubWithMembers {
        public String clubId;
        public String clubName;
        public List<Member> members;

        public ClubWithMembers(String clubId, String clubName, List<Member> members) {
            this.clubId = clubId;
            this.clubName = clubName;
            this.members = members;
        }
    }

    public void setClubList(List<ClubWithMembers> clubs) {
        this.clubList = clubs;
        notifyDataSetChanged();
    }

    public void setOnMemberClickListener(OnMemberClickListener listener) {
        this.memberClickListener = listener;
    }

    @NonNull
    @Override
    public ClubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_club_member_list, parent, false);
        return new ClubViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClubViewHolder holder, int position) {
        ClubWithMembers club = clubList.get(position);
        holder.bind(club, memberClickListener);
    }

    @Override
    public int getItemCount() {
        return clubList.size();
    }

    static class ClubViewHolder extends RecyclerView.ViewHolder {
        TextView tvClubName, tvMemberCount, tvEmptyMessage;
        RecyclerView rvMembers;

        ClubViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClubName = itemView.findViewById(R.id.tvClubName);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            rvMembers = itemView.findViewById(R.id.rvMembers);
            tvEmptyMessage = itemView.findViewById(R.id.tvEmptyMessage);
        }

        void bind(ClubWithMembers club, OnMemberClickListener listener) {
            tvClubName.setText(club.clubName);
            tvMemberCount.setText(club.members.size() + "명");

            // 멤버가 없으면 빈 메시지 표시
            if (club.members.isEmpty()) {
                rvMembers.setVisibility(View.GONE);
                tvEmptyMessage.setVisibility(View.VISIBLE);
            } else {
                rvMembers.setVisibility(View.VISIBLE);
                tvEmptyMessage.setVisibility(View.GONE);

                // 내부 멤버 RecyclerView 설정
                MemberSimpleAdapter adapter = new MemberSimpleAdapter(club.clubId, club.clubName, listener);
                rvMembers.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                rvMembers.setAdapter(adapter);
                adapter.setMembers(club.members);
            }
        }
    }

    // 내부 멤버 어댑터
    static class MemberSimpleAdapter extends RecyclerView.Adapter<MemberSimpleAdapter.MemberViewHolder> {
        private List<Member> members = new ArrayList<>();
        private String clubId;
        private String clubName;
        private OnMemberClickListener listener;

        MemberSimpleAdapter(String clubId, String clubName, OnMemberClickListener listener) {
            this.clubId = clubId;
            this.clubName = clubName;
            this.listener = listener;
        }

        void setMembers(List<Member> members) {
            this.members = members;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_member_simple, parent, false);
            return new MemberViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            Member member = members.get(position);
            holder.bind(member, clubId, clubName, listener);
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        static class MemberViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProfile;
            TextView tvMemberName, tvMemberInfo;

            MemberViewHolder(@NonNull View itemView) {
                super(itemView);
                ivProfile = itemView.findViewById(R.id.ivProfile);
                tvMemberName = itemView.findViewById(R.id.tvMemberName);
                tvMemberInfo = itemView.findViewById(R.id.tvMemberInfo);
            }

            void bind(Member member, String clubId, String clubName, OnMemberClickListener listener) {
                // 이름이 이메일 형식이면 이메일의 @ 앞부분만 표시하거나 "이름 없음" 표시
                String displayName = member.getName();
                if (displayName == null || displayName.isEmpty()) {
                    displayName = "이름 없음";
                } else if (displayName.contains("@")) {
                    // 이메일 형식인 경우 @ 앞부분만 표시
                    displayName = displayName.substring(0, displayName.indexOf("@"));
                }
                tvMemberName.setText(displayName);

                // 직급 표시 (설정된 role 값 우선)
                String role = member.getRole();
                boolean isAdmin = member.isAdmin();

                // 설정된 직급이 있으면 그 직급을 표시
                if ("부회장".equals(role)) {
                    tvMemberInfo.setText(clubName + " 부회장");
                } else if ("총무".equals(role)) {
                    tvMemberInfo.setText(clubName + " 총무");
                } else if ("회계".equals(role)) {
                    tvMemberInfo.setText(clubName + " 회계");
                } else if ("회장".equals(role) || "admin".equals(role)) {
                    tvMemberInfo.setText(clubName + " 회장");
                } else if (isAdmin) {
                    // 직급이 없지만 관리자 권한이 있는 경우
                    tvMemberInfo.setText(clubName + " 회장");
                } else {
                    tvMemberInfo.setText("부원");
                }

                // 클릭 리스너 설정
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMemberClick(member, clubId, clubName);
                    }
                });
            }
        }
    }
}
