package com.example.pet_app.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.ChatAdapter;
import com.example.pet_app.ChatMessage;
import com.example.pet_app.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etInput;
    private ImageButton btnSend;
    private List<ChatMessage> messageList = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private OkHttpClient client = new OkHttpClient();

    private String petSpecies, petBirthday, petGender;
    private boolean isSterilized;

    // 🌟 1. 處理選擇照片和檔案的 Launcher
    private ActivityResultLauncher<Intent> photoLauncher;
    private ActivityResultLauncher<Intent> fileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            petSpecies = extras.getString("petSpecies", "狗");
            petBirthday = extras.getString("petBirthday", "");
            petGender = extras.getString("petGender", "公");
            isSterilized = extras.getBoolean("isSterilized", false);
        }

        rvChat = findViewById(R.id.rvChat);
        etInput = findViewById(R.id.inputSection).findViewById(R.id.etChatInput);
        btnSend = findViewById(R.id.btnSend);

        chatAdapter = new ChatAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        setupKeywordClick();

        // 🌟 綁定 + 號圖示
        ImageView btnAdd = findViewById(R.id.btn_add_file);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showUploadMenu());
        }

        // 🌟 綁定返回鍵 (關閉此頁，回到上一頁)
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 送出訊息按鈕
        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
            }
        });

        // 🌟 2. 初始化照片選擇器
        photoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        Toast.makeText(this, "成功選取照片！", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 🌟 3. 初始化檔案選擇器
        fileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        Toast.makeText(this, "成功選取檔案！", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void showUploadMenu() {
        String[] options = {"上傳照片", "上傳檔案"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("請選擇要傳送的內容");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // 🌟 4. 真的打開手機相簿
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                photoLauncher.launch(intent);
            } else if (which == 1) {
                // 🌟 5. 真的打開檔案總管
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                fileLauncher.launch(intent);
            }
        });
        builder.show();
    }

    private void sendMessage(String text) {
        messageList.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChat.scrollToPosition(messageList.size() - 1);
        etInput.setText("");
        fetchAiResponse(text);
    }

    private void fetchAiResponse(String userInputText) {
        try {
            String sterilizedStatus = isSterilized ? "已結紮" : "未結紮";
            String fullPrompt = String.format("寵物資訊：[%s, %s, %s, %s]。需求：%s",
                    petSpecies, petBirthday, petGender, sterilizedStatus, userInputText);

            String encodedText = java.net.URLEncoder.encode(fullPrompt, "UTF-8");
            String url = "http://172.20.10.6:8000/ai_recommend?userInput=" + encodedText;

            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    showAiReply("連線失敗: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String jsonData = response.body().string();
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonObject = new JSONObject(jsonData);
                            String aiAnalysis = jsonObject.optString("ai_analysis", "分析完成");

                            JSONArray foodsArray = jsonObject.optJSONArray("recommended_foods");
                            StringBuilder foodResults = new StringBuilder();

                            if (foodsArray != null && foodsArray.length() > 0) {
                                foodResults.append("\n\n🍴 為您推薦以下口味 (前10筆)：\n");
                                int count = Math.min(foodsArray.length(), 10);
                                for (int i = 0; i < count; i++) {
                                    JSONObject food = foodsArray.getJSONObject(i);
                                    String brand = food.optString("Brand", "品牌");
                                    String flavor = food.optString("Flavor", "精選口味");
                                    foodResults.append(i + 1).append(". [").append(brand).append("] ").append(flavor).append("\n");
                                }
                            } else {
                                foodResults.append("\n\n⚠️ 找不到符合條件的飼料。");
                            }

                            showAiReply(aiAnalysis + foodResults.toString());

                        } catch (Exception e) {
                            showAiReply("解析失敗：" + jsonData);
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAiReply(String text) {
        runOnUiThread(() -> {
            messageList.add(new ChatMessage(text, false));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            rvChat.scrollToPosition(messageList.size() - 1);
        });
    }

    private void setupKeywordClick() {
        int[] ids = {R.id.kw1, R.id.kw2, R.id.kw3, R.id.kw4};
        for (int id : ids) {
            TextView tv = findViewById(id);
            if (tv != null) {
                tv.setOnClickListener(v -> sendMessage(tv.getText().toString()));
            }
        }
    }
}