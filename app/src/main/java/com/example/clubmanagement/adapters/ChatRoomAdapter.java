package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clubmanagement.R;
import com.example.clubmanagement.models.ChatRoom;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {

    private List<ChatRoom> chatRooms = new ArrayList<>();
    private OnChatRoomClickListener listener;
    private OnChatRoomSettingsListener settingsListener;

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
    }

    public interface OnChatRoomSettingsListener {
        void onToggleNotification(ChatRoom chatRoom, int position);
        void onDeleteChatRoom(ChatRoom chatRoom, int position);
    }

    public void setChatRooms(List<ChatRoom> chatRooms) {
        this.chatRooms = chatRooms;
        notifyDataSetChanged();
    }

    public void setOnChatRoomClickListener(OnChatRoomClickListener listener) {
        this.listener = listener;
    }

    public void setOnChatRoomSettingsListener(OnChatRoomSettingsListener listener) {
        this.settingsListener = listener;
    }

    public void removeChatRoom(int position) {
        if (position >= 0 && position < chatRooms.size()) {
            chatRooms.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void addChatRoom(ChatRoom chatRoom) {
        // 중복 체크
        for (ChatRoom existing : chatRooms) {
            if (existing.getChatRoomId().equals(chatRoom.getChatRoomId())) {
                return; // 이미 존재하면 추가하지 않음
            }
        }
        chatRooms.add(0, chatRoom); // 최신 채팅방을 맨 위에 추가
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);
        holder.bind(chatRoom, position, listener, settingsListener);
    }

    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvChatRoomName, tvClubName, tvLastMessage, tvTime, tvUnreadCount;
        ImageButton btnSettings;

        ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvChatRoomName = itemView.findViewById(R.id.tvChatRoomName);
            tvClubName = itemView.findViewById(R.id.tvClubName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            btnSettings = itemView.findViewById(R.id.btnSettings);
        }

        void bind(ChatRoom chatRoom, int position, OnChatRoomClickListener listener, OnChatRoomSettingsListener settingsListener) {
            // 채팅방 이름 설정
            tvChatRoomName.setText(chatRoom.getChatRoomTitle());

            // 단체 채팅방인 경우
            if (chatRoom.isGroupChat()) {
                // 멤버 수 표시
                String memberInfo = chatRoom.getMemberCount() > 0
                        ? "멤버 " + chatRoom.getMemberCount() + "명"
                        : "단체 채팅";
                tvClubName.setText(memberInfo);

                // 마지막 메시지
                String lastMsg = chatRoom.getLastMessage();
                if (lastMsg == null || lastMsg.isEmpty()) {
                    tvLastMessage.setText("새로운 단체 채팅방입니다");
                    tvLastMessage.setTextColor(0xFF757575);
                } else {
                    tvLastMessage.setText(lastMsg);
                    tvLastMessage.setTextColor(0xFF757575);
                }
            } else {
                // 개인 채팅방인 경우
                // 동아리 이름 (직급 정보도 표시)
                String clubInfo = chatRoom.getClubName() != null ? chatRoom.getClubName() : "";
                if (chatRoom.getPartnerRole() != null && !chatRoom.getPartnerRole().isEmpty()) {
                    clubInfo += " " + chatRoom.getPartnerRole();
                }
                tvClubName.setText(clubInfo);

                // 마지막 메시지 또는 나간 사용자 표시
                String lastMsg = chatRoom.getLastMessage();
                if (chatRoom.getLeftUserId() != null && !chatRoom.getLeftUserId().isEmpty()) {
                    tvLastMessage.setText("상대방이 나갔습니다");
                    tvLastMessage.setTextColor(0xFFFF5722); // 주황색
                } else if (lastMsg == null || lastMsg.isEmpty()) {
                    tvLastMessage.setText("새로운 채팅방입니다");
                    tvLastMessage.setTextColor(0xFF757575); // 기본 회색
                } else {
                    tvLastMessage.setText(lastMsg);
                    tvLastMessage.setTextColor(0xFF757575); // 기본 회색
                }
            }

            // 시간 표시
            tvTime.setText(formatTime(chatRoom.getLastMessageTime()));

            // 읽지 않은 메시지 수
            if (chatRoom.getUnreadCount() > 0) {
                tvUnreadCount.setVisibility(View.VISIBLE);
                tvUnreadCount.setText(String.valueOf(chatRoom.getUnreadCount()));
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }

            // 카드 클릭 리스너
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatRoomClick(chatRoom);
                }
            });

            // 설정 버튼 클릭 리스너
            btnSettings.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);

                // 알림 끄기/켜기 메뉴
                String notificationText = chatRoom.isNotificationEnabled() ? "알림 끄기" : "알림 켜기";
                popup.getMenu().add(0, 1, 0, notificationText);

                // 단체 채팅방이 아닌 경우에만 나가기 메뉴 표시
                if (!chatRoom.isGroupChat()) {
                    popup.getMenu().add(0, 2, 1, "채팅방 나가기");
                }

                popup.setOnMenuItemClickListener(item -> {
                    if (settingsListener != null) {
                        switch (item.getItemId()) {
                            case 1:
                                settingsListener.onToggleNotification(chatRoom, position);
                                return true;
                            case 2:
                                settingsListener.onDeleteChatRoom(chatRoom, position);
                                return true;
                        }
                    }
                    return false;
                });

                popup.show();
            });
        }

        private String formatTime(long timestamp) {
            if (timestamp == 0) return "";

            Date date = new Date(timestamp);
            Date now = new Date();

            SimpleDateFormat todayFormat = new SimpleDateFormat("a h:mm", Locale.KOREA);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.KOREA);

            // 오늘인지 확인
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
            if (dayFormat.format(date).equals(dayFormat.format(now))) {
                return todayFormat.format(date);
            } else {
                return dateFormat.format(date);
            }
        }
    }
}
