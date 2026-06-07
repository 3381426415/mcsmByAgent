package com.gitcode.mcsm_backend.agent.ai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.gitcode.mcsm_backend.agent.core.ChatMessage;

import java.util.List;
import java.util.Map;

/**
 * Token 估算器 — 使用 JTokkit cl100k_base 编码
 */
public class TokenEstimator {

    private static final Encoding ENCODING;
    private static final int OVERHEAD_PER_MESSAGE = 4; // role + formatting overhead
    private static final int OVERHEAD_PER_TOOL_CALL = 10;

    static {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        ENCODING = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    /**
     * 计算文本的 token 数
     */
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return ENCODING.encode(text, Integer.MAX_VALUE).getTokens().size();
    }

    /**
     * 估算单条消息的 token 数（含 content + tool_calls + overhead）
     */
    public int estimateMessage(ChatMessage msg) {
        int tokens = OVERHEAD_PER_MESSAGE;

        // content
        if (msg.getContent() != null) {
            tokens += countTokens(msg.getContent());
        }

        // tool_call_id
        if (msg.getToolCallId() != null) {
            tokens += countTokens(msg.getToolCallId());
        }

        // tool_calls (assistant 消息携带的)
        if (msg.getToolCalls() != null) {
            for (Map<String, Object> tc : msg.getToolCalls()) {
                tokens += OVERHEAD_PER_TOOL_CALL;
                Object func = tc.get("function");
                if (func instanceof Map) {
                    Map<String, Object> funcMap = (Map<String, Object>) func;
                    Object name = funcMap.get("name");
                    Object args = funcMap.get("arguments");
                    if (name != null) tokens += countTokens(name.toString());
                    if (args != null) tokens += countTokens(args.toString());
                }
            }
        }

        return tokens;
    }

    /**
     * 估算整段消息列表的总 token 数
     */
    public int estimateMessages(List<ChatMessage> messages) {
        int total = 0;
        for (ChatMessage msg : messages) {
            total += estimateMessage(msg);
        }
        return total;
    }
}
