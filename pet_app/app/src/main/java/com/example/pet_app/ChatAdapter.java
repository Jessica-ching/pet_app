package com.example.pet_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ChatMessage> messageList;

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        // 根據訊息物件裡的布林值決定類型
        return messageList.get(position).isUser() ? 1 : 0;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) { // 使用者
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new MessageViewHolder(v);
        } else { // AI
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_ai, parent, false);
            return new MessageViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        ((MessageViewHolder) holder).tvMessage.setText(message.getContent());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // 簡單的 ViewHolder 即可，因為兩個 XML 的 TextView ID 都叫 tvMessage
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        MessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}
