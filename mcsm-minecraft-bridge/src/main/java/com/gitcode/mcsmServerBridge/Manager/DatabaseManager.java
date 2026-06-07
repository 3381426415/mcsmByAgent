package com.gitcode.mcsmServerBridge.Manager;

import com.gitcode.mcsmServerBridge.McsmBridge;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private final McsmBridge plugin;
    String ip;
    String port;
    String database;
    String user;
    String pass;
    String url;

    public DatabaseManager(McsmBridge plugin) {
        this.plugin = plugin;
        reloadconfig(plugin);
    }

    public void reloadconfig(McsmBridge plugin){
        ip = plugin.getConfig().getString("database.ip");
        port = plugin.getConfig().getString("database.port");
        database = plugin.getConfig().getString("database.database");
        user = plugin.getConfig().getString("database.user");
        pass = plugin.getConfig().getString("database.pass");
        url = "jdbc:mysql://" + ip + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true";
    }

    /**
     * 获取新的数据库连接（每次创建新连接，保证线程安全）
     * 调用方必须自行关闭连接（try-with-resources）
     */
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        }
        return DriverManager.getConnection(url, user, pass);
    }

    public void close() {
        // 不再维护单连接，无需清理
    }
}