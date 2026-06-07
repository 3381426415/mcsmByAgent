package com.gitcode.mcsm_backend.agent.communication;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 询问队列 - 多个子任务同时需要询问用户时，排队处理
 */
@Slf4j
public class InteractionQueue {

    private final PriorityBlockingQueue<InteractionEvent> queue = new PriorityBlockingQueue<>(10,
            (a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

    private final AtomicReference<InteractionEvent> currentQuestion = new AtomicReference<>();
    private Consumer<InteractionEvent> onUserReplyCallback;

    public void setOnUserReplyCallback(Consumer<InteractionEvent> callback) {
        this.onUserReplyCallback = callback;
    }

    public void submit(InteractionEvent event) {
        queue.offer(event);
        log.info("[InteractionQueue] 提交询问: agentId={}, priority={}", event.getAgentId(), event.getPriority());
    }

    public InteractionEvent getNext() {
        InteractionEvent next = queue.poll();
        currentQuestion.set(next);
        return next;
    }

    public InteractionEvent getCurrentQuestion() {
        return currentQuestion.get();
    }

    public boolean hasPending() {
        return !queue.isEmpty();
    }

    public void onUserReply(String reply) {
        InteractionEvent current = currentQuestion.getAndSet(null);
        if (current != null && onUserReplyCallback != null) {
            current.setReply(reply);
            onUserReplyCallback.accept(current);
        }
    }


    public static class InteractionEvent {
        private final String agentId;
        private final String question;
        private final InteractionType type;
        private final int priority;  // higher = more urgent
        private String reply;

        public InteractionEvent(String agentId, String question, InteractionType type, int priority) {
            this.agentId = agentId;
            this.question = question;
            this.type = type;
            this.priority = priority;
        }

        public String getAgentId() { return agentId; }
        public String getQuestion() { return question; }
        public InteractionType getType() { return type; }
        public int getPriority() { return priority; }
        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
    }

    public enum InteractionType {
        SAFETY_CONFIRM,    // 危险操作确认 (priority: 100)
        USER_CONFIRM,      // 用户确认 (priority: 50)
        CLARIFICATION,     // 澄清 (priority: 10)
        CHOICE             // 选择 (priority: 30)
    }
}
