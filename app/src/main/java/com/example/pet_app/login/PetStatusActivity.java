package com.example.pet_app.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.HomeActivity;
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class PetStatusActivity extends AppCompatActivity {

    private int selectedActivityPos = -1;
    private int selectedBodyConditionPos = -1;

    private String petName, petSpecies, petGender, petBirthday;
    private boolean isSterilized;
    private int selectedFoodID;
    private EditText etPetWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_status);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            petName = extras.getString("petName", "我的寵物");
            petSpecies = extras.getString("petSpecies", "貓");
            petGender = extras.getString("petGender", "公");
            petBirthday = extras.getString("petBirthday", "");
            isSterilized = extras.getBoolean("isSterilized", false);
            selectedFoodID = extras.getInt("selectedFoodID", -1);
        }

        TextView tvPetNameBadge = findViewById(R.id.tvPetNameBadge);
        etPetWeight = findViewById(R.id.etPetWeight);
        tvPetNameBadge.setText(petName);

        TextView tvActivityHeader = findViewById(R.id.tvActivityHeader);
        RecyclerView rvActivityList = findViewById(R.id.rvActivityList);
        List<String> activityData = Arrays.asList("低活動量 (靜態/室內)", "中活動量 (一般)", "高活動量 (好動/運動量大)");
        setupDropdown(tvActivityHeader, rvActivityList, activityData, true);

        TextView tvBodyConditionHeader = findViewById(R.id.tvBodyConditionHeader);
        RecyclerView rvBodyConditionList = findViewById(R.id.rvBodyConditionList);
        List<String> bodyConditionData = Arrays.asList("過瘦", "標準", "過胖");
        setupDropdown(tvBodyConditionHeader, rvBodyConditionList, bodyConditionData, false);

        Button btnNextStep = findViewById(R.id.btnNextStep);
        btnNextStep.setOnClickListener(v -> {
            String weightStr = etPetWeight.getText().toString().trim();
            if (weightStr.isEmpty() || selectedActivityPos == -1 || selectedBodyConditionPos == -1) {
                Toast.makeText(this, "請完整填寫體重、活動量與體態", Toast.LENGTH_SHORT).show();
                return;
            }

            float weight = Float.parseFloat(weightStr);
            savePetToDB(weight, activityData.get(selectedActivityPos), bodyConditionData.get(selectedBodyConditionPos));
        });
    }

    /**
     * 🌟 每日熱量計算 (DER) - 倍率疊加制
     */
    private double calculateDailyCalories(float weight, String activity, String body) {
        double rer = 70 * Math.pow(weight, 0.75);
        double factor = 1.0;
        int months = calculateMonths(petBirthday);

        if ("貓".equals(petSpecies)) {
            if (months <= 4) factor *= 2.5;
            else if (months <= 12) factor *= 2.0;
            else if (months >= 120) factor *= 1.1;
            else if (isSterilized) factor *= 1.2;
            else factor *= 1.4;

            if (activity.contains("高")) factor *= 1.2;
            else if (activity.contains("低")) factor *= 0.8;

            if ("過瘦".equals(body)) factor *= 1.2;
            else if ("過胖".equals(body)) factor *= 0.8;
        } else {
            if (months <= 4) factor *= 3.0;
            else if (months <= 12) factor *= 2.0;
            else if (isSterilized) factor *= 1.6;
            else factor *= 1.8;

            if (activity.contains("高")) factor *= 1.5;
            else if (activity.contains("低")) factor *= 1.2;

            if ("過瘦".equals(body)) factor *= 1.2;
            else if ("過胖".equals(body)) factor *= 0.8;
        }
        return rer * factor;
    }

    /**
     * 🌟 每日建議飲水量計算 - 體重 * 50ml
     */
    private int calculateDailyWater(float weight) {
        return Math.round(weight * 50);
    }

    private int calculateMonths(String birthday) {
        if (birthday == null || birthday.isEmpty()) return 24;
        try {
            String[] parts = birthday.split("-");
            int birthYear = Integer.parseInt(parts[0]);
            int birthMonth = Integer.parseInt(parts[1]);
            Calendar now = Calendar.getInstance();
            return (now.get(Calendar.YEAR) - birthYear) * 12 + (now.get(Calendar.MONTH) + 1 - birthMonth);
        } catch (Exception e) {
            return 24;
        }
    }

    private void savePetToDB(float weight, String activity, String body) {
        SharedPreferences pref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        int userId = pref.getInt("UserID", -1);

        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 計算建議值
                int dailyCals = (int) calculateDailyCalories(weight, activity, body);
                int dailyWater = calculateDailyWater(weight); // 🌟 使用 50ml 公式

                // 1. 存入 Pets 表
                String sql = "INSERT INTO Pets (UserID, PetName, Gender, Species, Birthday, Weight, FoodID, Activity, BodyType, IsSterilized, RecommendCalories, RecommendWater) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
                pstmt.setInt(1, userId);
                pstmt.setString(2, petName);
                pstmt.setString(3, petGender);
                pstmt.setString(4, petSpecies);
                pstmt.setString(5, petBirthday);
                pstmt.setFloat(6, weight);
                if (selectedFoodID != -1) pstmt.setInt(7, selectedFoodID); else pstmt.setNull(7, java.sql.Types.INTEGER);
                pstmt.setString(8, activity);
                pstmt.setString(9, body);
                pstmt.setBoolean(10, isSterilized);
                pstmt.setInt(11, dailyCals);
                pstmt.setInt(12, dailyWater);
                pstmt.executeUpdate();

                // 2. 取得新 PetID 並建立 DailyFood 與 DailyWater 的初始目標紀錄
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int newPetId = rs.getInt(1);

                    // 存入每日熱量初始紀錄
                    String foodSql = "INSERT INTO DailyFood (PetID, Calories, RecordDate) VALUES (?, ?, GETDATE())";
                    PreparedStatement fstmt = conn.prepareStatement(foodSql);
                    fstmt.setInt(1, newPetId);
                    fstmt.setInt(2, dailyCals);
                    fstmt.executeUpdate();

                    // 🌟 存入每日飲水初始紀錄 (如果你的 DailyWater 表結構允許)
                    String waterSql = "INSERT INTO DailyWater (PetID, WaterML, RecordDate) VALUES (?, 0, GETDATE())";
                    PreparedStatement wstmt = conn.prepareStatement(waterSql);
                    wstmt.setInt(1, newPetId);
                    wstmt.executeUpdate();
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "寵物資料建立成功！", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finishAffinity();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "存檔失敗: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void setupDropdown(TextView header, RecyclerView list, List<String> data, boolean isActivity) {
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new StatusAdapter(data, header, list, isActivity));
        header.setOnClickListener(v -> list.setVisibility(list.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
    }

    class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.ViewHolder> {
        private List<String> data; private TextView header; private RecyclerView list; private boolean isActivity;
        public StatusAdapter(List<String> d, TextView h, RecyclerView l, boolean a) { this.data = d; this.header = h; this.list = l; this.isActivity = a; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_food_box, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            String s = data.get(p); h.tvName.setText(s);
            h.itemView.setOnClickListener(v -> {
                if (isActivity) selectedActivityPos = h.getAdapterPosition(); else selectedBodyConditionPos = h.getAdapterPosition();
                header.setText(s); list.setVisibility(View.GONE); notifyDataSetChanged();
            });
        }
        @Override public int getItemCount() { return data.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { TextView tvName; ViewHolder(View v) { super(v); tvName = v.findViewById(R.id.tvFoodDetail); } }
    }
}