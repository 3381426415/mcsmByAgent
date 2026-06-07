package com.gitcode.mcsm_backend.agent.core;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Agent 池化管理
 */
@Slf4j
public class AgentPool {

    private final Map<AgentType, Integer> activeCounts = new ConcurrentHashMap<>();
    private final int maxConcurrentAgents = 8;
    private final Semaphore semaphore = new Semaphore(maxConcurrentAgents);

    public boolean canAcquire() {
        return semaphore.availablePermits() > 0;
    }

    public void acquire(SubAgent agent) throws InterruptedException {
        semaphore.acquire();
        activeCounts.merge(agent.getType(), 1, Integer::sum);
        log.info("[AgentPool] acquire {}: active={}", agent.getType(), activeCounts);
    }

    public void release(SubAgent agent) {
        activeCounts.computeIfPresent(agent.getType(), (k, v) -> v > 0 ? v - 1 : 0);
        semaphore.release();
        log.info("[AgentPool] release {}: active={}", agent.getType(), activeCounts);
    }
}
