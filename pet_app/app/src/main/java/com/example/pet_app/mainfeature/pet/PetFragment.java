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
    private TextView tvLoginHint; // 🌟 把消失的提示文字加回來
    private List<PetModel> petList = new ArrayList<>();
    private PetAdapter petAdapter;
    private int currentUserId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_pet, container, false);

        // 綁定畫面元件
        rvPetList = v.findViewById(R.id.rv_pet_list);
        btnAddNewPet = v.findViewById(R.id.btn_add_new_pet);
        tvLoginHint = v.findViewById(R.id.tv_login_hint); // 🌟 綁定提示文字

        if (rvPetList != null) rvPetList.setLayoutManager(new LinearLayoutManager(getContext()));

        if (btnAddNewPet != null) {
            btnAddNewPet.setOnClickListener(view -> {
                Intent intent = new Intent(getActivity(), com.example.pet_app.login.CreatePetInfoActivity.class);
                startActivity(intent);
            });
        }

        return v;
    }

    // 🌟 處理 UI 顯示切換
    public void loadPetData() {
        if (checkLoginStatus()) {
            // 登入成功：把「請先登入」藏起來，顯示清單跟按鈕
            if (tvLoginHint != null) tvLoginHint.setVisibility(View.GONE);
            if (rvPetList != null) rvPetList.setVisibility(View.VISIBLE);
            if (btnAddNewPet != null) btnAddNewPet.setVisibility(View.VISIBLE);
            fetchUserPetsFromDB();
        } else {
            // 沒登入：顯示「請先登入」，把清單跟按鈕藏起來
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
        return currentUserId != -1; // 有抓到 ID 就回傳 true
    }

    private void fetchUserPetsFromDB() {
        if (!isAdded() || getContext() == null) return;

        new Thread(() -> {
            List<PetModel> tempList = new ArrayList<>();
            try (Connection conn = ConnectionHelper.getConnection()) {
                String sql = "SELECT p.*, " +
                        "(SELECT ISNULL(SUM(Calories), 0) FROM DailyFood WHERE PetID = p.PetID AND RecordDate = CAST(GETDATE() AS DATE)) as TodayCals " +
                        "FROM Pets p WHERE p.UserID = ?";

                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, currentUserId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    PetModel pet = new PetModel(rs.getInt("PetID"), rs.getString("PetName"), "", "", 0, rs.getFloat("Weight"));
                    pet.setCurrentCals(rs.getInt("TodayCals"));
                    pet.setGoalCals(rs.getInt("RecommendCalories"));
                    tempList.add(pet);
                }
            } catch (Exception e) { e.printStackTrace(); }

            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // 記住展開狀態，防止閃爍
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
                        // Inside PetFragment.java setupAdapterWithClick()
                        petAdapter = new PetAdapter(petList, pet -> {
                            // 1. 初始化 Intent 準備跳轉到就醫紀錄清單
                            Intent intent = new Intent(getActivity(), com.example.pet_app.mainfeature.record.MedicalListActivity.class);

                            // 2. 🌟 關鍵修正：同時傳送 ID 與 名字
                            intent.putExtra("PET_ID", pet.getId());
                            intent.putExtra("PET_NAME", pet.getName()); // 🌟 傳送「小黑」名字

                            startActivity(intent);
                        });
                        if (rvPetList != null) rvPetList.setAdapter(petAdapter);
                    } else {
                        petAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }
}