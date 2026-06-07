package com.gitcode.mcsm_backend.agent.memory;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 本地知识文件存储
 * 管理 data/knowledge/ 目录下的 JSON 知识文件，提供查询和添加接口
 */
@Slf4j
public class LocalKnowledgeStore {

    private final Path knowledgeDir;
    private final Map<String, KnowledgeFile> cache = new ConcurrentHashMap<>();

    public LocalKnowledgeStore(String basePath) {
        this.knowledgeDir = Paths.get(basePath, "data", "knowledge");
        initDirectory();
        loadAll();
    }

    private void initDirectory() {
        try {
            Files.createDirectories(knowledgeDir);
            Files.createDirectories(knowledgeDir.resolve("custom"));
        } catch (IOException e) {
            log.error("[LocalKnowledgeStore] 创建知识目录失败: {}", e.getMessage());
        }
    }

    private void loadAll() {
        try {
            try (var files = Files.list(knowledgeDir)) {
                files.filter(p -> p.toString().endsWith(".json")).forEach(this::loadFile);
            }
            Path customDir = knowledgeDir.resolve("custom");
            if (Files.isDirectory(customDir)) {
                try (var files = Files.list(customDir)) {
                    files.filter(p -> p.toString().endsWith(".json")).forEach(this::loadFile);
                }
            }
        } catch (IOException e) {
            log.error("[LocalKnowledgeStore] 加载知识文件失败: {}", e.getMessage());
        }
    }

    private void loadFile(Path path) {
        try {
            String content = Files.readString(path);
            KnowledgeFile kf = parseKnowledgeFile(content, path);
            if (kf != null) {
                cache.put(kf.category, kf);
            }
        } catch (IOException e) {
            log.error("[LocalKnowledgeStore] 读取文件失败: {} - {}", path, e.getMessage());
        }
    }

    private KnowledgeFile parseKnowledgeFile(String json, Path path) {
        try {
            KnowledgeFile kf = new KnowledgeFile();
            kf.filePath = path;
            kf.category = extractString(json, "category");
            kf.version = extractString(json, "version");
            kf.lastUpdated = extractString(json, "lastUpdated");
            kf.entries = parseEntries(json);
            return kf;
        } catch (Exception e) {
            log.error("[LocalKnowledgeStore] 解析 JSON 失败: {} - {}", path, e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> parseEntries(String json) {
        List<Map<String, Object>> entries = new ArrayList<>();
        int start = json.indexOf("\"entries\"");
        if (start < 0) return entries;

        int arrStart = json.indexOf('[', start);
        if (arrStart < 0) return entries;

        int depth = 0;
        int arrEnd = -1;
        for (int i = arrStart; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') {
                depth--;
                if (depth == 0) { arrEnd = i; break; }
            }
        }
        if (arrEnd < 0) return entries;

        String arrContent = json.substring(arrStart + 1, arrEnd);
        int objStart = -1;
        depth = 0;
        for (int i = 0; i < arrContent.length(); i++) {
            if (arrContent.charAt(i) == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (arrContent.charAt(i) == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = arrContent.substring(objStart, i + 1);
                    entries.add(parseObject(obj));
                    objStart = -1;
                }
            }
        }
        return entries;
    }

    private Map<String, Object> parseObject(String obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        int i = 0;
        while (i < obj.length()) {
            int keyStart = obj.indexOf('"', i);
            if (keyStart < 0) break;
            int keyEnd = obj.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String key = obj.substring(keyStart + 1, keyEnd);

            int colon = obj.indexOf(':', keyEnd);
            if (colon < 0) break;

            int valStart = colon + 1;
            while (valStart < obj.length() && obj.charAt(valStart) == ' ') valStart++;

            if (valStart >= obj.length()) break;

            if (obj.charAt(valStart) == '"') {
                int valEnd = valStart + 1;
                while (valEnd < obj.length()) {
                    if (obj.charAt(valEnd) == '\\') {
                        valEnd += 2;
                    } else if (obj.charAt(valEnd) == '"') {
                        break;
                    } else {
                        valEnd++;
                    }
                }
                if (valEnd >= obj.length()) break;
                map.put(key, unescapeJson(obj.substring(valStart + 1, valEnd)));
                i = valEnd + 1;
            } else if (obj.charAt(valStart) == '[') {
                int depth = 0;
                int arrEnd = -1;
                for (int j = valStart; j < obj.length(); j++) {
                    if (obj.charAt(j) == '[') depth++;
                    else if (obj.charAt(j) == ']') {
                        depth--;
                        if (depth == 0) { arrEnd = j; break; }
                    }
                }
                if (arrEnd >= 0) {
                    String arrStr = obj.substring(valStart + 1, arrEnd);
                    List<String> arr = new ArrayList<>();
                    for (String s : arrStr.split(",")) {
                        s = s.trim();
                        if (s.startsWith("\"") && s.endsWith("\"")) {
                            s = s.substring(1, s.length() - 1);
                        }
                        arr.add(s);
                    }
                    map.put(key, arr);
                    i = arrEnd + 1;
                } else {
                    i = valStart + 1;
                }
            } else if (obj.charAt(valStart) == '{') {
                i = valStart + 1;
            } else {
                int valEnd = valStart;
                while (valEnd < obj.length() && obj.charAt(valEnd) != ',' && obj.charAt(valEnd) != '}') valEnd++;
                String val = obj.substring(valStart, valEnd).trim();
                if ("true".equals(val)) map.put(key, true);
                else if ("false".equals(val)) map.put(key, false);
                else {
                    try { map.put(key, Long.parseLong(val)); }
                    catch (NumberFormatException e1) {
                        try { map.put(key, Double.parseDouble(val)); }
                        catch (NumberFormatException e2) { map.put(key, val); }
                    }
                }
                i = valEnd + 1;
            }
        }
        return map;
    }

    private String extractString(String json, String key) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + pattern.length());
        if (colon < 0) return null;
        int valStart = json.indexOf('"', colon + 1);
        if (valStart < 0) return null;
        int valEnd = json.indexOf('"', valStart + 1);
        if (valEnd < 0) return null;
        return json.substring(valStart + 1, valEnd);
    }

