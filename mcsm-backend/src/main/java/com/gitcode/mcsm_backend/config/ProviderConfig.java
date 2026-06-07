package com.gitcode.mcsm_backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * LLM 厂商配置 - 从 llm-providers.yml 加载，支持动态增删自定义厂商
 */
@Slf4j
@Service
public class ProviderConfig {

    private static final String CONFIG_FILE = com.gitcode.mcsm_backend.common.McsmPaths.LLM_PROVIDERS_YML;
    private static final Set<String> BUILTIN_IDS = Set.of("mimo", "deepseek");

    /** provider id -> { name, baseUrl, modelsEndpoint, thinkingField, ... } */
    private final Map<String, Map<String, Object>> providers = Collections.synchronizedMap(new LinkedHashMap<>());

    @PostConstruct
    public void init() {
        loadConfig();
    }

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        Path path = Path.of(CONFIG_FILE);
        if (!Files.exists(path)) {
            log.info("配置文件不存在，创建默认配置: {}", CONFIG_FILE);
            createDefaultProviders();
            saveConfig();
            return;
        }

        try (InputStream is = Files.newInputStream(path)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);
            if (root == null || !root.containsKey("providers")) return;

            Map<String, Map<String, Object>> raw = (Map<String, Map<String, Object>>) root.get("providers");
            if (raw != null) {
                providers.clear();
                providers.putAll(raw);
            }
            log.info("加载了 {} 个 LLM 厂商配置", providers.size());
        } catch (Exception e) {
            log.error("加载配置失败: {}", e.getMessage(), e);
        }
    }

    private void createDefaultProviders() {
        Map<String, Object> mimo = new LinkedHashMap<>();
        mimo.put("name", "小米 MiMo(tokenplan-CN)");
        mimo.put("baseUrl", "https://token-plan-cn.xiaomimimo.com/v1");
        mimo.put("modelsEndpoint", "/models");
        mimo.put("thinkingField", "reasoning_content");
        mimo.put("contentField", "content");
        mimo.put("toolCallsField", "tool_calls");
        mimo.put("deltaContentField", "content");
        mimo.put("deltaToolCallsField", "tool_calls");
        providers.put("mimo", mimo);

        Map<String, Object> deepseek = new LinkedHashMap<>();
        deepseek.put("name", "DeepSeek");
        deepseek.put("baseUrl", "https://api.deepseek.com/v1");
        deepseek.put("modelsEndpoint", "https://api.deepseek.com/models");
        deepseek.put("thinkingField", "reasoning_content");
        deepseek.put("contentField", "content");
        deepseek.put("toolCallsField", "tool_calls");
        deepseek.put("deltaContentField", "content");
        deepseek.put("deltaToolCallsField", "tool_calls");
        providers.put("deepseek", deepseek);
    }

    /** 获取指定厂商配置 */
    public Map<String, Object> getProvider(String id) {
        Map<String, Object> p = providers.get(id);
        if (p == null) return null;
        Map<String, Object> item = new LinkedHashMap<>(p);
        item.put("id", id);
        item.put("builtin", BUILTIN_IDS.contains(id));
        return item;
    }

    /** 获取所有厂商列表 */
    public List<Map<String, Object>> listProviders() {
        List<Map<String, Object>> list = new ArrayList<>();
        synchronized (providers) {
            for (Map.Entry<String, Map<String, Object>> entry : providers.entrySet()) {
                Map<String, Object> item = new LinkedHashMap<>(entry.getValue());
                item.put("id", entry.getKey());
                item.put("builtin", BUILTIN_IDS.contains(entry.getKey()));
                list.add(item);
            }
        }
        return list;
    }

    /** 添加自定义厂商并写入 YAML */
    public synchronized boolean addCustomProvider(String id, String name, String baseUrl, String modelsEndpoint) {
        if (providers.containsKey(id)) return false;

        Map<String, Object> newProvider = new LinkedHashMap<>();
        newProvider.put("name", name);
        newProvider.put("baseUrl", baseUrl);
        newProvider.put("modelsEndpoint", modelsEndpoint != null ? modelsEndpoint : "/models");
        newProvider.put("thinkingField", "reasoning_content");
        newProvider.put("contentField", "content");
        newProvider.put("toolCallsField", "tool_calls");
        newProvider.put("deltaContentField", "content");
        newProvider.put("deltaToolCallsField", "tool_calls");

        providers.put(id, newProvider);
        return saveConfig();
    }

    /** 删除自定义厂商并写入 YAML */
    public synchronized boolean deleteCustomProvider(String id) {
        if (BUILTIN_IDS.contains(id)) return false;
        if (!providers.containsKey(id)) return false;

        providers.remove(id);
        return saveConfig();
    }

    /** 检查是否为预设厂商 */
    public boolean isBuiltin(String id) {
        return BUILTIN_IDS.contains(id);
    }

    // --- field getters ---

    public String getThinkingField(String providerId) {
        return getField(providerId, "thinkingField", null);
    }

    public String getDefaultBaseUrl(String providerId) {
        return getField(providerId, "baseUrl", null);
    }

    public String getContentField(String providerId) {
        return getField(providerId, "contentField", "content");
    }

    public String getToolCallsField(String providerId) {
        return getField(providerId, "toolCallsField", "tool_calls");
    }

    public String getDeltaContentField(String providerId) {
        return getField(providerId, "deltaContentField", "content");
    }

    public String getDeltaToolCallsField(String providerId) {
        return getField(providerId, "deltaToolCallsField", "tool_calls");
    }

    public String getModelsEndpoint(String providerId) {
        return getField(providerId, "modelsEndpoint", "/models");
    }

    // --- private helpers ---

    private String getField(String providerId, String field, String defaultValue) {
        Map<String, Object> p = providers.get(providerId);
        if (p == null) return defaultValue;
        Object val = p.get(field);
        return val != null ? val.toString() : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private boolean saveConfig() {
        try {
            String content;
            synchronized (providers) {
                Map<String, Object> root = new LinkedHashMap<>();
                root.put("providers", providers);

                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                options.setPrettyFlow(true);
                options.setIndent(2);

                Yaml yaml = new Yaml(options);
                content = yaml.dump(root);
            }

            Files.writeString(Path.of(CONFIG_FILE), content);
            log.info("配置已保存到 {}", CONFIG_FILE);
            return true;
        } catch (Exception e) {
            log.error("保存配置失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
