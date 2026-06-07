package com.gitcode.mcsm_backend.agent.core;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Agent 执行器 - 使用 Java 21 虚拟线程并行执行子 Agent
 */
@Slf4j
public class AgentExecutor {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final AgentPool agentPool;

    public AgentExecutor(AgentPool agentPool) {
        this.agentPool = agentPool;
    }

    /**
     * 并行执行一组无依赖的子 Agent
     */
    public List<AgentResult> executeParallel(List<SubAgent> agents) {
        List<Future<AgentResult>> futures = new ArrayList<>();

        for (SubAgent agent : agents) {
            try {
                agentPool.acquire(agent);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
            futures.add(executor.submit(() -> {
                try {
                    return agent.execute();
                } finally {
                    agentPool.release(agent);
                }
            }));
        }

        List<AgentResult> results = new ArrayList<>();
        for (Future<AgentResult> future : futures) {
            try {
                results.add(future.get(5, TimeUnit.MINUTES));
            } catch (TimeoutException e) {
                results.add(AgentResult.error(new RuntimeException("Agent 执行超时")));
            } catch (Exception e) {
                results.add(AgentResult.error(e));
            }
        }
        return results;
    }

    /**
     * 有依赖关系的子任务：拓扑排序后，层内并行、层间串行
     */
    public List<AgentResult> executeWithDependencies(List<AgentTask> tasks) {
        List<List<AgentTask>> layers = topologicalSort(tasks);
        List<AgentResult> allResults = new ArrayList<>();

        for (List<AgentTask> layer : layers) {
            log.info("[AgentExecutor] 执行层 {}: {} 个任务", layers.indexOf(layer), layer.size());
            List<SubAgent> agents = layer.stream()
                    .map(AgentTask::getAgent)
                    .toList();
            List<AgentResult> layerResults = executeParallel(agents);
            allResults.addAll(layerResults);
        }

        return allResults;
    }

    private List<List<AgentTask>> topologicalSort(List<AgentTask> tasks) {
        Map<String, AgentTask> taskMap = new java.util.HashMap<>();
        Map<String, Integer> inDegree = new java.util.HashMap<>();
        Map<String, List<String>> adjacency = new java.util.HashMap<>();

        for (AgentTask task : tasks) {
            taskMap.put(task.getId(), task);
            inDegree.put(task.getId(), 0);
            adjacency.put(task.getId(), new ArrayList<>());
        }

        for (AgentTask task : tasks) {
            for (String dep : task.getDependsOn()) {
                if (adjacency.containsKey(dep)) {
                    adjacency.get(dep).add(task.getId());
                    inDegree.merge(task.getId(), 1, Integer::sum);
                }
            }
        }

        java.util.Queue<String> queue = new java.util.LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }

        List<List<AgentTask>> layers = new ArrayList<>();
        while (!queue.isEmpty()) {
            int size = queue.size();
            List<AgentTask> layer = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String id = queue.poll();
                layer.add(taskMap.get(id));
                for (String next : adjacency.get(id)) {
                    inDegree.merge(next, -1, Integer::sum);
                    if (inDegree.get(next) == 0) queue.add(next);
                }
            }
            layers.add(layer);
        }

        return layers;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
