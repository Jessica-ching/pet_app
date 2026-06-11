package com.example.pet_app.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FoodSelectionActivity extends AppCompatActivity {

    private String selectedFoodName = "";
    private int selectedFoodID = -1;
    private int selectedPosition = -1;

    private TextView tvDropdownHeader;
    private RecyclerView rvFoodList;
    private EditText etSearchFood;

    private List<FoodItem> allFoodList = new ArrayList<>();   // 完整清單
    private List<FoodItem> filteredList = new ArrayList<>(); // 搜尋清單
    private FoodAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fodder_selection);

        // 1. 綁定元件
        tvDropdownHeader = findViewById(R.id.tvDropdownHeader);
        rvFoodList = findViewById(R.id.rvFoodList);
        etSearchFood = findViewById(R.id.etSearchFood);
        Button btnNextStep = findViewById(R.id.btnNextStep);
        View btnAICat = findViewById(R.id.btnAICat);

        // 2. 初始化 RecyclerView
        rvFoodList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FoodAdapter(filteredList);
        rvFoodList.setAdapter(adapter);

        // 3. 從資料庫抓取真實飼料資料
        fetchFoodFromDB();

        // 4. 點擊下拉標頭：切換清單顯示/隱藏
        tvDropdownHeader.setOnClickListener(v -> {
            if (rvFoodList.getVisibility() == View.GONE) {
                rvFoodList.setVisibility(View.VISIBLE);
            } else {
                rvFoodList.setVisibility(View.GONE);
            }
        });

        // 5. 🌟 搜尋框監聽器：輸入時自動過濾
        etSearchFood.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 6. AI 小幫手跳轉：將前面接收到的資料傳遞給 ChatActivity
        if (btnAICat != null) {
            btnAICat.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatActivity.class);

                // 🌟 關鍵：將上一頁傳來的 Bundle 直接轉傳給 ChatActivity
                // 這樣 ChatActivity 就能拿到 petSpecies, petBirthday, petGender, isSterilized
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    intent.putExtras(extras);
                }

                startActivity(intent);
            });
        }

        // 7. 下一步：攜帶所有資料去 PetStatusActivity
        btnNextStep.setOnClickListener(v -> {
            if (selectedFoodID == -1) {
                Toast.makeText(this, "請選擇飼料", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, PetStatusActivity.class);
                // 傳遞上一頁過來的全部資料 (姓名、生日、性別等)
                if (getIntent().getExtras() != null) {
                    intent.putExtras(getIntent().getExtras());
                }
                // 額外加上這一頁選的 FoodID
                intent.putExtra("selectedFoodID", selectedFoodID);
                startActivity(intent);
            }
        });
    }

    private void fetchFoodFromDB() {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 依照 [cite: 1] 文件，Food 表有 FoodID, Brand, Flavor
                String sql = "SELECT FoodID, Brand, Flavor FROM Food";
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        String displayName = "[" + rs.getString("Brand") + "] " + rs.getString("Flavor");
                        allFoodList.add(new FoodItem(rs.getInt("FoodID"), displayName));
                    }

                    runOnUiThread(() -> {
                        filteredList.clear();
                        filteredList.addAll(allFoodList);
                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void filterList(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(allFoodList);
        } else {
            for (FoodItem item : allFoodList) {
                if (item.name.toLowerCase().contains(text.toLowerCase())) {
                    filteredList.add(item);
                }
            }
        }
        // 如果有輸入文字，自動展開清單
        if (!text.isEmpty()) rvFoodList.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    // 資料模型
    class FoodItem {
        int id; String name;
        FoodItem(int id, String name) { this.id = id; this.name = name; }
    }

    // 適配器
    class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.ViewHolder> {
        private List<FoodItem> data;
        public FoodAdapter(List<FoodItem> data) { this.data = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_box, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FoodItem item = data.get(position);
            holder.tvName.setText(item.name);

            // 選中效果
            holder.itemView.setBackgroundResource(selectedPosition == position ?
                    R.drawable.bg_rounded_selected : R.drawable.bg_rounded_input);
            holder.itemView.setPadding(45, 45, 45, 45);

            holder.itemView.setOnClickListener(v -> {
                selectedPosition = holder.getAdapterPosition();
                selectedFoodID = item.id;
                selectedFoodName = item.name;
                tvDropdownHeader.setText(item.name);
                rvFoodList.setVisibility(View.GONE);
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvFoodDetail);
            }
        }
    }
}