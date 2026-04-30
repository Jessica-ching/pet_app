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

    private void setupOtherPickers() {
        if (etPetType != null) {
            etPetType.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String[] types = {"貓", "狗", "其他"};
                    new AlertDialog.Builder(CreatePetInfoActivity.this)
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
                    new AlertDialog.Builder(CreatePetInfoActivity.this)
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
                    new AlertDialog.Builder(CreatePetInfoActivity.this)
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
                    new AlertDialog.Builder(CreatePetInfoActivity.this)
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
        if (btnNextStep != null) {
            btnNextStep.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 1. 取得使用者輸入的內容
                    String name = etPetName.getText().toString().trim();
                    String type = etPetType.getText().toString().trim(); // 貓/狗
                    String birthday = etPetBirthday.getText().toString().trim();
                    String gender = etPetGender.getText().toString().trim();
                    String isSterilizedStr = etSpayed.getText().toString().trim();

                    // 2. 簡單檢查必填項目
                    if (name.isEmpty() || type.isEmpty()) {
                        Toast.makeText(CreatePetInfoActivity.this, "請填寫寵物姓名與類型", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 3. 準備傳送到下一頁 (FoodSelectionActivity)
                    Intent intent = new Intent(CreatePetInfoActivity.this, FoodSelectionActivity.class);
                    intent.putExtra("petName", name);
                    intent.putExtra("petSpecies", type); // 對應資料庫 Species
                    intent.putExtra("petBirthday", birthday); // 對應資料庫 Birthday
                    intent.putExtra("petGender", gender); // 對應資料庫 Gender
                    intent.putExtra("isSterilized", isSterilizedStr.equals("是")); // 轉為 boolean

                    startActivity(intent);
                }
            });
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePicker = new DatePickerDialog(CreatePetInfoActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        String dateString = selectedYear + "/" + (selectedMonth + 1) + "/" + selectedDay;
                        etPetBirthday.setText(dateString);
                    }
                }, year, month, day);
        datePicker.show();
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
        new AlertDialog.Builder(CreatePetInfoActivity.this)
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
}