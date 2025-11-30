package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.NoticeComment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoticeCommentAdapter extends RecyclerView.Adapter<NoticeCommentAdapter.CommentViewHolder> {

    private List<NoticeComment> comments = new ArrayList<>();
    private String currentUserId;
    private OnCommentActionListener listener;
    private boolean isSuperAdminMode = false;
    private boolean isClubAdminMode = false;
    private Set<String> clubMemberIds = new HashSet<>();

    public interface OnCommentActionListener {
        void onEditComment(NoticeComment comment);
        void onDeleteComment(NoticeComment comment);
    }

    public NoticeCommentAdapter(String currentUserId, OnCommentActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setSuperAdminMode(boolean isSuperAdminMode) {
        this.isSuperAdminMode = isSuperAdminMode;
        notifyDataSetChanged();
    }

    public void setClubAdminMode(boolean isClubAdminMode) {
        this.isClubAdminMode = isClubAdminMode;
        notifyDataSetChanged();
    }

    public void setClubMemberIds(Set<String> memberIds) {
        this.clubMemberIds = memberIds != null ? memberIds : new HashSet<>();
        notifyDataSetChanged();
    }

    public void setComments(List<NoticeComment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    public void addComment(NoticeComment comment) {
        this.comments.add(comment);
        notifyItemInserted(comments.size() - 1);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        NoticeComment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvAuthor;
        private TextView tvDate;
        private TextView tvContent;
        private TextView tvEdited;
        private ImageView ivMore;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvEdited = itemView.findViewById(R.id.tvEdited);
            ivMore = itemView.findViewById(R.id.ivMore);
        }

        public void bind(NoticeComment comment) {
            tvAuthor.setText(comment.getAuthorName());
            tvDate.setText(comment.getFormattedDate());
            tvContent.setText(comment.getContent());

            // 수정됨 표시
            if (comment.isEdited()) {
                tvEdited.setVisibility(View.VISIBLE);
            } else {
                tvEdited.setVisibility(View.GONE);
            }

            // 권한 체크:
            // 1. 최고 관리자 모드: 모든 댓글 수정/삭제 가능
            // 2. 동아리 관리자 모드: 자신의 동아리 부원 댓글만 수정/삭제 가능
            // 3. 일반 사용자: 본인 댓글만 수정/삭제 가능
            boolean canModify = false;

            if (isSuperAdminMode) {
                // 최고 관리자는 모든 댓글 수정/삭제 가능
                canModify = true;
            } else if (isClubAdminMode && clubMemberIds.contains(comment.getAuthorId())) {
                // 동아리 관리자는 자기 동아리 부원 댓글만 수정/삭제 가능
                canModify = true;
            } else if (comment.isAuthor(currentUserId)) {
                // 본인 댓글은 언제나 수정/삭제 가능
                canModify = true;
            }

            if (canModify) {
                ivMore.setVisibility(View.VISIBLE);
                ivMore.setOnClickListener(v -> showCommentMenu(comment));
            } else {
                ivMore.setVisibility(View.GONE);
            }
        }

        private void showCommentMenu(NoticeComment comment) {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(itemView.getContext(), ivMore);
            popup.getMenu().add("수정");
            popup.getMenu().add("삭제");

            popup.setOnMenuItemClickListener(item -> {
                if ("수정".equals(item.getTitle())) {
                    if (listener != null) listener.onEditComment(comment);
                    return true;
                } else if ("삭제".equals(item.getTitle())) {
                    if (listener != null) listener.onDeleteComment(comment);
                    return true;
                }
                return false;
            });

            popup.show();
        }
    }
}
