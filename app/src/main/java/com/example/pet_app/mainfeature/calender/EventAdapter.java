package com.example.pet_app.mainfeature.calender;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.R;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<EventModel> eventList;

    public EventAdapter(List<EventModel> eventList) {
        this.eventList = eventList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventModel model = eventList.get(position);

        // --- 1. 處理左側大數字 (只抓取日期) ---
        String fullDate = model.getDate();
        if (fullDate != null && fullDate.contains("/")) {
            String[] parts = fullDate.split("/");
            // parts[2] 就是日期，例如 "01"
            holder.tvDate.setText(parts[2]);
        } else {
            holder.tvDate.setText(fullDate);
        }

        // --- 2. 處理乾淨的標題 ---
        // 假設 model.getTitle()="kk", model.getSubtitle()="📅 安安\n(2026/06...)"
        String cleanTitle = model.getTitle();
        String subtitle = model.getSubtitle();

        if (subtitle != null && !subtitle.isEmpty()) {
            // 切掉括號與日期，只保留 "看醫生" 或 "安安" 等字眼
            String cleanSubtitle = subtitle.split("\\(")[0].replace("📅", "").trim();
            cleanTitle = cleanTitle + " - " + cleanSubtitle;
        }
        holder.tvTitle.setText(cleanTitle);

        // --- 3. 處理 CheckBox 與時間標籤的切換顯示 ---
        if (model.getTimeTag() != null && !model.getTimeTag().isEmpty()) {
            holder.tvTime.setVisibility(View.VISIBLE);
            holder.tvTime.setText(model.getTimeTag());
            holder.cbDone.setVisibility(View.GONE);
        } else {
            holder.tvTime.setVisibility(View.GONE);
            holder.cbDone.setVisibility(View.VISIBLE);

            // 避免 RecyclerView 重用 View 導致的監聽器錯亂
            holder.cbDone.setOnCheckedChangeListener(null);
            holder.cbDone.setChecked(model.isDone());
            holder.cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                model.setDone(isChecked);
            });
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public void updateList(List<EventModel> newList) {
        this.eventList = newList;
        notifyDataSetChanged();
    }

    // 內部的 ViewHolder，對應新的精簡版 XML
    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTitle, tvTime;
        CheckBox cbDone;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_item_date);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvTime = itemView.findViewById(R.id.tv_item_time);
            cbDone = itemView.findViewById(R.id.cb_event_done);
        }
    }
}