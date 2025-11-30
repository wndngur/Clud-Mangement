package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.ClubNotice;

import java.util.ArrayList;
import java.util.List;

public class ClubNoticeAdapter extends RecyclerView.Adapter<ClubNoticeAdapter.NoticeViewHolder> {

    private List<ClubNotice> notices = new ArrayList<>();
    private OnNoticeClickListener listener;

    public interface OnNoticeClickListener {
        void onNoticeClick(ClubNotice notice);
    }

    public ClubNoticeAdapter(OnNoticeClickListener listener) {
        this.listener = listener;
    }

    public void setNotices(List<ClubNotice> notices) {
        this.notices = notices;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notice, parent, false);
        return new NoticeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        ClubNotice notice = notices.get(position);
        holder.bind(notice);
    }

    @Override
    public int getItemCount() {
        return notices.size();
    }

    class NoticeViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;
        private TextView tvContent;
        private TextView tvAuthor;
        private TextView tvDate;
        private TextView tvViewCount;
        private TextView tvCommentCount;
        private TextView tvPinnedBadge;

        public NoticeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvViewCount = itemView.findViewById(R.id.tvViewCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            tvPinnedBadge = itemView.findViewById(R.id.tvPinnedBadge);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNoticeClick(notices.get(position));
                }
            });
        }

        public void bind(ClubNotice notice) {
            tvTitle.setText(notice.getTitle());
            tvContent.setText(notice.getContent());
            tvAuthor.setText(notice.getAuthorName());
            tvDate.setText(notice.getShortDate());
            tvViewCount.setText(String.valueOf(notice.getViewCount()));
            tvCommentCount.setText(String.valueOf(notice.getCommentCount()));

            if (notice.isPinned()) {
                tvPinnedBadge.setVisibility(View.VISIBLE);
            } else {
                tvPinnedBadge.setVisibility(View.GONE);
            }
        }
    }
}
