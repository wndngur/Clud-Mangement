package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Member;

import java.util.List;

public class MemberSimpleAdapter extends RecyclerView.Adapter<MemberSimpleAdapter.MemberViewHolder> {

    private List<Member> membersList;

    public MemberSimpleAdapter(List<Member> membersList) {
        this.membersList = membersList;
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
        Member member = membersList.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMemberName;
        private TextView tvMemberInfo;
        private TextView tvAdminBadge;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberInfo = itemView.findViewById(R.id.tvMemberInfo);
            tvAdminBadge = itemView.findViewById(R.id.tvAdminBadge);
        }

        public void bind(Member member) {
            tvMemberName.setText(member.getName());

            // Show role/info
            if (member.isAdmin()) {
                tvMemberInfo.setText("동아리 관리자");
                tvAdminBadge.setVisibility(View.VISIBLE);
            } else {
                tvMemberInfo.setText("부원");
                tvAdminBadge.setVisibility(View.GONE);
            }
        }
    }
}
