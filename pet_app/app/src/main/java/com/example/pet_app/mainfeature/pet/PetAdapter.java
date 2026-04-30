package com.example.pet_app.mainfeature.pet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.R;

import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.ViewHolder> {
    private List<PetModel> petList;
    private OnPetClickListener listener;

    public interface OnPetClickListener {
        void onPetClick(PetModel pet);
    }

    public PetAdapter(List<PetModel> list, OnPetClickListener listener) {
        this.petList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PetModel pet = petList.get(position);
        holder.tvName.setText(pet.getName());
        // 點擊名字卡片跳轉 P3
        holder.itemView.setOnClickListener(v -> listener.onPetClick(pet));
    }

    @Override
    public int getItemCount() {
        return petList != null ? petList.size() : 0;
    }

    public void setList(List<PetModel> list) {
        this.petList = list;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_pet_item_name);
        }
    }
}
