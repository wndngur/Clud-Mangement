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
import com.example.clubmanagement.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private List<ChatMessage> messages = new ArrayList<>();
    private String currentUserId;
    private OnMessageLongClickListener longClickListener;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message, boolean isOwnMessage);
    }

    public ChatMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setMessages(List<ChatMessage> newMessages) {
        this.messages.clear();
        if (newMessages != null) {
            this.messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        boolean isOwnMessage = message.getSenderId() != null && message.getSenderId().equals(currentUserId);

        // 날짜 구분선 표시 여부 결정
        boolean showDateDivider = false;
        if (position == 0) {
            showDateDivider = true;
        } else {
            ChatMessage prevMessage = messages.get(position - 1);
            showDateDivider = !isSameDay(prevMessage.getTimestamp(), message.getTimestamp());
        }

        holder.bind(message, currentUserId, showDateDivider);

        // 롱클릭 리스너 설정
        View.OnLongClickListener longClick = v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(message, isOwnMessage);
            }
            return true;
        };

        holder.layoutSent.setOnLongClickListener(longClick);
        holder.layoutReceived.setOnLongClickListener(longClick);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private boolean isSameDay(long timestamp1, long timestamp2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(timestamp2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutSent, layoutReceived;
        TextView tvMessageSent, tvTimeSent, tvMessageReceived, tvTimeReceived, tvSenderName;
        TextView tvDateDivider, tvReadStatus;
        ImageView ivProfile;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSent = itemView.findViewById(R.id.layoutSent);
            layoutReceived = itemView.findViewById(R.id.layoutReceived);
            tvMessageSent = itemView.findViewById(R.id.tvMessageSent);
            tvTimeSent = itemView.findViewById(R.id.tvTimeSent);
            tvMessageReceived = itemView.findViewById(R.id.tvMessageReceived);
            tvTimeReceived = itemView.findViewById(R.id.tvTimeReceived);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvDateDivider = itemView.findViewById(R.id.tvDateDivider);
            tvReadStatus = itemView.findViewById(R.id.tvReadStatus);
        }

        void bind(ChatMessage message, String currentUserId, boolean showDateDivider) {
            boolean isSent = message.getSenderId() != null && message.getSenderId().equals(currentUserId);

            // 날짜 구분선 표시
            if (showDateDivider && tvDateDivider != null) {
                tvDateDivider.setVisibility(View.VISIBLE);
                tvDateDivider.setText(formatDate(message.getTimestamp()));
            } else if (tvDateDivider != null) {
                tvDateDivider.setVisibility(View.GONE);
            }

            if (isSent) {
                layoutSent.setVisibility(View.VISIBLE);
                layoutReceived.setVisibility(View.GONE);
                tvMessageSent.setText(message.getMessage());
                tvTimeSent.setText(formatTime(message.getTimestamp()));

                // 읽음 표시 (선택 사항)
                if (tvReadStatus != null) {
                    if (message.isRead()) {
                        tvReadStatus.setVisibility(View.GONE);
                    } else {
                        tvReadStatus.setVisibility(View.GONE); // 현재는 숨김
                    }
                }
            } else {
                layoutSent.setVisibility(View.GONE);
                layoutReceived.setVisibility(View.VISIBLE);
                tvMessageReceived.setText(message.getMessage());
                tvTimeReceived.setText(formatTime(message.getTimestamp()));

                // 발신자 이름 설정
                String senderName = message.getSenderName();
                if (senderName != null && !senderName.isEmpty()) {
                    if (senderName.contains("@")) {
                        senderName = senderName.substring(0, senderName.indexOf("@"));
                    }
                    tvSenderName.setText(senderName);
                } else {
                    tvSenderName.setText("알 수 없음");
                }
            }
        }

        private String formatTime(long timestamp) {
            if (timestamp == 0) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("a h:mm", Locale.KOREA);
            return sdf.format(new Date(timestamp));
        }

        private String formatDate(long timestamp) {
            if (timestamp == 0) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA);
            return sdf.format(new Date(timestamp));
        }
    }
}
