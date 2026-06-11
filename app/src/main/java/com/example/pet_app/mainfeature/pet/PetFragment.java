package com.example.pet_app.mainfeature.pet;

import static android.content.Context.MODE_PRIVATE;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PetFragment extends Fragment {
    private RecyclerView rvPetList;
    private Button btnAddNewPet;
    private TextView tvLoginHint;
    private List<PetModel> petList = new ArrayList<>();
    private PetAdapter petAdapter;
    private int currentUserId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_pet, container, false);

        rvPetList = v.findViewById(R.id.rv_pet_list);
        btnAddNewPet = v.findViewById(R.id.btn_add_new_pet);
        tvLoginHint = v.findViewById(R.id.tv_login_hint);

        if (rvPetList != null) rvPetList.setLayoutManager(new LinearLayoutManager(getContext()));

        if (btnAddNewPet != null) {
            btnAddNewPet.setOnClickListener(view -> {
                Intent intent = new Intent(getActivity(), com.example.pet_app.login.CreatePetInfoActivity.class);
                startActivity(intent);
            });
        }

        return v;
    }

    public void loadPetData() {
        if (checkLoginStatus()) {
            if (tvLoginHint != null) tvLoginHint.setVisibility(View.GONE);
            if (rvPetList != null) rvPetList.setVisibility(View.VISIBLE);
            if (btnAddNewPet != null) btnAddNewPet.setVisibility(View.VISIBLE);
            fetchUserPetsFromDB();
        } else {
            if (tvLoginHint != null) tvLoginHint.setVisibility(View.VISIBLE);
            if (rvPetList != null) rvPetList.setVisibility(View.GONE);
            if (btnAddNewPet != null) btnAddNewPet.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPetData();
    }

    private boolean checkLoginStatus() {
        if (getContext() == null) return false;
        SharedPreferences prefs = getContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("UserID", -1);
        return currentUserId != -1;
    }

    private void fetchUserPetsFromDB() {
        if (!isAdded() || getContext() == null) return;

        new Thread(() -> {
            List<PetModel> tempList = new ArrayList<>();

            // 🌟 最安全的寫法：先用最單純的 SQL 把寵物抓出來，避免因為子查詢失敗導致整頁寵物消失
            String sqlPets = "SELECT * FROM Pets WHERE UserID = ?";

            try (Connection conn = ConnectionHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlPets)) {

                pstmt.setInt(1, currentUserId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        PetModel pet = new PetModel(
                                rs.getInt("PetID"),
                                rs.getString("PetName"),
                                "", "", 0,
                                rs.getFloat("Weight")
                        );

                        // 🌟 預防欄位不存在：加個預設目標值防呆
                        int recommendCals = 1200; // 給個預設值
                        int recommendWater = 250;
                        try { recommendCals = rs.getInt("RecommendCalories"); } catch (Exception e) {}
                        try { recommendWater = rs.getInt("RecommendWater"); } catch (Exception e) {}

                        pet.setGoalCals(recommendCals);
                        pet.setGoalWater(recommendWater);

                        int petId = pet.getId();

                        // 🌟 個別撈今日熱量 (萬一 DailyFood 壞掉也不會影響別人)
                        try {
                            // 使用 COALESCE 替代 ISNULL，因為 MySQL/SQL Server 通用
                            String sqlFood = "SELECT COALESCE(SUM(Calories), 0) FROM DailyFood WHERE PetID = ? AND RecordDate = CAST(GETDATE() AS DATE)";
                            // 如果你是用 MySQL，上面的 CAST(GETDATE() AS DATE) 如果報錯，改用 CURDATE()
                            try (PreparedStatement pFood = conn.prepareStatement(sqlFood)) {
                                pFood.setInt(1, petId);
                                try (ResultSet rFood = pFood.executeQuery()) {
                                    if (rFood.next()) pet.setCurrentCals(rFood.getInt(1));
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("PetFragment", "抓取每日熱量失敗，可能是 RecordDate 的格式或函數問題", e);
                        }

                        // 🌟 個別撈今日飲水 (就算 DailyWater 還沒建表，也只會抓到 0 毫升，寵物絕對不會消失！)
                        try {
                            String sqlWater = "SELECT COALESCE(SUM(WaterML), 0) FROM DailyWater WHERE PetID = ? AND RecordDate = CAST(GETDATE() AS DATE)";
                            try (PreparedStatement pWater = conn.prepareStatement(sqlWater)) {
                                pWater.setInt(1, petId);
                                try (ResultSet rWater = pWater.executeQuery()) {
                                    if (rWater.next()) pet.setCurrentWater(rWater.getInt(1));
                                }
                            }
                        } catch (Exception e) {
                            // 這裡會印出錯誤，讓你知道是不是 DailyWater 表格名稱不對
                            android.util.Log.e("PetFragment", "抓取飲水量失敗！請確認你的資料庫有沒有 DailyWater 這個表，或是欄位有沒有 Ml", e);
                            pet.setCurrentWater(0); // 失敗就當作 0
                        }

                        tempList.add(pet);
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("PetFragment", "寵物列表大崩潰，連連線都失敗：", e);
                e.printStackTrace();
            }

            // 以下更新 UI 的部分維持原樣
            if (isAdded() && getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded() && getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {

                        int expandedId = -1;
                        if (petAdapter != null) {
                            expandedId = petAdapter.getExpandedPetId();
                        }

                        petList.clear();
                        for (PetModel p : tempList) {
                            if (p.getId() == expandedId) p.setExpanded(true);
                            petList.add(p);
                        }

                        if (petAdapter == null) {
                            petAdapter = new PetAdapter(petList, pet -> {
                                Intent intent = new Intent(getActivity(), com.example.pet_app.mainfeature.record.MedicalListActivity.class);
                                intent.putExtra("PET_ID", pet.getId());
                                intent.putExtra("PET_NAME", pet.getName());
                                startActivity(intent);
                            });
                            if (rvPetList != null) rvPetList.setAdapter(petAdapter);
                        } else {
                            petAdapter.notifyDataSetChanged();
                        }

                        // 偵錯提示：
                        if (petList.isEmpty()) {
                            android.widget.Toast.makeText(getContext(), "注意：沒撈到任何寵物資料！", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }
}