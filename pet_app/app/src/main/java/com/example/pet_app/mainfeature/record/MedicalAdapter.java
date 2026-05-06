package com.example.pet_app.mainfeature.record;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.R;
import java.util.List;

public class MedicalAdapter extends RecyclerView.Adapter<MedicalAdapter.MedicalViewHolder> {

    private List<MedicalModel> medicalList;

    public MedicalAdapter(List<MedicalModel> medicalList) {
        this.medicalList = medicalList;
    }

    @NonNull
    @Override
    public MedicalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medical, parent, false);
        return new MedicalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicalViewHolder holder, int position) {
        MedicalModel record = medicalList.get(position);

        // 🌟 只保留日期跟原因，完美對齊你的截圖畫面
        if (holder.tvDate != null) holder.tvDate.setText(record.getDate());
        if (holder.tvReason != null) holder.tvReason.setText(record.getReason());
    }

    @Override
    public int getItemCount() {
        return medicalList != null ? medicalList.size() : 0;
    }

    public void setList(List<MedicalModel> list) {
        this.medicalList = list;
        notifyDataSetChanged();
    }

    public static class MedicalViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvReason; // 🌟 把 tvClinic 拿掉了

        public MedicalViewHolder(@NonNull View itemView) {
            super(itemView);
            // 🌟 只綁定這兩個 ID，就不會再報錯找不到 tv_clinic_name 囉！
            tvDate = itemView.findViewById(R.id.tv_medical_date);
            tvReason = itemView.findViewById(R.id.tv_medical_reason);
        }
    }
}