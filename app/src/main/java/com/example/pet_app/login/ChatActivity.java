package com.example.pet_app.login;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

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

    // 🌟 儲存從前幾頁傳來的寵物資訊
    private String petSpecies, petBirthday, petGender;
    private boolean isSterilized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. 接收寵物背景資料 (從 Intent 取得)
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            petSpecies = extras.getString("petSpecies", "狗");
            petBirthday = extras.getString("petBirthday", "");
            petGender = extras.getString("petGender", "公");
            isSterilized = extras.getBoolean("isSterilized", false);
        }

        // UI 初始化 (省略部分重複代碼...)
        rvChat = findViewById(R.id.rvChat);
        etInput = findViewById(R.id.inputSection).findViewById(R.id.etChatInput);
        btnSend = findViewById(R.id.btnSend);

        chatAdapter = new ChatAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        setupKeywordClick();

        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
            }
        });
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
            // 🌟 自動帶入寵物背景，讓 AI 精準判斷
            String sterilizedStatus = isSterilized ? "已結紮" : "未結紮";
            String fullPrompt = String.format("寵物資訊：[%s, %s, %s, %s]。需求：%s",
                    petSpecies, petBirthday, petGender, sterilizedStatus, userInputText);

            String encodedText = java.net.URLEncoder.encode(fullPrompt, "UTF-8");
            String url = "http://172.20.10.4:8000/ai_recommend?userInput=" + encodedText;

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

                            // 🌟 處理推薦清單：改用 flavor，並限制前 10 筆
                            JSONArray foodsArray = jsonObject.optJSONArray("recommended_foods");
                            StringBuilder foodResults = new StringBuilder();

                            if (foodsArray != null && foodsArray.length() > 0) {
                                foodResults.append("\n\n🍴 為您推薦以下口味 (前10筆)：\n");
                                int count = Math.min(foodsArray.length(), 10); // 限制 10 筆
                                for (int i = 0; i < count; i++) {
                                    JSONObject food = foodsArray.getJSONObject(i);
                                    String brand = food.optString("Brand", "品牌");
                                    String flavor = food.optString("Flavor", "精選口味"); // 🌟 改為 flavor
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