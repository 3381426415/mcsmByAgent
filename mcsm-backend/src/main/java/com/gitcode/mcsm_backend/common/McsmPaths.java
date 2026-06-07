package com.gitcode.mcsm_backend.common;

import java.io.File;

public final class McsmPaths {
    private McsmPaths() {}

    // ==================== Directory constants ====================
    public static final String DATA_DIR          = "data";
    public static final String CONFIG_DIR        = DATA_DIR + "/config";
    public static final String TEMPLATES_DIR     = DATA_DIR + "/templates";
    public static final String SERVERS_DIR       = DATA_DIR + "/servers";
    public static final String KNOWLEDGE_DIR     = DATA_DIR + "/knowledge";
    public static final String AGENT_LOGS_DIR    = DATA_DIR + "/logs";
    public static final String BACKUPS_DIR       = DATA_DIR + "/backups";
    public static final String PLUGINS_DIR       = DATA_DIR + "/plugins";
    public static final String FRP_DIR           = DATA_DIR + "/frp";

    // ==================== File constants ====================
    public static final String APPLICATION_PROPERTIES         = CONFIG_DIR + "/application.properties";
    public static final String APPLICATION_PROPERTIES_EXAMPLE = CONFIG_DIR + "/application.properties.example";
    public static final String AGENT_CONFIG_YML               = CONFIG_DIR + "/agent-config.yml";
    public static final String LLM_PROVIDERS_YML              = CONFIG_DIR + "/llm-providers.yml";
    public static final String DB_PROPERTIES                  = CONFIG_DIR + "/db.properties";
    public static final String SETUP_FLAG                     = DATA_DIR + "/.mcsm_installed";
    public static final String BRIDGE_JAR                     = PLUGINS_DIR + "/mcsm-bridge.jar";
    public static final String FRPC_CONFIG                    = FRP_DIR + "/frpc.toml";

    public static File file(String path) {
        return new File(path);
    }

    public static File writeFile(String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        return file;
    }

    public static void initDirectories() {
        new File(CONFIG_DIR).mkdirs();
        new File(TEMPLATES_DIR).mkdirs();
        new File(SERVERS_DIR).mkdirs();
        new File(KNOWLEDGE_DIR).mkdirs();
        new File(AGENT_LOGS_DIR).mkdirs();
        new File(BACKUPS_DIR).mkdirs();
        new File(PLUGINS_DIR).mkdirs();
        new File(FRP_DIR).mkdirs();
    }
}
