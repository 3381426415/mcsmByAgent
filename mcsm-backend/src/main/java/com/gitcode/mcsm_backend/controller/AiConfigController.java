package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.config.ProviderConfig;
import com.gitcode.mcsm_backend.service.AgentChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;

@RestController
@RequestMapping("/api/admin/ai-config")
@PreAuthorize("hasAuthority('admin:server')")
public class AiConfigController {

    @Value("${ai.llm.provider:api}")
    private String provider;

    @Value("${ai.llm.base-url:}")
    private String baseUrl;

    @Value("${ai.llm.api-key:}")
    private String apiKey;

    @Value("${ai.llm.pro-model:}")
    private String proModel;

    @Value("${ai.llm.flash-model:}")
    private String flashModel;

    @Autowired
    private ProviderConfig providerConfig;

    @Autowired
    private AgentChatService agentChatService;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();

    @GetMapping
    public Result<Map<String, String>> getConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("provider", provider);
        config.put("baseUrl", baseUrl);
        config.put("apiKey", apiKey);
        config.put("proModel", proModel);
        config.put("flashModel", flashModel);
        return Result.success("获取成功", config);
    }

    @PutMapping
    public Result<String> updateConfig(@RequestBody Map<String, String> body) {
        String newProvider = body.getOrDefault("provider", provider);
        String newBaseUrl = body.getOrDefault("baseUrl", baseUrl);
        String newApiKey = body.getOrDefault("apiKey", apiKey);
        String newProModel = body.getOrDefault("proModel", proModel);
        String newFlashModel = body.getOrDefault("flashModel", flashModel);

        try {
            File propsFile = findApplicationProperties();
            if (propsFile == null || !propsFile.exists()) {
                return Result.error("找不到配置文件");
            }

            String content = Files.readString(propsFile.toPath());

            content = updateProperty(content, "ai.llm.provider", newProvider);
            content = updateProperty(content, "ai.llm.base-url", newBaseUrl);
            content = updateProperty(content, "ai.llm.api-key", newApiKey);
            content = updateProperty(content, "ai.llm.pro-model", newProModel);
            content = updateProperty(content, "ai.llm.flash-model", newFlashModel);

            try (FileWriter fw = new FileWriter(propsFile)) {
                fw.write(content);
            }

            this.provider = newProvider;
            this.baseUrl = newBaseUrl;
            this.apiKey = newApiKey;
            this.proModel = newProModel;
            this.flashModel = newFlashModel;

            // 热更新 Agent 服务的 LLM 配置
            agentChatService.reload(newProvider, newBaseUrl, newApiKey, newProModel, newFlashModel);

            return Result.successMsg("配置已保存");
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /** 获取可用模型列表 */
    @PostMapping("/models")
    public Result<List<Map<String, String>>> listModels(@RequestBody(required = false) Map<String, String> body) {
        String reqBaseUrl = (body != null) ? body.get("baseUrl") : null;
        String reqApiKey = (body != null) ? body.get("apiKey") : null;
        String reqProvider = (body != null) ? body.get("provider") : null;

        if (reqBaseUrl == null || reqBaseUrl.isEmpty()) reqBaseUrl = this.baseUrl;
        if (reqApiKey == null || reqApiKey.isEmpty()) reqApiKey = this.apiKey;
        if (reqProvider == null || reqProvider.isEmpty()) reqProvider = this.provider;

        if (reqApiKey == null || reqApiKey.isEmpty()) {
            return Result.error("请先填写 API Key");
        }

        // 从配置获取模型列表端点
        String modelsEndpoint = providerConfig.getModelsEndpoint(reqProvider);
        if ("none".equalsIgnoreCase(modelsEndpoint)) {
            return Result.error("该厂商不支持模型列表获取");
        }

        // 构造 models 端点 URL（支持绝对 URL 和相对路径）
        String modelsUrl;
        if (modelsEndpoint.startsWith("http://") || modelsEndpoint.startsWith("https://")) {
            modelsUrl = modelsEndpoint;
        } else {
            String baseUrlTrimmed = reqBaseUrl.endsWith("/") ? reqBaseUrl.substring(0, reqBaseUrl.length() - 1) : reqBaseUrl;
            modelsUrl = baseUrlTrimmed + modelsEndpoint;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .header("Authorization", "Bearer " + reqApiKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return Result.error("获取模型列表失败: HTTP " + response.statusCode());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> result = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response.body(), Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");

            List<Map<String, String>> models = new ArrayList<>();
            if (data != null) {
                for (Map<String, Object> m : data) {
                    String id = (String) m.get("id");
                    if (id != null) {
                        Map<String, String> item = new LinkedHashMap<>();
                        item.put("id", id);
                        item.put("ownedBy", String.valueOf(m.getOrDefault("owned_by", "")));
                        models.add(item);
                    }
                }
            }

            models.sort(Comparator.comparing(m -> m.get("id")));
            return Result.success("获取成功", models);
        } catch (Exception e) {
            return Result.error("获取模型列表失败: " + e.getMessage());
        }
    }

    /** 获取 LLM 厂商列表（预设 + 自定义） */
    @GetMapping("/providers")
    public Result<List<Map<String, Object>>> getProviders() {
        return Result.success("获取成功", providerConfig.listProviders());
    }

    /** 添加自定义厂商 */
    @PostMapping("/providers")
    public Result<String> addProvider(@RequestBody Map<String, String> body) {
        String id = body.get("providerId");
        String name = body.get("name");
        String baseUrlVal = body.get("baseUrl");
        String modelsEndpoint = body.get("modelsEndpoint");

        if (id == null || id.isBlank()) return Result.error("厂商标识不能为空");
        if (name == null || name.isBlank()) return Result.error("厂商名称不能为空");
        if (baseUrlVal == null || baseUrlVal.isBlank()) return Result.error("Base URL 不能为空");

        // 只允许英文字母、数字、下划线、横线
        if (!id.matches("^[a-zA-Z0-9_-]+$")) {
            return Result.error("厂商标识只能包含英文字母、数字、下划线和横线");
        }

        if (providerConfig.getProvider(id) != null) {
            return Result.error("厂商标识已存在");
        }

        String ep = (modelsEndpoint != null && !modelsEndpoint.isBlank()) ? modelsEndpoint.trim() : "/models";
        boolean ok = providerConfig.addCustomProvider(id.trim(), name.trim(), baseUrlVal.trim(), ep);
        return ok ? Result.successMsg("添加成功") : Result.error("添加失败");
    }

    /** 删除自定义厂商 */
    @DeleteMapping("/providers/{providerId}")
    public Result<String> deleteProvider(@PathVariable String providerId) {
        if (providerConfig.isBuiltin(providerId)) {
            return Result.error("系统预设厂商不能删除");
        }
        if (providerConfig.getProvider(providerId) == null) {
            return Result.error("厂商不存在");
        }

        boolean ok = providerConfig.deleteCustomProvider(providerId);
        return ok ? Result.successMsg("删除成功") : Result.error("删除失败");
    }

    private String updateProperty(String content, String key, String value) {
        String pattern = key + "=";
        int start = content.indexOf(pattern);
        if (start == -1) {
            return content + "\n" + key + "=" + value;
        }
        int end = content.indexOf('\n', start);
        if (end == -1) {
            end = content.length();
        }
        return content.substring(0, start) + key + "=" + value + content.substring(end);
    }

    private File findApplicationProperties() {
        File config = new File(com.gitcode.mcsm_backend.common.McsmPaths.APPLICATION_PROPERTIES);
        if (config.exists()) return config;
        try {
            new File(com.gitcode.mcsm_backend.common.McsmPaths.CONFIG_DIR).mkdirs();
            File example = new File(com.gitcode.mcsm_backend.common.McsmPaths.APPLICATION_PROPERTIES_EXAMPLE);
            if (example.exists()) {
                Files.copy(example.toPath(), config.toPath());
                return config;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
