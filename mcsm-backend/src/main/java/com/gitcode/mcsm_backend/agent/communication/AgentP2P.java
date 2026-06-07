package com.gitcode.mcsm_backend.agent.communication;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Agent P2P 通信 - 两个 Agent 之间直接传递结构化数据
 */
@Slf4j
public class AgentP2P {

    private final Map<String, LinkedBlockingQueue<P2PMessage>> mailboxes = new ConcurrentHashMap<>();

    public void register(String agentId) {
        mailboxes.putIfAbsent(agentId, new LinkedBlockingQueue<>());
    }

    public void unregister(String agentId) {
        mailboxes.remove(agentId);
    }

    public void send(String from, String to, String type, Object data) {
        LinkedBlockingQueue<P2PMessage> mailbox = mailboxes.get(to);
        if (mailbox != null) {
            mailbox.offer(new P2PMessage(from, to, type, data, System.currentTimeMillis()));
            log.info("[P2P] {} -> {}: type={}", from, to, type);
        } else {
            log.info("[P2P] 目标 Agent 未注册: {}", to);
        }
    }

    public P2PMessage receive(String agentId, long timeoutMs) throws InterruptedException {
        LinkedBlockingQueue<P2PMessage> mailbox = mailboxes.get(agentId);
        if (mailbox == null) return null;
        return mailbox.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public record P2PMessage(String from, String to, String type, Object data, long timestamp) {}
}
