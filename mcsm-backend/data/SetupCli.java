import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.zip.*;

public class SetupCli {

    private static final String DB_NAME = "mcsm";

    public static void main(String[] args) throws Exception {
        String baseDir = System.getProperty("user.dir");
        File mysqlDir = new File(baseDir, "data/mysql");
        File mysqlBin = new File(mysqlDir, "bin");
        File mysqlData = new File(mysqlDir, "data");
        File configDir = new File(baseDir, "data/config");
        File dbPropsFile = new File(configDir, "db.properties");
        File initSqlFile = new File(baseDir, "data/init.sql");

        System.out.println();
        System.out.println("  ========================================");
        System.out.println("         MCSM Database Setup");
        System.out.println("  ========================================");
        System.out.println();

        try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (Exception e) {
            System.out.println("[ERROR] MySQL JDBC driver not found");
            System.exit(1);
        }

        boolean installed = new File(mysqlBin, "mysqld.exe").exists();

        if (!installed) {
            // ========== 未安装：解压 + 写默认配置 ==========
            System.out.println("[1/1] Installing MySQL...");
            File zipFile = findMysqlZip(mysqlDir);
            if (zipFile == null) {
                System.out.println("[ERROR] MySQL zip not found in data/mysql/");
                System.exit(1);
            }
            extractZip(zipFile, mysqlDir);
            for (File sub : mysqlDir.listFiles()) {
                if (sub.isDirectory() && sub.getName().startsWith("mysql-") && new File(sub, "bin/mysqld.exe").exists()) {
                    copyDirectory(sub, mysqlDir);
                    deleteDirectory(sub);
                    break;
                }
            }
            zipFile.delete();
            System.out.println("[OK] MySQL extracted");

            // 写默认配置（密码和库都已预初始化在 data 目录中）
            writeDbProps(configDir, dbPropsFile, "127.0.0.1", "3306", "root", "mcsm2024");
            printDone();
            return;
        }

        // ========== 已安装 ==========
        System.out.println("[INFO] MySQL binaries found.");

        if (dbPropsFile.exists()) {
            System.out.println("[OK] Already configured. Run start.bat to launch.");
            System.out.println();
            return;
        }

        // 有预初始化的数据目录 → 直接写配置
        if (new File(mysqlData, "ibdata1").exists()) {
            System.out.println("[INFO] Pre-initialized MySQL data found.");
            writeDbProps(configDir, dbPropsFile, "127.0.0.1", "3306", "root", "mcsm2024");
            printDone();
            return;
        }

        // 无预初始化数据 → 检查是否有外部 MySQL，否则初始化本地
        boolean hasExternal = false;
        try (java.net.Socket s = new java.net.Socket("127.0.0.1", 3306)) { hasExternal = true; } catch (Exception ignore) {}

        if (!hasExternal) {
            System.out.println("[ERROR] MySQL data directory not found and no external MySQL running.");
            System.out.println("  Expected: " + mysqlData.getAbsolutePath());
            System.out.println("  Please make sure the distribution package is complete.");
            System.exit(1);
        }

        System.out.println("[INFO] External MySQL detected on port 3306.");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        String dbHost = "127.0.0.1";
        String dbPort = "3306";
        String dbUser = "root";
        String dbPass = "";

        // 尝试默认
        if (testConnection(dbHost, dbPort, "root", "mcsm2024")) {
            dbPass = "mcsm2024";
            System.out.println("[OK] Connected with default credentials.");
        } else {
            while (true) {
                System.out.print("  Host [" + dbHost + "]: ");
                String h = scanner.nextLine().trim();
                if (!h.isEmpty()) dbHost = h;
                System.out.print("  Port [" + dbPort + "]: ");
                String p = scanner.nextLine().trim();
                if (!p.isEmpty()) dbPort = p;
                System.out.print("  Username: ");
                dbUser = scanner.nextLine().trim();
                if (dbUser.isEmpty()) continue;
                System.out.print("  Password: ");
                dbPass = scanner.nextLine().trim();
                if (testConnection(dbHost, dbPort, dbUser, dbPass)) {
                    System.out.println("[OK] Connected.");
                    break;
                }
                System.out.println("[ERROR] Connection failed. Try again.");
            }
        }

        // 建库 + 导表 + 写配置
        initDatabase(dbHost, dbPort, dbUser, dbPass, initSqlFile);
        writeDbProps(configDir, dbPropsFile, dbHost, dbPort, dbUser, dbPass);
        printDone();
    }

