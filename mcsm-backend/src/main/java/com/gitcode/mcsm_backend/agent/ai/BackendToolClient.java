package com.gitcode.mcsm_backend.agent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * 后端工具 HTTP 客户端
 *
 * 调用后端 /api/agent-tools/list 和 /api/agent-tools/call
 * 使用 JWT 作为认证凭证
 */
@Slf4j
public class BackendToolClient {

    private final String backendUrl;
    private final String jwtToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BackendToolClient(String backendUrl, String jwtToken) {
        this.backendUrl = backendUrl;
        this.jwtToken = jwtToken;
        this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 从后端获取可用工具列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchBackendTools() {
        try {
            String json = objectMapper.writeValueAsString(Map.of());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + "/api/agent-tools/list"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + jwtToken)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[BackendToolClient] 获取工具列表失败 HTTP {}", response.statusCode());
                return List.of();
            }

            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            int code = ((Number) result.getOrDefault("code", 3000)).intValue();
            if (code == 2000 && result.containsKey("data")) {
                return (List<Map<String, Object>>) result.get("data");
            }
            log.error("[BackendToolClient] 获取后端工具失败: {}", result.get("msg"));
            return List.of();
        } catch (Exception e) {
            log.error("[BackendToolClient] 连接后端失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 调用后端工具
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> callBackendTool(String name, Map<String, Object> arguments) {
        try {
            Map<String, Object> body = Map.of(
                    "name", name,
                    "arguments", arguments != null ? arguments : Map.of()
            );
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + "/api/agent-tools/call"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + jwtToken)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[BackendToolClient] 工具调用失败 HTTP {}: {}", response.statusCode(), response.body());
                return Map.of("code", 3000, "msg", "后端调用失败 HTTP " + response.statusCode());
            }

            return objectMapper.readValue(response.body(), Map.class);
        } catch (Exception e) {
            log.error("[BackendToolClient] 调用后端工具失败: {}", e.getMessage());
            return Map.of("code", 3000, "msg", "后端工具调用失败: " + e.getMessage());
        }
    }

    /**
     * 检查后端是否可达
     */
    public boolean isBackendAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(backendUrl + "/api/agent-tools/list"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + jwtToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
