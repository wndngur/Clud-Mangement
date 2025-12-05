package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.QnAItem;
import com.example.clubmanagement.utils.DateHelper;

import java.util.ArrayList;
import java.util.List;

public class QnAAdapter extends RecyclerView.Adapter<QnAAdapter.QnAViewHolder> {

    private List<QnAItem> items = new ArrayList<>();
    private OnItemClickListener listener;
    private OnSettingsClickListener settingsListener;
    private boolean isAdminMode = false;

    public interface OnItemClickListener {
        void onItemClick(QnAItem item);
    }

    public interface OnSettingsClickListener {
        void onSettingsClick(QnAItem item);
    }

    public QnAAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setSettingsClickListener(OnSettingsClickListener listener) {
        this.settingsListener = listener;
    }

    public void setAdminMode(boolean adminMode) {
        this.isAdminMode = adminMode;
        notifyDataSetChanged();
    }

    public void setItems(List<QnAItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QnAViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_qna, parent, false);
        return new QnAViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QnAViewHolder holder, int position) {
        QnAItem item = items.get(position);
        holder.bind(item, isAdminMode, settingsListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class QnAViewHolder extends RecyclerView.ViewHolder {
        private TextView tvType;
        private TextView tvPrivateBadge;
        private TextView tvAskerName;
        private TextView tvDate;
        private TextView tvQuestion;
        private LinearLayout llAnswer;
        private TextView tvAnswer;
        private TextView tvUnanswered;
        private TextView tvAnswererInfo;
        private ImageView ivSettings;

        public QnAViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvType);
            tvPrivateBadge = itemView.findViewById(R.id.tvPrivateBadge);
            tvAskerName = itemView.findViewById(R.id.tvAskerName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            llAnswer = itemView.findViewById(R.id.llAnswer);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
            tvUnanswered = itemView.findViewById(R.id.tvUnanswered);
            tvAnswererInfo = itemView.findViewById(R.id.tvAnswererInfo);
            ivSettings = itemView.findViewById(R.id.ivSettings);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(items.get(position));
                }
            });
        }

        public void bind(QnAItem item, boolean isAdminMode, OnSettingsClickListener settingsListener) {
            // Type badge
            if (item.isQnA()) {
                tvType.setText("Q&A");
                tvType.setBackgroundColor(itemView.getContext().getColor(R.color.purple_500));

                // Show asker name for Q&A
                if (item.getAskerName() != null && !item.getAskerName().isEmpty()) {
                    tvAskerName.setText(item.getAskerName());
                    tvAskerName.setVisibility(View.VISIBLE);
                } else {
                    tvAskerName.setVisibility(View.GONE);
                }
            } else {
                tvType.setText("FAQ");
                tvType.setBackgroundColor(itemView.getContext().getColor(R.color.teal_700));
                tvAskerName.setVisibility(View.GONE);
            }

            // Private badge for Q&A
            if (item.isPrivate() && item.isQnA()) {
                tvPrivateBadge.setVisibility(View.VISIBLE);
            } else {
                tvPrivateBadge.setVisibility(View.GONE);
            }

            // Date
            if (item.getCreatedAt() != null) {
                String dateStr = DateHelper.formatDate(item.getCreatedAt().toDate());
                tvDate.setText(dateStr);
            } else {
                tvDate.setText("");
            }

            // Question
            tvQuestion.setText(item.getQuestion());

            // Answer
            if (item.isAnswered() && item.getAnswer() != null && !item.getAnswer().isEmpty()) {
                llAnswer.setVisibility(View.VISIBLE);
                tvAnswer.setText(item.getAnswer());
                tvUnanswered.setVisibility(View.GONE);

                // Answerer info
                if (item.getAnswererName() != null && !item.getAnswererName().isEmpty()) {
                    tvAnswererInfo.setText("답변자: " + item.getAnswererName());
                    tvAnswererInfo.setVisibility(View.VISIBLE);
                } else {
                    tvAnswererInfo.setVisibility(View.GONE);
                }
            } else {
                llAnswer.setVisibility(View.GONE);
                tvAnswererInfo.setVisibility(View.GONE);

                // Show "unanswered" status for Q&A only
                if (item.isQnA()) {
                    tvUnanswered.setVisibility(View.VISIBLE);
                } else {
                    tvUnanswered.setVisibility(View.GONE);
                }
            }

            // Settings button (Admin mode only)
            if (isAdminMode && settingsListener != null) {
                ivSettings.setVisibility(View.VISIBLE);
                ivSettings.setOnClickListener(v -> settingsListener.onSettingsClick(item));
            } else {
                ivSettings.setVisibility(View.GONE);
                ivSettings.setOnClickListener(null);
            }
        }
    }
}
