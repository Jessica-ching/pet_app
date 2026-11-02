package com.example.pet_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class PetDropdownAdapter extends BaseAdapter {

    private Context context;
    private List<PetDropdownModel> petList;

    public PetDropdownAdapter(Context context, List<PetDropdownModel> petList) {
        this.context = context;
        this.petList = petList;
    }

    @Override
    public int getCount() {
        return petList.size();
    }

    @Override
    public Object getItem(int position) {
        return petList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return petList.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_pet_dropdown, parent, false);
        }

        PetDropdownModel pet = petList.get(position);
        ImageView ivIcon = convertView.findViewById(R.id.iv_pet_dropdown_icon);
        TextView tvName = convertView.findViewById(R.id.tv_pet_dropdown_name);

        tvName.setText(pet.name);
        if (pet.image != null && pet.image.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(pet.image, 0, pet.image.length);
            ivIcon.setImageBitmap(bitmap);
        } else {
            if (pet.species != null && pet.species.contains("貓")) {
                ivIcon.setImageResource(R.drawable.cat_placeholder);
            } else {
                ivIcon.setImageResource(R.drawable.dog_placeholder);
            }
        }

        return convertView;
    }

    public static class PetDropdownModel {
        public int id;
        public String name;
        public String species;
        public byte[] image;

        public PetDropdownModel(int id, String name, String species, byte[] image) {
            this.id = id;
            this.name = name;
            this.species = species;
            this.image = image;
        }
    }
}