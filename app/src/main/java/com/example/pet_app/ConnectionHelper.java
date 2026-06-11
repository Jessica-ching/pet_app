package com.example.pet_app;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectionHelper {
    public static Connection getConnection() throws Exception {
        // 使用 jTDS 驅動程式 (在 Android 上通常更穩定)
        Class.forName("net.sourceforge.jtds.jdbc.Driver");

        String server = "petlove.database.windows.net";
        String database = "PetDB";
        String user = "petapp_admin";
        String password = "groupH115";

//        // Azure SQL Database 連線資訊
//        String server = "pet-grouph.database.windows.net";
//        String database = "PetDB";
//        String user = "petapp_admin";
//        String password = "groupH115";

        // jTDS 連線字串
        String connUrl = "jdbc:jtds:sqlserver://" + server + ":1433/" + database + ";" +
                "user=" + user + ";" +
                "password=" + password + ";" +
                "ssl=request;" +
                "loginTimeout=30;";

        return DriverManager.getConnection(connUrl);
    }
}
