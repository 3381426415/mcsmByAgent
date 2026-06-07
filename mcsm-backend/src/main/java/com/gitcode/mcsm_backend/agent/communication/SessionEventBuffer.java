package com.gitcode.mcsm_backend.agent.communication;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话事件缓存 — 支持 ACK 确认和断线重连恢复
 * 每个 session 缓存最近的事件，前端 ACK 后清除，断线重连时可重发漏掉的事件
 */
public class SessionEventBuffer {

    private static final int MAX_EVENTS = 200;
    private static final long TTL_MS = 5 * 60 * 1000; // 5 分钟

    private final ConcurrentHashMap<String, SessionBuffer> sessions = new ConcurrentHashMap<>();

    public record BufferedEvent(int seq, String type, Object data, long timestamp) {}

    /**
     * 缓存一个事件
     */
    public void buffer(String sessionId, int seq, String type, Object data) {
        SessionBuffer buf = sessions.computeIfAbsent(sessionId, k -> new SessionBuffer());
        synchronized (buf.events) {
            buf.events.put(seq, new BufferedEvent(seq, type, data, System.currentTimeMillis()));
            buf.lastSeq.set(seq);
            buf.lastActivityTime = System.currentTimeMillis();
            while (buf.events.size() > MAX_EVENTS) {
                Integer oldest = buf.events.keySet().iterator().next();
                buf.events.remove(oldest);
            }
        }
    }

    /**
     * 前端确认收到，清除 <= seq 的所有事件
     */
    public void ack(String sessionId, int seq) {
        SessionBuffer buf = sessions.get(sessionId);
        if (buf == null) return;
        synchronized (buf.events) {
            buf.events.keySet().removeIf(k -> k <= seq);
            buf.lastAckedSeq.set(Math.max(buf.lastAckedSeq.get(), seq));
        }
    }

    /**
     * 获取 > lastSeq 的所有缓存事件（断线恢复用）
     */
    public List<BufferedEvent> getSince(String sessionId, int lastSeq) {
        SessionBuffer buf = sessions.get(sessionId);
        if (buf == null) return Collections.emptyList();
        long now = System.currentTimeMillis();
        List<BufferedEvent> result = new ArrayList<>();
        synchronized (buf.events) {
            buf.events.values().removeIf(e -> now - e.timestamp() > TTL_MS);
            for (BufferedEvent e : buf.events.values()) {
                if (e.seq() > lastSeq) {
                    result.add(e);
                }
            }
        }
        return result;
    }

    /**
     * 获取当前 session 最新的 seq 号
     */
    public int getLastSeq(String sessionId) {
        SessionBuffer buf = sessions.get(sessionId);
        return buf != null ? buf.lastSeq.get() : 0;
    }

    /**
     * 获取最后确认的 seq 号
     */
    public int getLastAckedSeq(String sessionId) {
        SessionBuffer buf = sessions.get(sessionId);
        return buf != null ? buf.lastAckedSeq.get() : 0;
    }

    /**
     * 清空 session 缓存
     */
    public void clear(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * 定期清理过期 session（可由定时任务调用）
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            SessionBuffer buf = entry.getValue();
            synchronized (buf.events) {
                if (buf.events.isEmpty() && now - buf.lastActivityTime > TTL_MS) {
                    return true;
                }
                buf.events.values().removeIf(e -> now - e.timestamp() > TTL_MS);
                return buf.events.isEmpty();
            }
        });
    }

    private static class SessionBuffer {
        final LinkedHashMap<Integer, BufferedEvent> events = new LinkedHashMap<>();
        final AtomicInteger lastSeq = new AtomicInteger(0);
        final AtomicInteger lastAckedSeq = new AtomicInteger(0);
        volatile long lastActivityTime = System.currentTimeMillis();
    }
}
