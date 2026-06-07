package com.gitcode.mcsm_backend;

import com.gitcode.mcsm_backend.common.McsmPaths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Properties;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
public class McsnBackendApplication {

    public static void main(String[] args) throws Exception {
        McsmPaths.initDirectories();

        // ---- 自动启动 MySQL ----
        File mysqlBin = new File("data/mysql/bin");
        File mysqlData = new File("data/mysql/data");
        String os = System.getProperty("os.name", "").toLowerCase();
        File mysqld = new File(mysqlBin, os.contains("win") ? "mysqld.exe" : "mysqld");
        File dbPropsFile = new File(McsmPaths.DB_PROPERTIES);

        if (mysqld.exists() && dbPropsFile.exists()) {
            if (!isPortOpen("127.0.0.1", 3306)) {
                log.info("MySQL not running, starting...");
                String mysqlDir = new File("data/mysql").getAbsolutePath();
                new ProcessBuilder(
                        mysqld.getAbsolutePath(),
                        "--basedir=" + mysqlDir,
                        "--datadir=" + mysqlData.getAbsolutePath(),
                        "--port=3306"
                ).directory(mysqlBin).redirectErrorStream(true)
                 .redirectOutput(new File(mysqlData, "mysqld.log")).start();

                for (int i = 0; i < 30 && !isPortOpen("127.0.0.1", 3306); i++) {
                    Thread.sleep(2000);
                }
                if (!isPortOpen("127.0.0.1", 3306)) {
                    log.error("MySQL startup timeout (60s)");
                    System.exit(1);
                }
                log.info("MySQL is ready");
            }
        } else if (!dbPropsFile.exists() && mysqld.exists()) {
            log.warn("Database not configured. Please run install-mysql.bat first.");
        }

        // ---- 原有启动逻辑 ----
        File configFile = new File(McsmPaths.APPLICATION_PROPERTIES);
        File setupFlag = new File(McsmPaths.SETUP_FLAG);

        boolean setupMode = !configFile.exists() || !setupFlag.exists();

        if (!configFile.exists()) {
            File exampleFile = new File(McsmPaths.APPLICATION_PROPERTIES_EXAMPLE);
            if (exampleFile.exists()) {
                Files.copy(exampleFile.toPath(), configFile.toPath());
                log.info("首次启动，进入安装向导模式");
                fillPlaceholdersFromDbProps(configFile, dbPropsFile);
            } else {
                log.error("找不到 {} 模板", McsmPaths.APPLICATION_PROPERTIES_EXAMPLE);
                System.exit(1);
            }
        } else if (setupMode && configHasPlaceholders(configFile)) {
            if (dbPropsFile.exists()) {
                fillPlaceholdersFromDbProps(configFile, dbPropsFile);
                log.info("已从 db.properties 替换配置文件中的占位符");
            } else {
                log.warn("============================================");
                log.warn("配置文件包含占位符 (<YOUR_MYSQL_PASSWORD>)，");
                log.warn("但 {} 不存在。", McsmPaths.DB_PROPERTIES);
                log.warn("请先运行 install-mysql.bat 初始化数据库配置。");
                log.warn("============================================");
            }
        }

        System.setProperty("spring.config.additional-location",
                "optional:file:./data/config/");

        int port = findAvailablePort(8000);
        Properties props = new Properties();
        props.setProperty("server.port", String.valueOf(port));

        SpringApplication app = new SpringApplication(McsnBackendApplication.class);
        app.setDefaultProperties(props);

        if (setupMode) {
            app.setAdditionalProfiles("setup");
            log.info("安装向导模式已启用，访问 http://127.0.0.1:{} 进行配置", port);
        }

        app.run(args);
        log.info("MCSM 后端已启动，端口: {}", port);
    }

    private static void fillPlaceholdersFromDbProps(File configFile, File dbPropsFile) throws IOException {
        if (!dbPropsFile.exists()) return;
        Properties dbProps = new Properties();
        try (InputStream in = new FileInputStream(dbPropsFile)) {
            dbProps.load(in);
        }
        String host = dbProps.getProperty("db.host", "127.0.0.1");
        String port = dbProps.getProperty("db.port", "3306");
        String dbUser = dbProps.getProperty("db.username", "root");
        String dbPass = dbProps.getProperty("db.password", "");
        String dbName = dbProps.getProperty("db.name", "mcsm");

        String content = Files.readString(configFile.toPath());
        content = content.replace("localhost:3306/user", host + ":" + port + "/" + dbName);
        content = content.replace("<YOUR_MYSQL_PASSWORD>", dbPass);
        content = content.replace("spring.datasource.username=root", "spring.datasource.username=" + dbUser);
        Files.writeString(configFile.toPath(), content);
        log.info("已从 db.properties 读取数据库配置");
    }

    private static boolean configHasPlaceholders(File configFile) throws IOException {
        String content = Files.readString(configFile.toPath());
        return content.contains("<YOUR_MYSQL_PASSWORD>") || content.contains("<YOUR_JWT_SECRET>");
    }

    private static int findAvailablePort(int startPort) {
        for (int port = startPort; port < startPort + 100; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                return port;
            } catch (IOException ignored) {}
        }
        return startPort;
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket s = new Socket(host, port)) { return true; } catch (IOException e) { return false; }
    }
}
