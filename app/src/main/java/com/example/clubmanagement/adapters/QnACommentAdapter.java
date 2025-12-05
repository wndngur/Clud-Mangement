package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.QnAComment;
import com.example.clubmanagement.utils.DateHelper;

import java.util.ArrayList;
import java.util.List;

public class QnACommentAdapter extends RecyclerView.Adapter<QnACommentAdapter.CommentViewHolder> {

    private List<QnAComment> comments = new ArrayList<>();
    private String currentUserId;
    private boolean isAdmin;
    private boolean isSuperAdmin;
    private OnEditClickListener editClickListener;

    public interface OnEditClickListener {
        void onEditClick(QnAComment comment);
    }

    public QnACommentAdapter() {
    }

    public QnACommentAdapter(String currentUserId, boolean isAdmin, boolean isSuperAdmin, OnEditClickListener listener) {
        this.currentUserId = currentUserId;
        this.isAdmin = isAdmin;
        this.isSuperAdmin = isSuperAdmin;
        this.editClickListener = listener;
    }

    public void setUserInfo(String currentUserId, boolean isAdmin, boolean isSuperAdmin) {
        this.currentUserId = currentUserId;
        this.isAdmin = isAdmin;
        this.isSuperAdmin = isSuperAdmin;
        notifyDataSetChanged();
    }

    public void setEditClickListener(OnEditClickListener listener) {
        this.editClickListener = listener;
    }

    public void setComments(List<QnAComment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    public void addComment(QnAComment comment) {
        comments.add(comment);
        notifyItemInserted(comments.size() - 1);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_qna_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        QnAComment comment = comments.get(position);
        holder.bind(comment, currentUserId, isAdmin, isSuperAdmin, editClickListener);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvAuthorName;
        private TextView tvAdminBadge;
        private TextView tvDate;
        private TextView tvContent;
        private ImageView ivEdit;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvAdminBadge = itemView.findViewById(R.id.tvAdminBadge);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvContent = itemView.findViewById(R.id.tvContent);
            ivEdit = itemView.findViewById(R.id.ivEdit);
        }

        public void bind(QnAComment comment, String currentUserId, boolean isAdmin, boolean isSuperAdmin, OnEditClickListener listener) {
            tvAuthorName.setText(comment.getAuthorName());
            tvContent.setText(comment.getContent());

            // Admin badge
            if (comment.isAdmin()) {
                tvAdminBadge.setVisibility(View.VISIBLE);
            } else {
                tvAdminBadge.setVisibility(View.GONE);
            }

            // Date
            if (comment.getCreatedAt() != null) {
                String dateStr = DateHelper.getTimeAgo(comment.getCreatedAt().toDate());
                tvDate.setText(dateStr);
            } else {
                tvDate.setText("");
            }

            // Edit button visibility
            // 동아리 가입자: 자신이 쓴 답변만 수정 가능
            // 관리자/최고관리자: 모든 답변 수정 가능
            boolean canEdit = false;
            if (isAdmin || isSuperAdmin) {
                canEdit = true;  // 관리자는 모든 답변 수정 가능
            } else if (currentUserId != null && currentUserId.equals(comment.getAuthorId())) {
                canEdit = true;  // 본인이 쓴 답변만 수정 가능
            }

            if (canEdit && listener != null) {
                ivEdit.setVisibility(View.VISIBLE);
                ivEdit.setOnClickListener(v -> listener.onEditClick(comment));
            } else {
                ivEdit.setVisibility(View.GONE);
                ivEdit.setOnClickListener(null);
            }
        }
    }
}
