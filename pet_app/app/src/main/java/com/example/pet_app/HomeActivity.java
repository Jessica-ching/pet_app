package com.example.pet_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.pet_app.login.LoginActivity;
import com.example.pet_app.mainfeature.AiHelpFragment;
import com.example.pet_app.mainfeature.calender.CalendarFragment;
import com.example.pet_app.mainfeature.pet.PetFragment;
import com.example.pet_app.mainfeature.record.RecordFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    public boolean isLoggedIn = false;
    public String currentPetName = "選擇寵物";
    public String currentUserName = ""; // 🌟 真實用戶名
    private DrawerLayout drawerLayout;
    private TextView txtUserStatus;
    private ImageView imgUserAvatar;
    private Button btnLoginLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // 系統狀態欄適配
        View mainView = findViewById(R.id.drawer_layout);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Side Menu 側邊欄設定
        drawerLayout = findViewById(R.id.drawer_layout);
        ImageButton btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // 綁定側邊欄 UI 元件 (修正從 NavigationView 獲取 Header View 的方式)
        NavigationView navView = findViewById(R.id.nav_view);
        if (navView != null && navView.getHeaderCount() > 0) {
            View headerView = navView.getHeaderView(0);
            txtUserStatus = headerView.findViewById(R.id.txt_user_status);
            imgUserAvatar = headerView.findViewById(R.id.img_user_avatar);
            btnLoginLogout = headerView.findViewById(R.id.btn_login_logout);
        }

        // 登入/登出按鈕邏輯
        if (btnLoginLogout != null) {
            btnLoginLogout.setOnClickListener(v -> {
                if (isLoggedIn) {
                    performLogout();
                } else {
                    startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                }
            });
        }

        // 底部導覽列
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setItemIconTintList(null);
            Menu menu = bottomNav.getMenu();
            setEmojiIcon(menu, R.id.nav_pet, "\uD83D\uDC3E");
            setEmojiIcon(menu, R.id.nav_record, "\uD83D\uDCDD");
            setEmojiIcon(menu, R.id.nav_home, "\uD83C\uDFE0");
            setEmojiIcon(menu, R.id.nav_calendar, "\uD83D\uDCC5");
            setEmojiIcon(menu, R.id.nav_settings, "\uD83D\uDC7D");

            bottomNav.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                int id = item.getItemId();
                if (id == R.id.nav_pet) selectedFragment = new PetFragment();
                else if (id == R.id.nav_record) selectedFragment = new RecordFragment();
                else if (id == R.id.nav_home) selectedFragment = new HomeFragment();
                else if (id == R.id.nav_calendar) selectedFragment = new CalendarFragment();
                else if (id == R.id.nav_settings) selectedFragment = new AiHelpFragment();

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment).commit();
                }
                return true;
            });

            // 預設載入 HomeFragment
            if (savedInstanceState == null) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    // 🌟 檢查真實登入狀態並獲取使用者名稱
    public void checkLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        currentUserName = prefs.getString("UserName", "用戶"); // 讀取登入時存下的名字
    }

    // 🌟 更新側邊欄 UI（移除假資料）
    public void updateDrawerUI() {
        if (txtUserStatus == null || imgUserAvatar == null || btnLoginLogout == null) return;

        if (isLoggedIn) {
            txtUserStatus.setText(currentUserName + " 歡迎您！");
            imgUserAvatar.setImageResource(R.drawable.cat_placeholder); // 可依需求根據寵物品種更換
            btnLoginLogout.setText("登出");
            btnLoginLogout.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E6B34D")));
        } else {
            txtUserStatus.setText("請先登入");
            imgUserAvatar.setImageResource(R.drawable.ic_user_placeholder);
            btnLoginLogout.setText("登入");
            btnLoginLogout.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
        }
    }

    private void performLogout() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply(); // 登出時清空所有資訊
        isLoggedIn = false;
        updateDrawerUI();
        if (drawerLayout != null) drawerLayout.closeDrawers();
        Toast.makeText(this, "已登出", Toast.LENGTH_SHORT).show();

        // 登出後重刷 HomeFragment 顯示未登入狀態
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLoginStatus(); // 從 SharedPreferences 讀取最新狀態
        updateDrawerUI();   // 更新側邊欄 (NavigationView)

        // 通知當前的 Fragment 更新畫面
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).checkStatusAndRefreshUI();
        } else if (currentFragment instanceof PetFragment) {
            // 確保你的 PetFragment 有這個 public 方法來重新抓取資料與更新 UI
            ((PetFragment) currentFragment).loadPetData();
        }
    }

    private void setEmojiIcon(Menu menu, int itemId, String emoji) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) item.setIcon(new EmojiDrawable(emoji));
    }
}

class EmojiDrawable extends Drawable {

    private final String text;
    private final Paint paint;

    public EmojiDrawable(String text) {
        this.text = text;
        this.paint = new Paint();
        this.paint.setTextSize(60f);
        this.paint.setAntiAlias(true);
        this.paint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        int width = getBounds().width();
        int height = getBounds().height();
        canvas.drawText(text, width / 2f, height / 1.5f, paint);
    }

    @Override
    public void setAlpha(int alpha) { paint.setAlpha(alpha); }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }

    @Override
    public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
