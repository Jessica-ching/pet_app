package com.example.pet_app.mainfeature;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pet_app.ChatAdapter;
import com.example.pet_app.ChatMessage;
import com.example.pet_app.HomeActivity;
import com.example.pet_app.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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

    private Uri selectedUri = null;

    private ActivityResultLauncher<Intent> photoLauncher;
    private ActivityResultLauncher<Intent> fileLauncher;

    // 🌟 核心修改：換成 Android 模擬器專用的本機端 IP
    private static final String BACKEND_BASE_URL = "http://172.20.10.6:8000";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        photoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedUri = result.getData().getData();
                        Toast.makeText(requireContext(), "已選擇圖片，輸入問題後按送出", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        fileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedUri = result.getData().getData();
                        Toast.makeText(requireContext(), "已選擇檔案，輸入問題後按送出", Toast.LENGTH_SHORT).show();
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

        // 🌟 綁定你的「+」號圖示按鈕
        ImageView btnAdd = view.findViewById(R.id.btn_add_file);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showUploadMenu());
        }

        ImageView btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Activity activity = getActivity();
                if (activity == null || activity.isFinishing()) return;

                if (activity instanceof HomeActivity) {
                    BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_home);
                    }
                } else {
                    activity.onBackPressed();
                }
            });
        }

        btnSend.setOnClickListener(v -> {
            String question = etInput.getText().toString().trim();

            if (question.isEmpty()) {
                Toast.makeText(requireContext(), "請輸入問題", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedUri == null) {
                addMessage(new ChatMessage(question, true));
                askAiAssistant(question);
            } else {
                Toast.makeText(requireContext(), "準備送出附檔問題", Toast.LENGTH_SHORT).show();
                addMessage(new ChatMessage(question + "\n[已附上檔案]", true));
                askAiAssistantWithImage(question, selectedUri);
                selectedUri = null;
            }
            etInput.setText("");
        });

        return view;
    }

    private void showUploadMenu() {
        String[] options = {"上傳照片", "上傳檔案"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("請選擇要傳送的內容");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                photoLauncher.launch(intent);
            } else if (which == 1) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                fileLauncher.launch(intent);
            }
        });
        builder.show();
    }

    private void addMessage(ChatMessage message) {
        Activity activity = getActivity();
        if (activity == null || !isAdded() || activity.isFinishing()) return;

        activity.runOnUiThread(() -> {
            if (isAdded() && getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {
                messageList.add(message);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvChat.scrollToPosition(messageList.size() - 1);
            }
        });
    }

    // 純文字版 API
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
                InputStream responseStream = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
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
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // 圖片/檔案版 API
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
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                OutputStream outputStream = conn.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"question\"").append("\r\n\r\n");
                writer.append(question).append("\r\n");
                writer.flush();

                String mimeType = null;
                Context context = getContext();
                if (isAdded() && context != null) {
                    mimeType = context.getContentResolver().getType(imageUri);
                }
                if (mimeType == null || mimeType.isEmpty()) mimeType = "image/jpeg";

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"").append("\r\n");
                writer.append("Content-Type: ").append(mimeType).append("\r\n\r\n");
                writer.flush();

                InputStream imageInputStream = null;
                if (isAdded() && getContext() != null) {
                    imageInputStream = getContext().getContentResolver().openInputStream(imageUri);
                }
                if (imageInputStream == null) throw new Exception("圖片讀取失敗或視窗已關閉");

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = imageInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                imageInputStream.close();

                writer.append("\r\n").append("--").append(boundary).append("--").append("\r\n");
                writer.flush();
                writer.close();

                int responseCode = conn.getResponseCode();
                InputStream responseStream = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
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
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private String readStream(InputStream inputStream) {
        if (inputStream == null) return "";
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String result = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return result;
    }
}