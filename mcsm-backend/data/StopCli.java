import java.io.*;
import java.util.Scanner;

public class StopCli {

    public static void main(String[] args) {
        System.out.println();
        System.out.println("  ========================================");
        System.out.println("         MCSM MySQL Manager");
        System.out.println("  ========================================");
        System.out.println();
        System.out.println("  [1] Stop MySQL only");
        System.out.println("  [2] Stop and reset MySQL (back to fresh state)");
        System.out.println("  [0] Cancel");
        System.out.println();
        System.out.print("  Choose: ");

        Scanner scanner = new Scanner(System.in);
        String choice = scanner.nextLine().trim();

        if ("1".equals(choice)) {
            stopMysql();
        } else if ("2".equals(choice)) {
            stopMysql();
            reset();
            System.out.println("Done. Run install-mysql.bat to set up again.");
        } else {
            System.out.println("Cancelled.");
        }

        System.out.println();
        System.out.println("Press Enter to exit...");
        scanner.nextLine();
    }

    private static void stopMysql() {
        System.out.println("Stopping MySQL on port 3306...");
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                // 找到占用 3306 端口的 PID 并杀掉
                Process p = new ProcessBuilder("cmd", "/c",
                        "for /f \"tokens=5\" %a in ('netstat -ano ^| findstr :3306.*LISTENING') do taskkill /f /pid %a")
                        .redirectErrorStream(true).start();
                p.waitFor();
            } else {
                // Linux/Mac: 找到占用 3306 端口的进程并杀掉
                new ProcessBuilder("sh", "-c",
                        "lsof -ti:3306 | xargs -r kill -9")
                        .redirectErrorStream(true).start().waitFor();
            }
            System.out.println("Done.");
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private static void reset() {
        System.out.println("Removing MySQL data and config...");
        delete(new File("data/mysql/data"));
        new File("data/config/db.properties").delete();
        new File("data/.mcsm_installed").delete();
        System.out.println("Reset complete.");
    }

    private static void delete(File f) {
        if (!f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) delete(c);
        }
        f.delete();
    }
}
