package com.gitcode.mcsm_backend.util;

/**
 * NBT/SNBT 格式 → 标准 JSON 转换工具
 */
public class NbtUtils {

    private NbtUtils() {}  // 工具类，禁止实例化

    /**
     * 将 Minecraft NBT/SNBT 格式字符串清洗为标准 JSON
     * <p>
     * 处理内容：
     * <ul>
     *   <li>去除 NBT 类型后缀（1b, 2s, 3L, 4f, 5d → 1, 2, 3, 4, 5）</li>
     *   <li>给无引号的 Key 加双引号</li>
     *   <li>去除 Minecraft 颜色代码（§0-9a-fk-orx）</li>
     *   <li>单引号值转双引号</li>
     * </ul>
     *
     * @param raw 原始 NBT/SNBT 字符串
     * @return 标准 JSON 字符串
     */
    public static String clean(String raw) {
        // 1. 去掉 NBT 类型后缀（1b, 2s, 3L, 4f, 5d → 1, 2, 3, 4, 5）
        String cleaned = raw.replaceAll("(?i)(\\d+)[bslfd]", "$1");

        // 2. 给无引号的 Key 加引号
        cleaned = cleaned.replaceAll("([{,])\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*:",
                "$1\"$2\":");

        // 3. 去掉 Minecraft 颜色代码
        cleaned = cleaned.replaceAll("§[0-9a-fk-orx]", "");

        // 4. 处理单引号包裹的 JSON 值 → 转成双引号并转义
        cleaned = cleaned.replaceAll(":'(\\{[^']*\\})'", ":$1");
        cleaned = cleaned.replaceAll(":'([^']*)'", ":\"$1\"");

        return cleaned;
    }
}
