package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.Member;
import com.example.clubmanagement.models.User;
import com.example.clubmanagement.utils.FirebaseManager;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    public static final int TYPE_MEMBERS = 0;
    public static final int TYPE_JOIN_REQUESTS = 1;
    public static final int TYPE_LEAVE_REQUESTS = 2;

    private List<Member> members;
    private int listType;
    private OnMemberActionListener listener;

    public interface OnMemberActionListener {
        void onGrantAdmin(Member member);
        void onRevokeAdmin(Member member);
        void onExpelMember(Member member);
        void onApprove(Member member);
        void onReject(Member member);
        void onSetRole(Member member);
    }

    public MemberAdapter(List<Member> members, int listType, OnMemberActionListener listener) {
        this.members = members;
        this.listType = listType;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = members.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvStudentId;
        private TextView tvDepartment;
        private TextView tvPhone;
        private TextView tvAdminBadge;
        private ImageButton btnMore;
        private LinearLayout layoutActions;
        private MaterialButton btnApprove;
        private MaterialButton btnReject;
        private LinearLayout layoutExpulsionWarning;
        private TextView tvExpulsionHistory;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvAdminBadge = itemView.findViewById(R.id.tvAdminBadge);
            btnMore = itemView.findViewById(R.id.btnMore);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            layoutExpulsionWarning = itemView.findViewById(R.id.layoutExpulsionWarning);
            tvExpulsionHistory = itemView.findViewById(R.id.tvExpulsionHistory);
        }

        public void bind(Member member) {
            tvName.setText(member.getName());
            tvStudentId.setText("학번: " + member.getStudentId());
            tvDepartment.setText(member.getDepartment());

            // Show/hide admin badge
            if (member.isAdmin() && listType == TYPE_MEMBERS) {
                tvAdminBadge.setVisibility(View.VISIBLE);
            } else {
                tvAdminBadge.setVisibility(View.GONE);
            }

            // Configure based on list type
            switch (listType) {
                case TYPE_MEMBERS:
                    setupMemberView(member);
                    break;
                case TYPE_JOIN_REQUESTS:
                    setupJoinRequestView(member);
                    break;
                case TYPE_LEAVE_REQUESTS:
                    setupLeaveRequestView(member);
                    break;
            }
        }

        private void setupMemberView(Member member) {
            layoutActions.setVisibility(View.GONE);
            btnMore.setVisibility(View.VISIBLE);
            layoutExpulsionWarning.setVisibility(View.GONE);

            // 관리자 권한이 있는 화면에서 전화번호 표시
            String phone = member.getPhone();
            if (phone != null && !phone.isEmpty()) {
                tvPhone.setText(phone);
                tvPhone.setVisibility(View.VISIBLE);
            } else {
                tvPhone.setVisibility(View.GONE);
            }

            btnMore.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);

                if (member.isAdmin()) {
                    popup.getMenu().add(0, 1, 0, "관리자 권한 해제");
                } else {
                    popup.getMenu().add(0, 2, 0, "관리자 권한 부여");
                }
                popup.getMenu().add(0, 4, 0, "직급 설정");
                popup.getMenu().add(0, 3, 0, "동아리 퇴출");

                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 1:
                            if (listener != null) {
                                listener.onRevokeAdmin(member);
                            }
                            return true;
                        case 2:
                            if (listener != null) {
                                listener.onGrantAdmin(member);
                            }
                            return true;
                        case 3:
                            if (listener != null) {
                                listener.onExpelMember(member);
                            }
                            return true;
                        case 4:
                            if (listener != null) {
                                listener.onSetRole(member);
                            }
                            return true;
                    }
                    return false;
                });

                popup.show();
            });
        }

        private void setupJoinRequestView(Member member) {
            layoutActions.setVisibility(View.VISIBLE);
            btnMore.setVisibility(View.GONE);

            // 가입 신청자의 전화번호 표시
            String phone = member.getPhone();
            if (phone != null && !phone.isEmpty()) {
                tvPhone.setText(phone);
                tvPhone.setVisibility(View.VISIBLE);
            } else {
                tvPhone.setVisibility(View.GONE);
            }

            btnApprove.setText("승인");
            btnReject.setText("거절");

            btnApprove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApprove(member);
                }
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(member);
                }
            });

            // Load and show expulsion history for join requests
            loadExpulsionHistory(member);
        }

        private void loadExpulsionHistory(Member member) {
            String userId = member.getUserId();
            if (userId == null || userId.isEmpty()) {
                layoutExpulsionWarning.setVisibility(View.GONE);
                return;
            }

            FirebaseManager.getInstance().getUserExpulsionHistory(userId, new FirebaseManager.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user != null && user.hasExpulsionHistory()) {
                        layoutExpulsionWarning.setVisibility(View.VISIBLE);
                        StringBuilder historyText = new StringBuilder();
                        for (User.ExpulsionRecord record : user.getExpulsionHistory()) {
                            historyText.append("• ").append(record.getClubName())
                                    .append(" (").append(record.getFormattedDate()).append(")\n")
                                    .append("  사유: ").append(record.getReason()).append("\n\n");
                        }
                        tvExpulsionHistory.setText(historyText.toString().trim());
                    } else {
                        layoutExpulsionWarning.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    layoutExpulsionWarning.setVisibility(View.GONE);
                }
            });
        }

        private void setupLeaveRequestView(Member member) {
            layoutActions.setVisibility(View.VISIBLE);
            btnMore.setVisibility(View.GONE);
            layoutExpulsionWarning.setVisibility(View.GONE);

            btnApprove.setText("승인");
            btnReject.setText("거절");

            btnApprove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApprove(member);
                }
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(member);
                }
            });
        }
    }
}
