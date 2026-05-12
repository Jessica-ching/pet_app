package com.example.pet_app;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectionHelper {
    public static Connection getConnection() throws Exception {
        Class.forName("net.sourceforge.jtds.jdbc.Driver");

        // Azure 專用的連線字串格式
        // 1. 伺服器位址後加上 :1433
        // 2. ssl=request 確保加密通訊
        String connUrl = "jdbc:jtds:sqlserver://petlove.database.windows.net:1433;" +
                "databaseName=PetDB;" +
                "user=petapp_admin@petlove;" + // Azure 有時需要 @伺服器名
                "password=groupH115;" +
                "ssl=request;"; // Azure 雲端通常強制要求加密

        return DriverManager.getConnection(connUrl);
    }
}