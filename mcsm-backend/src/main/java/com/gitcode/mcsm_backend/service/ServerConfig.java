package com.gitcode.mcsm_backend.service;

/**
 * 单个 Minecraft 服务器实例的配置
 */
public class ServerConfig {

    private String name = "默认服务器";
    private boolean enabled = true;
    private String directory = "";
    private String jarFile = "";
    private String javaArgs = "-Xmx2G -Xms1G";
    private int port = 25565;
    private int rconPort = 25575;
    private String rconPassword = "";
    private boolean autoStart = false;
    private String javaHome = "";

    // ========== Getters & Setters ==========

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }

    public String getJarFile() { return jarFile; }
    public void setJarFile(String jarFile) { this.jarFile = jarFile; }

    public String getJavaArgs() { return javaArgs; }
    public void setJavaArgs(String javaArgs) { this.javaArgs = javaArgs; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getRconPort() { return rconPort; }
    public void setRconPort(int rconPort) { this.rconPort = rconPort; }

    public String getRconPassword() { return rconPassword; }
    public void setRconPassword(String rconPassword) { this.rconPassword = rconPassword; }

    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }

    public String getJavaHome() { return javaHome; }
    public void setJavaHome(String javaHome) { this.javaHome = javaHome; }

    /** 从 Map 构建（SnakeYAML 解析结果） */
    @SuppressWarnings("unchecked")
    public static ServerConfig fromMap(java.util.Map<String, Object> map) {
        ServerConfig c = new ServerConfig();
        if (map.containsKey("name")) c.name = String.valueOf(map.get("name"));
        if (map.containsKey("enabled")) c.enabled = Boolean.parseBoolean(String.valueOf(map.get("enabled")));
        if (map.containsKey("directory")) c.directory = String.valueOf(map.get("directory"));
        if (map.containsKey("jarFile")) c.jarFile = String.valueOf(map.get("jarFile"));
        if (map.containsKey("javaArgs")) c.javaArgs = String.valueOf(map.get("javaArgs"));
        if (map.containsKey("port")) c.port = toInt(map.get("port"), 25565);
        if (map.containsKey("rconPort")) c.rconPort = toInt(map.get("rconPort"), 25575);
        if (map.containsKey("rconPassword")) c.rconPassword = String.valueOf(map.get("rconPassword"));
        if (map.containsKey("autoStart")) c.autoStart = Boolean.parseBoolean(String.valueOf(map.get("autoStart")));
        if (map.containsKey("javaHome")) c.javaHome = String.valueOf(map.get("javaHome"));
        return c;
    }

    private static int toInt(Object val, int fallback) {
        try { return Integer.parseInt(String.valueOf(val)); }
        catch (NumberFormatException e) { return fallback; }
    }
}
