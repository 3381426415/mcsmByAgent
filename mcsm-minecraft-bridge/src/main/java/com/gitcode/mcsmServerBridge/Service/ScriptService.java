package com.gitcode.mcsmServerBridge.Service;




import com.gitcode.mcsmServerBridge.Common.Result;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.bukkit.Bukkit;
import java.util.concurrent.CompletableFuture;
import com.gitcode.mcsmServerBridge.McsmBridge;
import groovy.lang.Script;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptService {

    // 缓存池：Key 是脚本内容（避免 hashCode 碰撞），Value 是编译好的 Script 对象
    // 使用 LinkedHashMap 实现 LRU 淘汰，防止内存泄漏
    private final Map<String, Script> scriptCache = new java.util.LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Script> eldest) {
            return size() > 100; // 最多缓存 100 个脚本
        }
    };
    private final Binding sharedBinding;

    public ScriptService() {
        // 初始化共享变量绑定
        this.sharedBinding = new Binding();
        this.sharedBinding.setVariable("bukkit", Bukkit.getServer());
    }

    public CompletableFuture<Result<Object>> executeDynamicCode(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 用代码内容本身做 key，避免 hashCode 碰撞
                Script script;
                synchronized (scriptCache) {
                    script = scriptCache.get(code);
                    if (script == null) {
                        // 每次 parse 用新 GroovyShell（线程安全）
                        GroovyShell shell = new GroovyShell(sharedBinding);
                        script = shell.parse(code);
                        scriptCache.put(code, script);
                    }
                }

                // 2. 切换到主线程执行
                final Script finalScript = script;
                Object result = Bukkit.getScheduler().callSyncMethod(
                        McsmBridge.getInstance(),
                        finalScript::run
                ).get();

                return Result.success("Execution success", result);
            } catch (Exception e) {
                return Result.error("Script execution error: " + e.getMessage());
            }
        });
    }

    /**
     * 清理缓存，防止内存占用过大
     */
    public void clearCache() {
        synchronized (scriptCache) {
            scriptCache.clear();
        }
    }
}