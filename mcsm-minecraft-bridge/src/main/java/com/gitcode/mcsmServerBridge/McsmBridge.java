package com.gitcode.mcsmServerBridge;

import com.gitcode.mcsmServerBridge.Manager.DatabaseManager;
import com.gitcode.mcsmServerBridge.Service.*;
import com.gitcode.mcsmServerBridge.Service.PluginManagerService;
import com.gitcode.mcsmServerBridge.command.BindCommand;
import com.gitcode.mcsmServerBridge.command.ClaimCommand;
import com.gitcode.mcsmServerBridge.websocket.PluginWebSocketClient;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;

public class McsmBridge extends JavaPlugin {

    private static McsmBridge instance;
    private DatabaseManager dbManager;
    private PlayerService playerService;
    private PluginWebSocketClient wsClient;
    private ScriptService scriptService;
    private NbtService nbtService;
    private PendingItemService pendingItemService;
    private PluginManagerService pluginManagerService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("§a========================================");
        getLogger().info("§a  MCSM Bridge starting...");
        getLogger().info("§a========================================");

        String backendUrl = getConfig().getString("backend-url", "ws://127.0.0.1:8000/ws/plugin");
        String serverId = getConfig().getString("server-id", "default");
        String secret = getConfig().getString("secret", "");

        // 1. Init database
        getLogger().info("§7[1/5] Connecting to database...");
        dbManager = new DatabaseManager(this);
        try (java.sql.Connection testConn = dbManager.getConnection()) {
            getLogger().info("§a  ✓ Database connected");
        } catch (Exception e) {
            getLogger().severe("§cDatabase connection failed: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Init pending item service
        getLogger().info("§7[2/5] Initializing pending item service...");
        this.pendingItemService = new PendingItemService(this, dbManager);
        getLogger().info("§a  ✓ Pending item service ready");

        // 3. Register commands
        getLogger().info("§7[3/5] Registering commands...");
        if (getCommand("claim") != null) {
            getCommand("claim").setExecutor(new ClaimCommand(this, pendingItemService));
            getLogger().info("§a  ✓ /claim registered");
        } else {
            getLogger().warning("§e  ⚠ /claim failed, check plugin.yml");
        }

        if (getCommand("bind") != null) {
            getCommand("bind").setExecutor(new BindCommand(this, dbManager));
            getLogger().info("§a  ✓ /bind registered");
        } else {
            getLogger().warning("§e  ⚠ /bind failed, check plugin.yml");
        }

        // 4. Init NBT service
        getLogger().info("§7[4/5] Initializing NBT service...");
        java.io.File worldFolder = org.bukkit.Bukkit.getWorlds().get(0).getWorldFolder();
        this.nbtService = new NbtService(worldFolder);
        getLogger().info("§a  ✓ NBT service ready");

        // 5. Init business services
        getLogger().info("§7[5/5] Initializing business services...");
        playerService = new PlayerService(this);
        scriptService = new ScriptService();
        playerService.resetAllPlayers();
        this.pluginManagerService = new PluginManagerService(this);
        getLogger().info("§a  ✓ Business services ready");

        // 6. Register listeners
        getServer().getPluginManager().registerEvents(new PlayerStatusListenerService(this, dbManager, serverId), this);
        getLogger().info("§a  ✓ Player listener registered");

        // 7. 连接后端 WebSocket
        try {
            // 构建带参数的 URI
            String separator = backendUrl.contains("?") ? "&" : "?";
            String wsUrl = backendUrl + separator + "serverId=" + serverId;
            if (secret != null && !secret.isEmpty()) {
                wsUrl += "&secret=" + secret;
            }
            wsClient = new PluginWebSocketClient(this, new URI(wsUrl));
            wsClient.connect();
            getLogger().info("§a  ✓ WebSocket client started, connecting: " + backendUrl);
        } catch (Exception e) {
            getLogger().severe("§c  ✗ WebSocket connection failed: " + e.getMessage());
            getLogger().warning("§e  Auto-reconnect enabled, plugin will retry");
        }

        getLogger().info("§a========================================");
        getLogger().info("§a  MCSM Bridge loaded!");
        getLogger().info("§a  Backend: " + backendUrl);
        getLogger().info("§a  Server ID: " + serverId);
        getLogger().info("§a========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("§cMCSM Bridge shutting down...");
        if (wsClient != null) {
            wsClient.closeIntentionally();
        }
        if (playerService != null) {
            playerService.resetAllPlayers();
        }
        if (dbManager != null) {
            dbManager.close();
        }
        instance = null;
        getLogger().info("§cMCSM Bridge stopped");
    }

    // Getter 方法
    public PlayerService getPlayerService() { return playerService; }
    public static McsmBridge getInstance() { return instance; }
    public ScriptService getScriptService() { return scriptService; }
    public NbtService getNbtService() { return nbtService; }
    public PendingItemService getPendingItemService() { return pendingItemService; }
    public PluginManagerService getPluginManagerService() { return pluginManagerService; }
    public DatabaseManager getDbManager() { return dbManager; }
}
