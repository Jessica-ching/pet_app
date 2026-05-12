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

    // 🌟 讓 Fragment 知道現在是哪隻寵物展開了 (防止返回時閃爍縮回)
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

        int current = pet.getCurrentCals();
        int goal = pet.getGoalCals() > 0 ? pet.getGoalCals() : 1;
        if (holder.pbFood != null) holder.pbFood.setProgress((int) ((current / (float) goal) * 100));
        if (holder.tvFoodStats != null) holder.tvFoodStats.setText(current + "/" + goal + " 大卡");

        // 🌟 1. 控制展開/收合
        if (holder.layoutDetail != null) {
            holder.layoutDetail.setVisibility(pet.isExpanded() ? View.VISIBLE : View.GONE);
        }

        // 🌟 2. 點擊整張卡片：展開或收合
        holder.itemView.setOnClickListener(v -> {
            boolean currentlyExpanded = pet.isExpanded();
            // 點擊時，把其他寵物先收起來，只展開點擊的這隻
            for (PetModel p : petList) p.setExpanded(false);
            pet.setExpanded(!currentlyExpanded);
            notifyDataSetChanged(); // 刷新動畫
        });

        // 🌟 3. 點擊展開的區域 (進度條旁邊)：跳轉到詳情頁 (圖三)
        if (holder.layoutDetail != null) {
            holder.layoutDetail.setOnClickListener(v -> {
                if (listener != null) listener.onPetClick(pet);
            });
        }

        // 🌟 4. 點擊按鈕：跳轉到新增額外進食
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
                // 🌟 修正：導向 PetInfoActivity
                Intent intent = new Intent(v.getContext(), com.example.pet_app.mainfeature.pet.PetInfoActivity.class);

                // 傳遞必要資訊
                intent.putExtra("PET_ID", pet.getId());
                intent.putExtra("PET_NAME", pet.getName());

                // 如果你的 PetInfoActivity 需要區分是「查看」還是「編輯」，可以保留這個 Flag
                intent.putExtra("IS_EDIT_MODE", true);

                v.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() { return petList != null ? petList.size() : 0; }

    public static class PetViewHolder extends RecyclerView.ViewHolder {
        TextView tvPetName, tvFoodStats;
        ProgressBar pbFood;
        View layoutDetail;
        Button btnAddFeeding;
        ImageButton btnSettingsAll;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPetName = itemView.findViewById(R.id.tv_daily_pet_name);
            tvFoodStats = itemView.findViewById(R.id.tv_food_stats);
            pbFood = itemView.findViewById(R.id.pb_food_daily);
            layoutDetail = itemView.findViewById(R.id.layout_detail); // 包住進度條跟按鈕的 Layout
            btnAddFeeding = itemView.findViewById(R.id.btn_add_feeding);
            btnSettingsAll = itemView.findViewById(R.id.btn_settings_all);
        }
    }
}