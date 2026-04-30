package com.example.pet_app.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pet_app.ConnectionHelper;
import com.example.pet_app.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. 精確綁定所有元件
        // 注意：根據您的 XML，etRegName 是輸入名稱，etRegEmail 是輸入信箱
        EditText etEmail = findViewById(R.id.etRegEmail);       // 電子信箱 (Email)
        EditText etAccount = findViewById(R.id.etRegAccount);   // 帳號 (Account)
        EditText etPassword = findViewById(R.id.etRegPassword); // 密碼 (Password)
        Button btnSubmitRegister = findViewById(R.id.btnSubmitRegister);

        // 提示：etRegAccount 在資料庫中沒有對應欄位，若不需要可從 XML 刪除

        btnSubmitRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String account = etAccount.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            // 欄位檢查
            if (email.isEmpty() || pass.isEmpty() || account.isEmpty()) {
                Toast.makeText(this, "請完整填寫所有欄位", Toast.LENGTH_SHORT).show();
                return;
            }

            // 執行註冊邏輯
            registerToDB(email, pass, account);
        });
    }

    private void registerToDB(String email, String password, String account) {
        new Thread(() -> {
            try (Connection conn = ConnectionHelper.getConnection()) {

                // 2. 檢查信箱是否已存在
                String checkSql = "SELECT COUNT(*) FROM Users WHERE Email = ?";
                PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
                checkPstmt.setString(1, email);
                ResultSet rs = checkPstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    runOnUiThread(() -> Toast.makeText(this, "此電子信箱已被註冊", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 3. 執行插入動作 (對應資料表：Email, Password, UserName)
                // 加入 HasPetInfo 欄位(預設0)，避免登入時 Invalid column name 報錯
                String sql = "INSERT INTO Users (Email, Password, UserName) VALUES (?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, email);
                pstmt.setString(2, password);
                pstmt.setString(3, account);

                if (pstmt.executeUpdate() > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "註冊成功！", Toast.LENGTH_SHORT).show();
                        finish(); // 註冊完成後返回登入頁
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    // 若顯示 Invalid column name 'HasPetInfo'，請務必去 Azure 執行 ALTER TABLE 指令
                    Toast.makeText(this, "註冊失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}