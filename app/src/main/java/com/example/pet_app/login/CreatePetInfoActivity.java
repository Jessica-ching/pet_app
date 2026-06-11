package com.example.pet_app.login;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pet_app.R;

import java.util.Calendar;

public class CreatePetInfoActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> galleryLauncher;
    private ImageView ivPetPhoto;
    private View btnNextStep;

    // 🌟 破案關鍵：幫元件「正名分家」！
    private EditText etPetName, etPetBirthday;
    private TextView etPetType, etPetGender, etSpayed;

    private boolean isEditMode = false;
    private int petId = -1;

    // 避免 WindowLeaked，追蹤目前開啟的 Dialog
    private AlertDialog currentDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pet_info);

        etPetName = findViewById(R.id.etPetName);
        etPetType = findViewById(R.id.etPetType);
        etPetBirthday = findViewById(R.id.etPetBirthday);
        etPetGender = findViewById(R.id.etPetGender);
        etSpayed = findViewById(R.id.etSpayed);
        btnNextStep = findViewById(R.id.btnNextStep);
        ivPetPhoto = findViewById(R.id.ivPetPhoto);

        // 🌟 接收編輯模式參數
        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
        petId = getIntent().getIntExtra("PET_ID", -1);

        if (isEditMode) {
            // 如果是編輯模式，把按鈕文字改掉，並抓資料
            ((TextView)btnNextStep).setText("確認修改");
            fetchExistingPetData();
        }

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            ivPetPhoto.setImageURI(uri);
                            ivPetPhoto.setPadding(0, 0, 0, 0);
                        }
                    }
                }
        );

        ivPetPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryLauncher.launch("image/*");
            }
        });

        setupOtherPickers();
    }

    @Override
    protected void onDestroy() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
        super.onDestroy();
    }

    private void setupOtherPickers() {
        if (etPetType != null) {
            etPetType.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String[] types = {"貓", "狗", "其他"};
                    currentDialog = new AlertDialog.Builder(CreatePetInfoActivity.this)
                            .setTitle("選擇寵物類型")
                            .setItems(types, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    etPetType.setText(types[which]);
                                }
                            }).show();
                }
            });
        }

        if (etPetBirthday != null) {
            etPetBirthday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String[] birthdayOptions = {"📅 選擇確切日期", "⏳ 輸入年齡推算 (幾歲幾個月)"};
                    currentDialog = new AlertDialog.Builder(CreatePetInfoActivity.this)
                            .setTitle("請問您知道確切生日嗎？")
                            .setItems(birthdayOptions, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0) showDatePicker();
                                    else showAgeEstimator();
                                }
                            }).show();
                }
            });
        }

        if (etPetGender != null) {
            etPetGender.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String[] genders = {"公", "母"};
                    currentDialog = new AlertDialog.Builder(CreatePetInfoActivity.this)
                            .setTitle("選擇寵物性別")
                            .setItems(genders, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    etPetGender.setText(genders[which]);
                                }
                            }).show();
                }
            });
        }

        if (etSpayed != null) {
            etSpayed.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String[] options = {"是", "否"};
                    currentDialog = new AlertDialog.Builder(CreatePetInfoActivity.this)
                            .setTitle("是否已結紮？")
                            .setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    etSpayed.setText(options[which]);
                                }
                            }).show();
                }
            });
        }

        // 通往飼料選用的任意門
        // 在 setupOtherPickers 的 btnNextStep 監聽器內
        if (btnNextStep != null) {
            btnNextStep.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = etPetName.getText().toString().trim();
                    String type = etPetType.getText().toString().trim();
                    String birthday = etPetBirthday.getText().toString().trim();
                    String gender = etPetGender.getText().toString().trim();
                    String isSterilizedStr = etSpayed.getText().toString().trim();

                    if (name.isEmpty() || type.isEmpty()) {
                        Toast.makeText(CreatePetInfoActivity.this, "請填寫完整資訊", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isEditMode) {
                        // 🌟 編輯模式：執行 UPDATE SQL
                        updatePetToDB(name, type, birthday, gender, isSterilizedStr.equals("是"));
                    } else {
                        // 🌟 新增模式：維持原本邏輯，跳轉到下一頁
                        Intent intent = new Intent(CreatePetInfoActivity.this, FoodSelectionActivity.class);
                        intent.putExtra("petName", name);
                        intent.putExtra("petSpecies", type);
                        intent.putExtra("petBirthday", birthday);
                        intent.putExtra("petGender", gender);
                        intent.putExtra("isSterilized", isSterilizedStr.equals("是"));
                        startActivity(intent);
                    }
                }
            });
        }
    }

    private void fetchExistingPetData() {
        new Thread(() -> {
            try (java.sql.Connection conn = com.example.pet_app.ConnectionHelper.getConnection()) {
                String sql = "SELECT PetName, Species, Birthday, Gender, IsSterilized FROM Pets WHERE PetID = ?";
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, petId);
                    try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            String name = rs.getString("PetName");
                            String type = rs.getString("Species");
                            String birthday = rs.getString("Birthday");
                            String gender = rs.getString("Gender");
                            boolean spayed = rs.getBoolean("IsSterilized");

                            runOnUiThread(() -> {
                                etPetName.setText(name);
                                etPetType.setText(type);
                                etPetBirthday.setText(birthday.replace("-", "/"));
                                etPetGender.setText(gender);
                                etSpayed.setText(spayed ? "是" : "否");
                            });
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        currentDialog = new DatePickerDialog(CreatePetInfoActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        String dateString = selectedYear + "/" + (selectedMonth + 1) + "/" + selectedDay;
                        etPetBirthday.setText(dateString);
                    }
                }, year, month, day);
        currentDialog.show();
    }

    private void showAgeEstimator() {
        LinearLayout layout = new LinearLayout(CreatePetInfoActivity.this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);
        final NumberPicker yearPicker = new NumberPicker(CreatePetInfoActivity.this);
        yearPicker.setMinValue(0); yearPicker.setMaxValue(30); yearPicker.setValue(1);
        TextView yearText = new TextView(CreatePetInfoActivity.this);
        yearText.setText(" 歲   "); yearText.setTextSize(18);
        final NumberPicker monthPicker = new NumberPicker(CreatePetInfoActivity.this);
        monthPicker.setMinValue(0); monthPicker.setMaxValue(11); monthPicker.setValue(0);
        TextView monthText = new TextView(CreatePetInfoActivity.this);
        monthText.setText(" 個月"); monthText.setTextSize(18);
        layout.addView(yearPicker); layout.addView(yearText); layout.addView(monthPicker); layout.addView(monthText);
        currentDialog = new AlertDialog.Builder(CreatePetInfoActivity.this)
                .setTitle("請輸入寵物大約年齡")
                .setView(layout)
                .setPositiveButton("自動推算", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.YEAR, -yearPicker.getValue());
                        cal.add(Calendar.MONTH, -monthPicker.getValue());
                        String estimatedDate = cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH);
                        etPetBirthday.setText(estimatedDate);
                    }
                })
                .setNegativeButton("取消", null).show();
    }

    private void updatePetToDB(String name, String type, String birthday, String gender, boolean isSterilized) {
        new Thread(() -> {
            try (java.sql.Connection conn = com.example.pet_app.ConnectionHelper.getConnection()) {
                String sql = "UPDATE Pets SET PetName=?, Species=?, Birthday=?, Gender=?, IsSterilized=? WHERE PetID=?";
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, name);
                    pstmt.setString(2, type);
                    pstmt.setString(3, birthday.replace("/", "-")); // SQL 通常用 -
                    pstmt.setString(4, gender);
                    pstmt.setBoolean(5, isSterilized);
                    pstmt.setInt(6, petId);

                    int result = pstmt.executeUpdate();
                    runOnUiThread(() -> {
                        if (result > 0) {
                            Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
                            finish(); // 回到 PetInfoActivity
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "修改失敗", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}