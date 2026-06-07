package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.common.McsmPaths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

@Slf4j
@Service
public class SetupService {

    public boolean isSetupComplete() {
        return new File(McsmPaths.SETUP_FLAG).exists()
                && new File(McsmPaths.APPLICATION_PROPERTIES).exists();
    }

    public boolean isDbConfigured() {
        return new File(McsmPaths.DB_PROPERTIES).exists();
    }

    public Properties loadDbProperties() {
        Properties props = new Properties();
        File file = new File(McsmPaths.DB_PROPERTIES);
        if (!file.exists()) return null;
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
            return props;
        } catch (IOException e) {
            log.error("读取 db.properties 失败: {}", e.getMessage());
            return null;
        }
    }

    public Map<String, Object> createAdmin(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        Properties dbProps = loadDbProperties();
        if (dbProps == null) {
            result.put("success", false);
            result.put("message", "数据库未配置，请先运行 install-mysql.bat");
            return result;
        }

        String host = dbProps.getProperty("db.host", "127.0.0.1");
        String port = dbProps.getProperty("db.port", "3306");
        String dbUser = dbProps.getProperty("db.username", "root");
        String dbPass = dbProps.getProperty("db.password", "");
        String dbName = dbProps.getProperty("db.name", "mcsm");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String encodedPassword = encoder.encode(password);

            try (PreparedStatement check = conn.prepareStatement("SELECT id FROM `user` WHERE username = ?")) {
                check.setString(1, username);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE `user` SET password = ? WHERE username = ?")) {
                        update.setString(1, encodedPassword);
                        update.setString(2, username);
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO `user` (username, password, baned, money) VALUES (?, ?, 0, 0)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        insert.setString(1, username);
                        insert.setString(2, encodedPassword);
                        insert.executeUpdate();

                        ResultSet keys = insert.getGeneratedKeys();
                        if (keys.next()) {
                            long userId = keys.getLong(1);
                            try (PreparedStatement role = conn.prepareStatement(
                                    "INSERT INTO `user_role` (user_id, role_id) VALUES (?, 1)")) {
                                role.setLong(1, userId);
                                role.executeUpdate();
                            }
                        }
                    }
                }
            }

            result.put("success", true);
            result.put("message", "管理员账户创建成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建失败: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> writeConfig() {
        Map<String, Object> result = new HashMap<>();

        Properties dbProps = loadDbProperties();
        if (dbProps == null) {
            result.put("success", false);
            result.put("message", "数据库未配置，请先运行 install-mysql.bat");
            return result;
        }

        try {
            String host = dbProps.getProperty("db.host", "127.0.0.1");
            String port = dbProps.getProperty("db.port", "3306");
            String dbUser = dbProps.getProperty("db.username", "root");
            String dbPass = dbProps.getProperty("db.password", "");
            String dbName = dbProps.getProperty("db.name", "mcsm");

            String jwtSecret = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
            String pluginSecret = UUID.randomUUID().toString().replace("-", "");

            Properties props = new Properties();
            props.setProperty("spring.application.name", "mcsm-backend");
            props.setProperty("spring.datasource.url", "jdbc:mysql://" + host + ":" + port + "/" + dbName
                    + "?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
            props.setProperty("spring.datasource.username", dbUser);
            props.setProperty("spring.datasource.password", dbPass);
            props.setProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
            props.setProperty("spring.servlet.multipart.max-file-size", "50MB");
            props.setProperty("spring.servlet.multipart.max-request-size", "50MB");
            props.setProperty("security.permit-all-paths",
                    "/,/index.html,/registerSW.js,/sw.js,/manifest.webmanifest,/favicon.ico,/api/login,/api/register,/api/setup/**,/error,/assets/**,/setup.html,/settings.html,/api/gameServer/**,/api/agent/**,/api/admin/settings/**,/ws/**,/api/frp/**");
            props.setProperty("jwt.secret", jwtSecret);
            props.setProperty("jwt.expire", "43200000");
            props.setProperty("http.connect-timeout", "3000");
            props.setProperty("http.read-timeout", "3000");
            props.setProperty("spring.main.allow-circular-references", "true");
            props.setProperty("plugin.secret", pluginSecret);

            File configFile = McsmPaths.writeFile(McsmPaths.APPLICATION_PROPERTIES);
            try (OutputStream out = new FileOutputStream(configFile)) {
                props.store(out, "MCSM Configuration - Generated by Setup Wizard");
            }

            Files.writeString(McsmPaths.writeFile(McsmPaths.SETUP_FLAG).toPath(), "installed", StandardCharsets.UTF_8);

            result.put("success", true);
            result.put("message", "配置已保存，请重启后端服务");
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "写入配置失败: " + e.getMessage());
        }
        return result;
    }
}
