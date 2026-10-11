package com.example.pet_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
        // 根據訊息物件裡的布林值決定類型：1 代表使用者，0 代表 AI
        return messageList.get(position).isUser() ? 1 : 0;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) { // 使用者訊息
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new MessageViewHolder(v);
        } else { // AI 訊息
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_ai, parent, false);
            return new MessageViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        MessageViewHolder vh = (MessageViewHolder) holder;

        // 設定基本文字內容
        vh.tvMessage.setText(message.getContent());

        // 處理 AI 訊息專屬的醫院卡片動態渲染
        if (!message.isUser() && vh.llHospitalContainer != null) {
            // 清除之前的舊 View，避免 RecyclerView 重複使用 (Recycle) 時產生畫面重疊
            vh.llHospitalContainer.removeAllViews();

            if (message.hasHospitals()) {
                vh.llHospitalContainer.setVisibility(View.VISIBLE);
                Context context = vh.itemView.getContext();

                for (Hospital hospital : message.getHospitalList()) {
                    // 動態載入醫院卡片 Layout
                    View cardView = LayoutInflater.from(context).inflate(R.layout.item_hospital_card, vh.llHospitalContainer, false);

                    TextView tvName = cardView.findViewById(R.id.tv_hospital_name);
                    TextView tvAddr = cardView.findViewById(R.id.tv_hospital_address);
                    TextView tvPhone = cardView.findViewById(R.id.tv_hospital_phone);

                    tvName.setText("🏥 " + hospital.getName());
                    tvAddr.setText("📍 " + hospital.getAddress());
                    tvPhone.setText("📞 " + hospital.getPhone());

                    // 點擊電話：啟動系統電話撥號頁面
                    tvPhone.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + hospital.getPhone()));
                        context.startActivity(intent);
                    });

                    // 點擊地址：開啓 Google Maps 進行地點搜尋或導航
                    tvAddr.setOnClickListener(v -> {
                        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(hospital.getName() + " " + hospital.getAddress()));
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");

                        // 若裝置沒有安裝預設 Google Maps app，改以一般瀏覽器開啟地圖
                        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                            context.startActivity(mapIntent);
                        } else {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(hospital.getName() + " " + hospital.getAddress())));
                            context.startActivity(browserIntent);
                        }
                    });

                    // 將組裝好的卡片加進容器中
                    vh.llHospitalContainer.addView(cardView);
                }
            } else {
                vh.llHospitalContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // 更新 ViewHolder 以便綁定醫院容器
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        LinearLayout llHospitalContainer; // AI 訊息專用的容器

        MessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            llHospitalContainer = itemView.findViewById(R.id.llHospitalContainer);
        }
    }
}