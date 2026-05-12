package com.example.pet_app.mainfeature.calender;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
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

        // --- 修改這部分：拆解日期字串 ---
        String fullDate = model.getDate(); // 假設格式是 "2026/4/12"
        if (fullDate != null && fullDate.contains("/")) {
            String[] parts = fullDate.split("/");

            // parts[1] 是月份，parts[2] 是日期
            String monthStr = parts[1] + "月";
            String dayNum = parts[2];

            holder.tvMonth.setText(monthStr);
            holder.tvDateNum.setText(dayNum);
        } else {
            holder.tvDateNum.setText(fullDate); // 防呆：如果格式不對就顯示原樣
        }
        // ----------------------------

        holder.tvTitle.setText(model.getTitle());
        holder.tvSubtitle.setText(model.getSubtitle());
        holder.ivIcon.setImageResource(model.getIconResId());

        // --- 剩下的 CheckBox 邏輯保持不變 ---
        if (model.getTimeTag() != null && !model.getTimeTag().isEmpty()) {
            holder.tvTimeTag.setVisibility(View.VISIBLE);
            holder.tvTimeTag.setText(model.getTimeTag());
            holder.cbDone.setVisibility(View.GONE);
        } else {
            holder.tvTimeTag.setVisibility(View.GONE);
            holder.cbDone.setVisibility(View.VISIBLE);
            holder.cbDone.setChecked(model.isDone());

            // 避免 RecyclerView 重用 View 導致的監聽器錯亂，先清空再設定
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

    // 內部的 ViewHolder，負責找到 XML 裡的元件
    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth, tvDateNum, tvTitle, tvSubtitle, tvTimeTag;
        ImageView ivIcon;
        CheckBox cbDone;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tv_event_month);
            tvDateNum = itemView.findViewById(R.id.tv_event_date_num);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvSubtitle = itemView.findViewById(R.id.tv_event_subtitle);
            ivIcon = itemView.findViewById(R.id.iv_event_icon);
            cbDone = itemView.findViewById(R.id.cb_event_done);
            tvTimeTag = itemView.findViewById(R.id.tv_event_time_tag);
        }
    }
}