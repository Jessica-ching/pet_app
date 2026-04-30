package com.example.pet_app;

public class DBConfig {
    // 未來 MSSQL 的連線資訊
    public static final String CONNECTION_URL = "jdbc:jtds:sqlserver://你的伺服器IP:1433;databaseName=PetDB;user=petapp_admin;password=groupH115;";

    // 建立一個標準的查詢模板
    public static void fetchDataFromSQL() {
        // 這裡未來寫連線邏輯
    }
}
