package com.example.pet_app.mainfeature.pet;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;
import com.example.pet_app.login.CreatePetInfoActivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PetFragment extends Fragment {

    private boolean isSelectEditMode = false;
    private RecyclerView rvPetList;
    private TextView tvLoginHint, tvMainTitle, tvNoDataHint;
    private Button btnAddNewPet;
    private ImageButton btnSettings;
    private List<PetModel> petList = new ArrayList<>();
    private PetAdapter petAdapter;
    private int currentUserId = -1;

    public PetFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pet, container, false);

        // 初始化元件
        rvPetList = view.findViewById(R.id.rv_pet_list);
        tvLoginHint = view.findViewById(R.id.tv_login_hint);
        tvMainTitle = view.findViewById(R.id.tv_main_title);
        tvNoDataHint = view.findViewById(R.id.tv_no_data_hint);
        btnAddNewPet = view.findViewById(R.id.btn_add_new_pet);
        btnSettings = view.findViewById(R.id.btn_settings_all);

        rvPetList.setLayoutManager(new LinearLayoutManager(getContext()));

        // 🌟 修正點：直接呼叫這個函式，確保一開始就有點擊事件
        setupAdapterWithClick();

        btnAddNewPet.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreatePetInfoActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            isSelectEditMode = true;
            Toast.makeText(getContext(), "模式切換：請選擇欲編輯的寵物", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到畫面都檢查登入狀態並更新清單
        if (checkLoginStatus()) {
            fetchUserPetsFromDB();
        } else {
            showLoginHint();
        }
    }

    private boolean checkLoginStatus() {
        if (getContext() == null) return false;
        SharedPreferences prefs = getContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("UserID", -1);
        return currentUserId != -1;
    }

    // 🌟 核心：從 MSSQL 撈取使用者的寵物
    private void fetchUserPetsFromDB() {
        if (!isAdded()) return; // 確保 Fragment 還在畫面上

        tvLoginHint.setVisibility(View.GONE);
        btnAddNewPet.setVisibility(View.VISIBLE);
        btnSettings.setVisibility(View.VISIBLE);

        new Thread(() -> {
            List<PetModel> tempList = new ArrayList<>();
            boolean isSuccess = false;

            try (Connection conn = ConnectionHelper.getConnection()) {
                if (conn != null) {
                    // 這裡建議檢查 currentUserId 是否為 -1
                    String sql = "SELECT PetID, PetName, Species, Gender, Birthday, Weight FROM Pets WHERE UserID = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setInt(1, currentUserId);
                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        String birthday = rs.getString("Birthday");
                        int age = 0;
                        if (birthday != null && !birthday.isEmpty()) {
                            try {
                                String[] parts = birthday.split("/");
                                if (parts.length >= 1) {
                                    int birthYear = Integer.parseInt(parts[0]);
                                    int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                                    age = currentYear - birthYear;
                                }
                            } catch (Exception e) {
                                age = 0;
                            }
                        }

                        tempList.add(new PetModel(
                                rs.getInt("PetID"),
                                rs.getString("PetName"),
                                rs.getString("Species"),
                                rs.getString("Gender"),
                                age,
                                rs.getFloat("Weight")
                        ));
                    }
                    isSuccess = true;
                }
            } catch (Exception e) {
                android.util.Log.e("DB_ERROR", "無法獲取寵物列表: " + e.getMessage());
            }

            // 回到 UI 執行緒
            if (isAdded() && getActivity() != null) {
                final boolean finalIsSuccess = isSuccess;
                getActivity().runOnUiThread(() -> {
                    petList.clear();
                    petList.addAll(tempList);

                    // 🌟 重要：重新建立或更新點擊事件 (處理跳轉)
                    setupAdapterWithClick();

                    updatePetUI(); // 切換顯示/隱藏狀態

                    if (!finalIsSuccess) {
                        Toast.makeText(getContext(), "連線失敗，請檢查網路或資料庫設定", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void setupAdapterWithClick() {
        if (petAdapter == null) {
            // 第一次載入時建立 Adapter
            petAdapter = new PetAdapter(petList, pet -> {
                Intent intent;
                if (isSelectEditMode) {
                    // 模式 A: 點擊後去修改資料 (PetInfoActivity)
                    intent = new Intent(getActivity(), PetInfoActivity.class);
                    isSelectEditMode = false; // 點完後自動關閉編輯模式，變回一般模式
                } else {
                    // 模式 B: 一般點擊去記錄 (PetDailyStatusActivity)
                    intent = new Intent(getActivity(), PetDailyStatusActivity.class);
                }
                intent.putExtra("PET_ID", pet.getId());
                intent.putExtra("PET_NAME", pet.getName());
                startActivity(intent);
            });
            rvPetList.setAdapter(petAdapter);
        } else {
            // 後續更新時，只需要通知 Adapter 資料變了
            petAdapter.notifyDataSetChanged();
        }
    }

    public void updatePetUI() {
        if (!isAdded()) return;

        if (currentUserId == -1) {
            showLoginHint(); // 沒登入就顯示提示
            return;
        }

        tvLoginHint.setVisibility(View.GONE);
        btnAddNewPet.setVisibility(View.VISIBLE);

        if (petList.isEmpty()) {
            rvPetList.setVisibility(View.GONE);
            tvNoDataHint.setVisibility(View.VISIBLE);
        } else {
            rvPetList.setVisibility(View.VISIBLE);
            tvNoDataHint.setVisibility(View.GONE);
        }
    }

    public void loadPetData() {
        //1. 從資料庫 (SQL Server) 或 API 抓取資料
        // 2. 抓取完成後更新 petList

        // 3. 關鍵步驟：更新 UI
        getActivity().runOnUiThread(() -> {
            if (petList != null && !petList.isEmpty()) {
                petAdapter.setList(petList); // 更新 Adapter 資料
                petAdapter.notifyDataSetChanged(); // 通知刷新
            }
            updatePetUI(); // 呼叫剛才寫的 UI 切換邏輯
        });
    }

    private void showLoginHint() {
        tvLoginHint.setVisibility(View.VISIBLE);
        rvPetList.setVisibility(View.GONE);
        btnAddNewPet.setVisibility(View.GONE);
        btnSettings.setVisibility(View.GONE);
        if (tvMainTitle != null) tvMainTitle.setText("寵物管理");
    }
}