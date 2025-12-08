package com.example.clubmanagement.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private List<ChatRoom> filteredChatRooms = new ArrayList<>();
    private OnChatRoomClickListener listener;
    private OnChatRoomLongClickListener longClickListener;
    private String currentFilter = "";

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
    }

    public interface OnChatRoomLongClickListener {
        void onChatRoomLongClick(ChatRoom chatRoom, int position);
    }

    public void setChatRooms(List<ChatRoom> chatRooms) {
        this.chatRooms = new ArrayList<>(chatRooms);
        applyFilter();
    }

    public void setOnChatRoomClickListener(OnChatRoomClickListener listener) {
        this.listener = listener;
    }

    public void setOnChatRoomLongClickListener(OnChatRoomLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void removeChatRoom(int position) {
        if (position >= 0 && position < filteredChatRooms.size()) {
            ChatRoom removed = filteredChatRooms.get(position);
            chatRooms.remove(removed);
            filteredChatRooms.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void removeChatRoomById(String chatRoomId) {
        // 원본 목록에서 제거
        ChatRoom toRemove = null;
        for (ChatRoom chatRoom : chatRooms) {
            if (chatRoom.getChatRoomId() != null && chatRoom.getChatRoomId().equals(chatRoomId)) {
                toRemove = chatRoom;
                break;
            }
        }
        if (toRemove != null) {
            chatRooms.remove(toRemove);
        }

        // 필터된 목록에서 제거
        int removePosition = -1;
        for (int i = 0; i < filteredChatRooms.size(); i++) {
            ChatRoom chatRoom = filteredChatRooms.get(i);
            if (chatRoom.getChatRoomId() != null && chatRoom.getChatRoomId().equals(chatRoomId)) {
                removePosition = i;
                break;
            }
        }
        if (removePosition >= 0) {
            filteredChatRooms.remove(removePosition);
            notifyItemRemoved(removePosition);
        }
    }

    public void addChatRoom(ChatRoom chatRoom) {
        // 중복 체크
        for (ChatRoom existing : chatRooms) {
            if (existing.getChatRoomId().equals(chatRoom.getChatRoomId())) {
                return;
            }
        }
        chatRooms.add(0, chatRoom);
        applyFilter();
    }

    // 검색 필터 적용
    public void filter(String query) {
        this.currentFilter = query != null ? query.toLowerCase().trim() : "";
        applyFilter();
    }

    private void applyFilter() {
        filteredChatRooms.clear();

        if (currentFilter.isEmpty()) {
            filteredChatRooms.addAll(chatRooms);
        } else {
            for (ChatRoom chatRoom : chatRooms) {
                // 채팅방 이름, 파트너 이름, 동아리 이름으로 검색
                String title = chatRoom.getChatRoomTitle() != null ? chatRoom.getChatRoomTitle().toLowerCase() : "";
                String partnerName = chatRoom.getPartnerName() != null ? chatRoom.getPartnerName().toLowerCase() : "";
                String clubName = chatRoom.getClubName() != null ? chatRoom.getClubName().toLowerCase() : "";

                if (title.contains(currentFilter) ||
                    partnerName.contains(currentFilter) ||
                    clubName.contains(currentFilter)) {
                    filteredChatRooms.add(chatRoom);
                }
            }
        }

        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return filteredChatRooms.isEmpty();
    }

    public boolean hasNoResults() {
        return !currentFilter.isEmpty() && filteredChatRooms.isEmpty();
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
        ChatRoom chatRoom = filteredChatRooms.get(position);
        holder.bind(chatRoom, position, listener, longClickListener);
    }

    @Override
    public int getItemCount() {
        return filteredChatRooms.size();
    }

    static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvChatRoomName, tvClubName, tvLastMessage, tvTime, tvUnreadCount;

        ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvChatRoomName = itemView.findViewById(R.id.tvChatRoomName);
            tvClubName = itemView.findViewById(R.id.tvClubName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }

        void bind(ChatRoom chatRoom, int position, OnChatRoomClickListener listener, OnChatRoomLongClickListener longClickListener) {
            // 채팅방 이름 설정
            tvChatRoomName.setText(chatRoom.getChatRoomTitle());

            // 단체 채팅방인 경우
            if (chatRoom.isGroupChat()) {
                // 멤버 수 표시
                String memberInfo = chatRoom.getMemberCount() > 0
                        ? chatRoom.getMemberCount() + ""
                        : "";
                if (!memberInfo.isEmpty()) {
                    tvChatRoomName.setText(chatRoom.getChatRoomTitle() + " " + memberInfo);
                }

                tvClubName.setVisibility(View.GONE);

                // 마지막 메시지
                String lastMsg = chatRoom.getLastMessage();
                if (lastMsg == null || lastMsg.isEmpty()) {
                    tvLastMessage.setText("새로운 단체 채팅방입니다");
                    tvLastMessage.setTextColor(0xFF888888);
                } else {
                    tvLastMessage.setText(lastMsg);
                    tvLastMessage.setTextColor(0xFF888888);
                }
            } else {
                // 개인 채팅방인 경우
                // 동아리 이름과 직급 정보
                String clubInfo = "";
                if (chatRoom.getClubName() != null && !chatRoom.getClubName().isEmpty()) {
                    clubInfo = chatRoom.getClubName();
                    if (chatRoom.getPartnerRole() != null && !chatRoom.getPartnerRole().isEmpty()) {
                        clubInfo += " · " + chatRoom.getPartnerRole();
                    }
                    tvClubName.setText(clubInfo);
                    tvClubName.setVisibility(View.VISIBLE);
                } else {
                    tvClubName.setVisibility(View.GONE);
                }

                // 마지막 메시지 또는 나간 사용자 표시
                String lastMsg = chatRoom.getLastMessage();
                if (chatRoom.getLeftUserId() != null && !chatRoom.getLeftUserId().isEmpty()) {
                    tvLastMessage.setText("상대방이 나갔습니다");
                    tvLastMessage.setTextColor(0xFFFF5722);
                } else if (lastMsg == null || lastMsg.isEmpty()) {
                    tvLastMessage.setText("새로운 채팅방입니다");
                    tvLastMessage.setTextColor(0xFF888888);
                } else {
                    tvLastMessage.setText(lastMsg);
                    tvLastMessage.setTextColor(0xFF888888);
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

            // 클릭 리스너
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatRoomClick(chatRoom);
                }
            });

            // 길게 터치 리스너
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onChatRoomLongClick(chatRoom, position);
                    return true;
                }
                return false;
            });
        }

        private String formatTime(long timestamp) {
            if (timestamp == 0) return "";

            Date date = new Date(timestamp);
            Date now = new Date();

            SimpleDateFormat todayFormat = new SimpleDateFormat("a h:mm", Locale.KOREA);
            SimpleDateFormat dateFormat = new SimpleDateFormat("M월 d일", Locale.KOREA);

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
