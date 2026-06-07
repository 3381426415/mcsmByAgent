package com.gitcode.mcsm_backend.agent.memory;

import com.gitcode.mcsm_backend.agent.core.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文压缩器 - 结构化压缩，不靠模型自由发挥
 */
public class ContextCompressor {

    public StructuredSummary compress(List<ChatMessage> history) {
        StructuredSummary summary = new StructuredSummary();

        List<String> decisions = new ArrayList<>();
        List<String> userDirectives = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (ChatMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                userDirectives.add(msg.getContent());
            } else if ("assistant".equals(msg.getRole())) {
                String content = msg.getContent();
                if (content.contains("错误") || content.contains("失败")) {
                    errors.add(content);
                } else {
                    decisions.add(content);
                }
            }
        }

        summary.setDecisions(decisions);
        summary.setUserDirectives(userDirectives);
        summary.setErrors(errors);
        summary.setTotalMessages(history.size());

        return summary;
    }

    public static class StructuredSummary {
        private List<String> decisions;
        private List<String> userDirectives;
        private List<String> errors;
        private int totalMessages;

        public List<String> getDecisions() { return decisions; }
        public void setDecisions(List<String> decisions) { this.decisions = decisions; }
        public List<String> getUserDirectives() { return userDirectives; }
        public void setUserDirectives(List<String> userDirectives) { this.userDirectives = userDirectives; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public int getTotalMessages() { return totalMessages; }
        public void setTotalMessages(int totalMessages) { this.totalMessages = totalMessages; }
    }
}
