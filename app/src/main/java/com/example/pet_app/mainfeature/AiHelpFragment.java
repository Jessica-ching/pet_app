package com.example.pet_app.mainfeature;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.ChatAdapter;
import com.example.pet_app.ChatMessage;
import com.example.pet_app.R;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AiHelpFragment extends Fragment {

    private EditText etInput;
    private ImageButton btnSend;
    private RecyclerView rvChat;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> pickImageLauncher;

    // 後端筆電 IP，如果後端 IP 改變，只改這裡
    private static final String BACKEND_BASE_URL = "http://172.20.10.4:8000";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        Toast.makeText(requireContext(), "已選擇圖片，輸入問題後按送出", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ai_help, container, false);

        etInput = view.findViewById(R.id.et_ai_input);
        btnSend = view.findViewById(R.id.btn_send_ai);
        rvChat = view.findViewById(R.id.rv_chat);

        chatAdapter = new ChatAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChat.setAdapter(chatAdapter);

        // 長按送出按鈕：選圖片
        btnSend.setOnLongClickListener(v -> {
            pickImageLauncher.launch("image/*");
            return true;
        });

        // 點擊送出按鈕：送文字或文字+圖片
        btnSend.setOnClickListener(v -> {
            String question = etInput.getText().toString().trim();

            if (question.isEmpty()) {
                Toast.makeText(requireContext(), "請輸入問題", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedImageUri == null) {
                Toast.makeText(requireContext(), "請先長按送出按鈕選擇圖片", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(requireContext(), "準備送出圖片問題", Toast.LENGTH_SHORT).show();

            addMessage(new ChatMessage(question + "\n[已附上圖片]", true));

            askAiAssistantWithImage(question, selectedImageUri);

            selectedImageUri = null;
            etInput.setText("");
        });

        return view;
    }

    private void addMessage(ChatMessage message) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            messageList.add(message);
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            rvChat.scrollToPosition(messageList.size() - 1);
        });
    }

    // 純文字版：POST /api/assistant
    private void askAiAssistant(String question) {
        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                URL url = new URL(BACKEND_BASE_URL + "/api/assistant");

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("question", question);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonInput.toString().getBytes("UTF-8"));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();

                InputStream responseStream;

                if (responseCode >= 200 && responseCode < 300) {
                    responseStream = conn.getInputStream();
                } else {
                    responseStream = conn.getErrorStream();
                }

                String response = readStream(responseStream);

                if (responseCode >= 200 && responseCode < 300) {
                    addMessage(new ChatMessage(response.trim(), false));
                } else {
                    addMessage(new ChatMessage("伺服器回應錯誤：" + responseCode + "\n" + response, false));
                }

            } catch (Exception e) {
                e.printStackTrace();
                addMessage(new ChatMessage("連線失敗：" + e.getMessage(), false));

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    // 圖片版：POST /api/assistant/image
    private void askAiAssistantWithImage(String question, Uri imageUri) {
        new Thread(() -> {
            HttpURLConnection conn = null;

            try {
                String boundary = "----PetAppBoundary" + System.currentTimeMillis();

                URL url = new URL(BACKEND_BASE_URL + "/api/assistant/image");

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");

                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);

                conn.setDoOutput(true);
                conn.setDoInput(true);

                conn.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data;boundary=" + boundary
                );

                OutputStream outputStream = conn.getOutputStream();

                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(outputStream, "UTF-8"),
                        true
                );

                // 1. 傳 question
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"question\"").append("\r\n");
                writer.append("\r\n");
                writer.append(question).append("\r\n");
                writer.flush();

                // 2. 傳 image
                String mimeType = requireContext().getContentResolver().getType(imageUri);

                if (mimeType == null || mimeType.isEmpty()) {
                    mimeType = "image/jpeg";
                }

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"")
                        .append("\r\n");
                writer.append("Content-Type: ").append(mimeType).append("\r\n");
                writer.append("\r\n");
                writer.flush();

                InputStream imageInputStream = requireContext()
                        .getContentResolver()
                        .openInputStream(imageUri);

                if (imageInputStream == null) {
                    throw new Exception("圖片讀取失敗");
                }

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = imageInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.flush();
                imageInputStream.close();

                writer.append("\r\n");
                writer.append("--").append(boundary).append("--").append("\r\n");
                writer.flush();
                writer.close();

                // 3. 讀取後端回應
                int responseCode = conn.getResponseCode();

                InputStream responseStream;

                if (responseCode >= 200 && responseCode < 300) {
                    responseStream = conn.getInputStream();
                } else {
                    responseStream = conn.getErrorStream();
                }

                String response = readStream(responseStream);

                if (responseCode >= 200 && responseCode < 300) {
                    addMessage(new ChatMessage(response.trim(), false));
                } else {
                    addMessage(new ChatMessage("後端錯誤：" + responseCode + "\n" + response, false));
                }

            } catch (Exception e) {
                e.printStackTrace();
                addMessage(new ChatMessage("圖片連線失敗：" + e.getMessage(), false));

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    private String readStream(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }

        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String result = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        return result;
    }
}