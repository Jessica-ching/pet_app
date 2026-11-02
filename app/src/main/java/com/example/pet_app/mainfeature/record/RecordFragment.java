package com.example.pet_app.mainfeature.record;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.HomeActivity;
import com.example.pet_app.PetDropdownAdapter;
import com.example.pet_app.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RecordFragment extends Fragment {

    private CardView cardPetSelector;
    private TextView tvPetName;
    private ShapeableImageView imgPetAvatar;

    private int currentUserId = -1;
    private int selectedPetId = -1; // 當前選中的 PetID
    private List<PetDropdownAdapter.PetDropdownModel> userPetList = new ArrayList<>();

    public RecordFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);

        // 綁定 UI
        cardPetSelector = view.findViewById(R.id.card_pet_selector);
        tvPetName = view.findViewById(R.id.tv_pet_name_in_record);
        imgPetAvatar = view.findViewById(R.id.img_pet_avatar_in_record);

        // 讀取登入 ID
        SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("UserID", -1);

        // 初始化資料
        fetchUserPets();

        // 每日紀錄按鈕
        view.findViewById(R.id.btn_daily_record).setOnClickListener(v -> {
            if (selectedPetId == -1) {
                Toast.makeText(getContext(), "請先選擇寵物", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(getActivity(), DailyRecordActivity.class);
            intent.putExtra("PET_ID", selectedPetId);
            intent.putExtra("PET_NAME", tvPetName.getText().toString());
            startActivity(intent);
        });

        // 統計管理按鈕
        view.findViewById(R.id.btn_chart_manage).setOnClickListener(v -> {
            if (selectedPetId == -1) return;
            Intent intent = new Intent(getActivity(), ChartManagementActivity.class);
            intent.putExtra("PET_ID", selectedPetId);
            intent.putExtra("PET_NAME", tvPetName.getText().toString());
            startActivity(intent);
        });

        // 醫療清單按鈕
        view.findViewById(R.id.btn_medical_record).setOnClickListener(v -> {
            if (selectedPetId == -1) return;
            Intent intent = new Intent(getActivity(), MedicalListActivity.class);
            intent.putExtra("PET_ID", selectedPetId);
            intent.putExtra("PET_NAME", tvPetName.getText().toString());
            startActivity(intent);
        });

        // 點擊卡片彈出選擇器
        cardPetSelector.setOnClickListener(this::showPetListPopup);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到這個畫面都刷新一次清單，確保資料庫有變動時能同步
        fetchUserPets();
    }

    // 🌟 從資料庫抓取寵物名單
    private void fetchUserPets() {
        if (currentUserId == -1) {
            tvPetName.setText("請先登入");
            return;
        }

        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                String sql = "SELECT PetID, PetName, Species, Image FROM Pets WHERE UserID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, currentUserId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        List<PetDropdownAdapter.PetDropdownModel> tempList = new ArrayList<>(); // 先存入暫存清單
                        while (rs.next()) {
                            tempList.add(new PetDropdownAdapter.PetDropdownModel(
                                rs.getInt("PetID"), 
                                rs.getString("PetName"), 
                                rs.getString("Species"),
                                rs.getBytes("Image")
                            ));
                        }

                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                userPetList.clear();
                                userPetList.addAll(tempList);

                                if (!userPetList.isEmpty() && selectedPetId == -1) {
                                    // 只有在還沒選過寵物的情況下，才自動選第一隻
                                    updateSelectedPetUI(userPetList.get(0));
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showPetListPopup(View anchor) {
        if (userPetList.isEmpty()) {
            Toast.makeText(getContext(), "尚無寵物資料，請先新增", Toast.LENGTH_SHORT).show();
            return;
        }

        PetDropdownAdapter adapter = new PetDropdownAdapter(getContext(), userPetList);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("選擇寵物")
                .setAdapter(adapter, (d, which) -> {
                    updateSelectedPetUI(userPetList.get(which));
                })
                .create();
        dialog.show();
    }

    private void updateSelectedPetUI(PetDropdownAdapter.PetDropdownModel pet) {
        this.selectedPetId = pet.id;
        tvPetName.setText(pet.name);

        if (pet.image != null && pet.image.length > 0) {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(pet.image, 0, pet.image.length);
            imgPetAvatar.setImageBitmap(bitmap);
        } else {
            // 根據種類更換預設頭像
            if (pet.species.contains("貓")) {
                imgPetAvatar.setImageResource(R.drawable.cat_placeholder);
            } else {
                imgPetAvatar.setImageResource(R.drawable.dog_placeholder);
            }
        }

        // 同步回 Activity (讓 HomeFragment 也能知道切換了)
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).currentPetName = pet.name;
        }
    }

}