    // ==================== 工具方法 ====================

    private static void writeDbProps(File configDir, File file, String host, String port, String user, String pass) throws IOException {
        configDir.mkdirs();
        String content = "# MCSM Database Configuration\n"
                + "db.host=" + host + "\n"
                + "db.port=" + port + "\n"
                + "db.username=" + user + "\n"
                + "db.password=" + pass + "\n"
                + "db.name=" + DB_NAME + "\n";
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        System.out.println("[OK] Config saved to data/config/db.properties");
    }

    private static void initDatabase(String host, String port, String user, String pass, File initSqlFile) throws Exception {
        System.out.println("[Init] Creating database...");
        String baseUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(baseUrl, user, pass);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + DB_NAME + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        System.out.println("[OK] Database ready");

        if (initSqlFile.exists()) {
            System.out.println("[Init] Importing tables...");
            String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + DB_NAME
                    + "?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&allowMultiQueries=true";
            try (Connection conn = DriverManager.getConnection(dbUrl, user, pass);
                 Statement stmt = conn.createStatement()) {
                String sql = Files.readString(initSqlFile.toPath(), StandardCharsets.UTF_8);
                for (String block : sql.split(";")) {
                    String cleaned = stripComments(block);
                    if (!cleaned.isEmpty()) {
                        try { stmt.executeUpdate(cleaned); } catch (Exception ignore) {}
                    }
                }
            }
            System.out.println("[OK] Tables imported");
        }
    }

    private static void printDone() {
        System.out.println();
        System.out.println("  ========================================");
        System.out.println("         Setup complete. Run start.bat!");
        System.out.println("  ========================================");
        System.out.println();
    }

    private static boolean testConnection(String host, String port, String user, String pass) {
        String url = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        try (Connection c = DriverManager.getConnection(url, user, pass)) { return true; } catch (Exception e) { return false; }
    }

    private static File findMysqlZip(File dir) {
        File[] zips = dir.listFiles((d, n) -> n.startsWith("mysql-") && n.endsWith(".zip"));
        return (zips != null && zips.length > 0) ? zips[0] : null;
    }

    private static void extractZip(File zip, File dest) throws IOException {
        dest.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                File out = new File(dest, e.getName());
                if (!out.getCanonicalPath().startsWith(dest.getCanonicalPath() + File.separator))
                    throw new IOException("Zip slip: " + e.getName());
                if (e.isDirectory()) { out.mkdirs(); }
                else {
                    out.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] buf = new byte[8192]; int n;
                        while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void copyDirectory(File src, File dest) {
        try {
            Files.walk(src.toPath()).forEach(s -> {
                try {
                    Path t = dest.toPath().resolve(src.toPath().relativize(s));
                    if (Files.isDirectory(s)) Files.createDirectories(t);
                    else Files.copy(s, t, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) { throw new RuntimeException(ex); }
            });
        } catch (IOException ex) { throw new RuntimeException(ex); }
    }

    private static void deleteDirectory(File dir) {
        File[] fs = dir.listFiles();
        if (fs != null) { for (File f : fs) { if (f.isDirectory()) deleteDirectory(f); else f.delete(); } }
        dir.delete();
    }

    private static String stripComments(String block) {
        StringBuilder sb = new StringBuilder();
        for (String line : block.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("--") || t.startsWith("/*") || t.startsWith("*/") || t.startsWith("/*!")) continue;
            sb.append(t).append(" ");
        }
        return sb.toString().trim();
    }
}