    public List<Map<String, Object>> search(String query, String category) {
        List<Map<String, Object>> results = new ArrayList<>();
        String lowerQuery = query != null ? query.toLowerCase() : "";

        for (KnowledgeFile kf : cache.values()) {
            if (category != null && !category.isEmpty() && !category.equals(kf.category)) {
                continue;
            }
            for (Map<String, Object> entry : kf.entries) {
                if (matchesQuery(entry, lowerQuery)) {
                    Map<String, Object> result = new LinkedHashMap<>(entry);
                    result.put("_category", kf.category);
                    results.add(result);
                }
            }
        }
        return results;
    }

    public Map<String, Object> getById(String id, String category) {
        for (KnowledgeFile kf : cache.values()) {
            if (category != null && !category.isEmpty() && !category.equals(kf.category)) {
                continue;
            }
            for (Map<String, Object> entry : kf.entries) {
                Object entryId = entry.get("id");
                if (entryId != null && entryId.toString().equalsIgnoreCase(id)) {
                    Map<String, Object> result = new LinkedHashMap<>(entry);
                    result.put("_category", kf.category);
                    return result;
                }
            }
        }
        return null;
    }

    private boolean matchesQuery(Map<String, Object> entry, String lowerQuery) {
        if (lowerQuery.isEmpty()) return true;

        for (String key : new String[]{"id", "name", "description"}) {
            Object val = entry.get(key);
            if (val != null && val.toString().toLowerCase().contains(lowerQuery)) {
                return true;
            }
        }

        Object tags = entry.get("tags");
        if (tags instanceof List<?> tagList) {
            for (Object tag : tagList) {
                if (tag.toString().toLowerCase().contains(lowerQuery)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean addEntry(String category, Map<String, Object> entry) {
        KnowledgeFile kf = cache.get(category);
        if (kf == null) {
            kf = new KnowledgeFile();
            kf.category = category;
            kf.version = "1.0";
            kf.entries = new ArrayList<>();
            kf.filePath = knowledgeDir.resolve(category + ".json");
            cache.put(category, kf);
        }
        kf.entries.add(entry);
        kf.lastUpdated = LocalDateTime.now().toString();
        return saveFile(kf);
    }

    public List<String> listCategories() {
        return new ArrayList<>(cache.keySet());
    }

    public List<Map<String, Object>> getAllByCategory(String category) {
        KnowledgeFile kf = cache.get(category);
        if (kf == null) return List.of();
        return new ArrayList<>(kf.entries);
    }

    private boolean saveFile(KnowledgeFile kf) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"category\": \"").append(escapeJson(kf.category)).append("\",\n");
            sb.append("  \"version\": \"").append(escapeJson(kf.version)).append("\",\n");
            sb.append("  \"lastUpdated\": \"").append(escapeJson(kf.lastUpdated)).append("\",\n");
            sb.append("  \"entries\": [\n");

            for (int i = 0; i < kf.entries.size(); i++) {
                sb.append("    ");
                sb.append(mapToJson(kf.entries.get(i)));
                if (i < kf.entries.size() - 1) sb.append(",");
                sb.append("\n");
            }

            sb.append("  ]\n");
            sb.append("}\n");

            Files.writeString(kf.filePath, sb.toString());
            return true;
        } catch (IOException e) {
            log.error("[LocalKnowledgeStore] 保存文件失败: {} - {}", kf.filePath, e.getMessage());
            return false;
        }
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(escapeJson(e.getKey())).append("\": ");
            sb.append(valueToJson(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + escapeJson(s) + "\"";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(valueToJson(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        if (value instanceof Map<?, ?> map) {
            return mapToJson((Map<String, Object>) map);
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static class KnowledgeFile {
        Path filePath;
        String category;
        String version;
        String lastUpdated;
        List<Map<String, Object>> entries;
    }
}
