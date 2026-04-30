package com.example.pet_app.mainfeature.record;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.R;

import java.util.ArrayList;
import java.util.List;

public class MedicalAdapter extends RecyclerView.Adapter<MedicalAdapter.ViewHolder> {
    private List<MedicalModel> mList;

    public MedicalAdapter(List<MedicalModel> list) {
        this.mList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicalModel data = mList.get(position);
        holder.tvTitle.setText(data.getTitle());
        holder.tvDate.setText(data.getDate());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate;
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_medical_title);
            tvDate = itemView.findViewById(R.id.tv_medical_date);
        }
    }
}
