package com.example.pet_app.login;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.ChatAdapter;
import com.example.pet_app.ChatMessage;
import com.example.pet_app.R;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etInput;
    private ImageButton btnSend;
    private List<ChatMessage> messageList = new ArrayList<>();
    private ChatAdapter chatAdapter; // 你需要建立這個 Adapter (後述)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. 初始化 UI 元件
        rvChat = findViewById(R.id.rvChat);
        etInput = findViewById(R.id.inputSection).findViewById(R.id.etChatInput); // 請確保 EditText 在 XML 有 android:id="@+id/etChatInput"
        btnSend = findViewById(R.id.btnSend);
        ImageView btnBack = findViewById(R.id.btnBack);

        // 2. 設定 RecyclerView
        chatAdapter = new ChatAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        // 3. 返回按鈕
        btnBack.setOnClickListener(v -> finish());

        // 4. 設定「快速選擇關鍵字」點擊事件
        setupKeywordClick();

        // 5. 發送按鈕邏輯
        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
            }
        });
    }

    private void sendMessage(String text) {
        // A. 顯示使用者訊息
        messageList.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChat.scrollToPosition(messageList.size() - 1);
        etInput.setText("");

        // B. 呼叫同伴寫的 AI API (模擬)
        fetchAiResponse(text);
    }

    private void setupKeywordClick() {
        // 找到 GridLayout 裡所有的 TextView (關鍵字)
        View keywordSection = findViewById(R.id.keywordSection);
        // 這裡可以手動綁定那四個關鍵字 TextView
        // 點擊後直接執行 sendMessage("文字內容")
    }

    private void fetchAiResponse(String userInput) {
        // 🌟 這裡就是接同伴 API 的地方
        // 建議暫時先用一個模擬回覆測試介面
        new android.os.Handler().postDelayed(() -> {
            String mockReply = "關於「" + userInput + "」，AI 正在為您查詢適合的飼料...";
            messageList.add(new ChatMessage(mockReply, false));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            rvChat.scrollToPosition(messageList.size() - 1);
        }, 1000);
    }
}