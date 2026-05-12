package com.example.pet_app.mainfeature.pet;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddExtraFeedingActivity extends AppCompatActivity {

    private Spinner spinnerSnacks;
    private EditText etGrams;
    private int selectedPetId;

    private List<SnackModel> snackList = new ArrayList<>();
    private List<String> snackNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_extra_feeding);

        selectedPetId = getIntent().getIntExtra("PET_ID", -1);

        spinnerSnacks = findViewById(R.id.spinner_snacks);
        etGrams = findViewById(R.id.et_grams);

        // 返回按鈕
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // 儲存按鈕
        Button btnSave = findViewById(R.id.btn_save_feeding);
        if (btnSave != null) btnSave.setOnClickListener(v -> saveFeedingData());

        // 加入其他食物按鈕
        Button btnAddCustom = findViewById(R.id.btn_add_custom_food);
        if (btnAddCustom != null) {
            btnAddCustom.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddCustomFoodActivity.class);
                intent.putExtra("PET_ID", selectedPetId);
                startActivity(intent);
            });
        }

        fetchSnacksFromDB();
    }

    private void fetchSnacksFromDB() {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                String sql = "SELECT SnackID, Name, Calories, Gram FROM Snacks";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery();

                snackNames.clear();
                snackList.clear();
                snackNames.add("- 請選擇額外進食種類 -");
                snackList.add(new SnackModel(-1, "", 0, 0));

                while (rs.next()) {
                    SnackModel s = new SnackModel(rs.getInt("SnackID"), rs.getString("Name"), rs.getInt("Calories"), rs.getFloat("Gram"));
                    snackList.add(s);
                    snackNames.add(s.name);
                }

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, snackNames);
                    spinnerSnacks.setAdapter(adapter);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void saveFeedingData() {
        int pos = spinnerSnacks.getSelectedItemPosition();
        String gramStr = etGrams.getText().toString();

        if (pos <= 0 || gramStr.isEmpty()) {
            Toast.makeText(this, "請選擇食物並輸入公克數", Toast.LENGTH_SHORT).show();
            return;
        }

        SnackModel snack = snackList.get(pos);
        float inputGram = Float.parseFloat(gramStr);
        int finalCals = Math.round((inputGram / snack.gram) * snack.calories);

        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                String sql = "INSERT INTO DailyFood (PetID, Calories, RecordDate) VALUES (?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);

                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                pstmt.setInt(1, selectedPetId);
                pstmt.setInt(2, finalCals);
                pstmt.setString(3, today);

                pstmt.executeUpdate();

                runOnUiThread(() -> {
                    Toast.makeText(this, "儲存成功！", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 🌟 資料模型類別 (放在類別內部)
    static class SnackModel {
        int id; String name; int calories; float gram;
        SnackModel(int i, String n, int c, float g) {
            id = i; name = n; calories = c; gram = g;
        }
    }
} // 🌟 這是最後一個大括號，確保所有東西都在它裡面