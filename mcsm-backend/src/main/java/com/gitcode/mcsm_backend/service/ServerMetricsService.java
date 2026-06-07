package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.common.Result;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.*;

import java.util.*;

/**
 * 服务器性能监控服务
 *
 * 负责采集宿主机硬件指标（CPU、内存、网络），
 * 并通过 ExternalApiClient 获取游戏服务器状态。
 */
/**
 * 服务器指标服务 - 采集 CPU、内存、磁盘等系统指标，定时推送给前端
 */
@Service
@SuppressWarnings("unchecked")
public class ServerMetricsService {

    private final SystemInfo si = new SystemInfo();
    private final HardwareAbstractionLayer hal = si.getHardware();

    private long[] prevTicks = new long[CentralProcessor.TickType.values().length];
    private Map<String, Long> prevNetData = new HashMap<>();
    private volatile Map<String, Object> latestMetrics = new HashMap<>();

    @Autowired
    private ExternalApiClient apiClient;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 初始化：预填一次快照，防止第一次 CPU 计算为空
     */
    @PostConstruct
    public void init() {
        prevTicks = hal.getProcessor().getSystemCpuLoadTicks();

        List<NetworkIF> networkIFs = hal.getNetworkIFs();
        long rx = 0, tx = 0;
        for (NetworkIF net : networkIFs) {
            rx += net.getBytesRecv();
            tx += net.getBytesSent();
        }
        prevNetData.put("rx", rx);
        prevNetData.put("tx", tx);

        collectMetrics();
    }

    /**
     * 每 2 秒采集一次性能数据
     */
    @Scheduled(fixedRate = 2000)
    public void collectMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        CentralProcessor processor = hal.getProcessor();

        // CPU 使用率
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = processor.getSystemCpuLoadTicks();
        metrics.put("cpuUsage", Math.max(0, Math.round(cpuLoad)));
        metrics.put("cpuCores", processor.getLogicalProcessorCount());

        // 内存使用率
        GlobalMemory memory = hal.getMemory();
        long totalMem = memory.getTotal();
        long availableMem = memory.getAvailable();
        metrics.put("memUsage", Math.round((1.0 - (double) availableMem / totalMem) * 100));
        metrics.put("memTotal", formatByte(totalMem));
        metrics.put("memUsed", formatByte(totalMem - availableMem));

        // 网络流量（KB/s）
        List<NetworkIF> networkIFs = hal.getNetworkIFs();
        long rxBytes = 0, txBytes = 0;
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            rxBytes += net.getBytesRecv();
            txBytes += net.getBytesSent();
        }
        long lastRx = prevNetData.getOrDefault("rx", rxBytes);
        long lastTx = prevNetData.getOrDefault("tx", txBytes);
        metrics.put("netIn", Math.max(0, (rxBytes - lastRx) / 2 / 1024));
        metrics.put("netOut", Math.max(0, (txBytes - lastTx) / 2 / 1024));
        prevNetData.put("rx", rxBytes);
        prevNetData.put("tx", txBytes);

        // 系统信息
        metrics.put("os", si.getOperatingSystem().getFamily() + " "
                + si.getOperatingSystem().getBitness() + "bit");
        metrics.put("upTime", formatUptime(si.getOperatingSystem().getSystemUptime()));

        this.latestMetrics = metrics;
        //推送到前端
        Map<String, Object> metricsWithGame = new HashMap<>(metrics);
        messagingTemplate.convertAndSend("/topic/server/metrics", metricsWithGame);
    }

    /**
     * 获取最新缓存性能数据
     */
    public Map<String, Object> getLatestMetrics() {
        return latestMetrics;
    }

    /**
     * 获取游戏服务器在线人数和 TPS（通过插件）
     */
    public Result<Map<String, Object>> getGameplayerAndTps() {
        return apiClient.sendToPlugin("default", "getServerStatus", Map.of());
    }

    /**
     * 获取 TPS（每秒刻度数）
     */
    public String getTps() {
        Result<Map<String, Object>> result = getGameplayerAndTps();
        if (result != null && result.getData() != null) {
            Object tps = result.getData().get("tps");
            return tps != null ? tps.toString() : "无法获取TPS";
        }
        return "无法获取TPS";
    }

    /**
     * 获取在线玩家列表
     * @param useCache 是否使用缓存（暂不支持，始终实时获取）
     */
    public String getOnlinePlayers(boolean useCache) {
        Result<Map<String, Object>> result = getGameplayerAndTps();
        if (result != null && result.getData() != null) {
            Object players = result.getData().get("players");
            return players != null ? players.toString() : "无法获取在线玩家";
        }
        return "无法获取在线玩家";
    }

    // ==================== 格式化工具 ====================

    private String formatByte(long bytes) {
        return String.format("%.1f GB", (double) bytes / 1024 / 1024 / 1024);
    }

    private String formatUptime(long seconds) {
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        return d + "天 " + h + "小时 " + m + "分";
    }
}