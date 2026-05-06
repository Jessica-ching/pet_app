package com.example.pet_app.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.HomeActivity;
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etEmail = findViewById(R.id.etUsername); // 這裡對應 XML 的帳號輸入框
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "請輸入電子信箱與密碼", Toast.LENGTH_SHORT).show();
                return;
            }

            loginToDB(email, password);
        });

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginToDB(String email, String pass) {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {
                // 1. 驗證帳號密碼 (對應資料表欄位: Email, Password)
                String userSql = "SELECT UserID FROM Users WHERE Email = ? AND Password = ?";
                PreparedStatement pstmt = conn.prepareStatement(userSql);
                pstmt.setString(1, email);
                pstmt.setString(2, pass);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int userId = rs.getInt("UserID");
                    // 假設 AccountName 也是 Users 表的一個欄位
                    // String userName = rs.getString("AccountName"); 
                    // 如果沒有 AccountName，可以用 email 替代
                    String userName = email.split("@")[0]; 

                    // 2. 儲存 UserID 到手機快取，供後續 PetStatusActivity 使用
                    SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    pref.edit()
                            .putInt("UserID", userId)
                            .putBoolean("isLoggedIn", true)
                            .putString("UserName", userName)
                            .apply();

                    // 3. 檢查 Pets 資料表是否有該使用者的寵物資料
                    String petSql = "SELECT COUNT(*) FROM Pets WHERE UserID = ?";
                    PreparedStatement pstmtPet = conn.prepareStatement(petSql);
                    pstmtPet.setInt(1, userId);
                    ResultSet rsPet = pstmtPet.executeQuery();

                    boolean hasPet = false;
                    if (rsPet.next() && rsPet.getInt(1) > 0) {
                        hasPet = true;
                    }

                    final boolean finalHasPet = hasPet;
                    runOnUiThread(() -> {
                        Intent intent;
                        if (finalHasPet) {
                            // 已有寵物 -> 去首頁
                            intent = new Intent(this, HomeActivity.class);
                        } else {
                            // 沒寵物 -> 去註冊寵物頁面
                            intent = new Intent(this, CreatePetInfoActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "帳號或密碼錯誤", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "連線失敗: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}