package com.example.pet_app.mainfeature.pet;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.R;
import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {
    private List<PetModel> petList;
    private OnPetClickListener listener;

    public interface OnPetClickListener { void onPetClick(PetModel pet); }

    public PetAdapter(List<PetModel> petList, OnPetClickListener listener) {
        this.petList = petList;
        this.listener = listener;
    }

    public int getExpandedPetId() {
        for (PetModel pet : petList) {
            if (pet.isExpanded()) return pet.getId();
        }
        return -1;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pet, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        PetModel pet = petList.get(position);
        if (pet == null) return;

        if (holder.tvPetName != null) holder.tvPetName.setText(pet.getName());

        // 🌟 1. 處理「食物熱量」進度與文字
        int currentCals = pet.getCurrentCals();
        int goalCals = pet.getGoalCals() > 0 ? pet.getGoalCals() : 1;
        if (holder.pbFood != null) {
            holder.pbFood.setMax(100); // 確保是百分比
            holder.pbFood.setProgress((int) ((currentCals / (float) goalCals) * 100));
        }
        if (holder.tvFoodStats != null) holder.tvFoodStats.setText(currentCals + "/" + goalCals + " 大卡");

        // 🌟 2. 修正：處理「飲水量」進度與文字
        int currentWater = pet.getCurrentWater();
        int goalWater = pet.getGoalWater() > 0 ? pet.getGoalWater() : 1;
        if (holder.pbWater != null) {
            holder.pbWater.setMax(100); // 確保是百分比
            holder.pbWater.setProgress((int) ((currentWater / (float) goalWater) * 100));
        }
        if (holder.tvWaterProgress != null) holder.tvWaterProgress.setText(currentWater + "/" + goalWater + " 毫升");

        // 控制展開/收合
        if (holder.layoutDetail != null) {
            holder.layoutDetail.setVisibility(pet.isExpanded() ? View.VISIBLE : View.GONE);
        }

        // 點擊整張卡片：展開或收合
        holder.itemView.setOnClickListener(v -> {
            boolean currentlyExpanded = pet.isExpanded();
            for (PetModel p : petList) p.setExpanded(false);
            pet.setExpanded(!currentlyExpanded);
            notifyDataSetChanged();
        });

        // 點擊展開的區域 (進度條旁邊)：跳轉到詳情頁
        if (holder.layoutDetail != null) {
            holder.layoutDetail.setOnClickListener(v -> {
                if (listener != null) listener.onPetClick(pet);
            });
        }

        // 點擊按鈕：跳轉到新增額外進食
        if (holder.btnAddFeeding != null) {
            holder.btnAddFeeding.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), AddExtraFeedingActivity.class);
                intent.putExtra("PET_ID", pet.getId());
                intent.putExtra("PET_NAME", pet.getName());
                v.getContext().startActivity(intent);
            });
        }

        if (holder.btnSettingsAll != null) {
            holder.btnSettingsAll.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), com.example.pet_app.mainfeature.pet.PetInfoActivity.class);
                intent.putExtra("PET_ID", pet.getId());
                intent.putExtra("PET_NAME", pet.getName());
                intent.putExtra("IS_EDIT_MODE", true);
                v.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() { return petList != null ? petList.size() : 0; }

    public static class PetViewHolder extends RecyclerView.ViewHolder {
        TextView tvPetName, tvFoodStats;
        // 🌟 修正：補上飲水文字欄位
        TextView tvWaterProgress;
        ProgressBar pbFood;
        // 🌟 修正：補上飲水進度條
        ProgressBar pbWater;
        View layoutDetail;
        Button btnAddFeeding;
        ImageButton btnSettingsAll;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPetName = itemView.findViewById(R.id.tv_daily_pet_name);
            tvFoodStats = itemView.findViewById(R.id.tv_food_stats);
            pbFood = itemView.findViewById(R.id.pb_food_daily);

            // 🌟 修正：用 findViewById 綁定 item_pet.xml 裡面的飲水 UI 元件
            tvWaterProgress = itemView.findViewById(R.id.tv_water_progress);
            pbWater = itemView.findViewById(R.id.pb_water_daily);

            layoutDetail = itemView.findViewById(R.id.layout_detail);
            btnAddFeeding = itemView.findViewById(R.id.btn_add_feeding);
            btnSettingsAll = itemView.findViewById(R.id.btn_settings_all);
        }
    }
}