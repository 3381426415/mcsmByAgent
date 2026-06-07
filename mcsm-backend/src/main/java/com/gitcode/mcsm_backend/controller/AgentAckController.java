package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.agent.communication.SessionEventBuffer;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.AgentChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent ACK 确认 + 断线恢复 API
 */
@RestController
@RequestMapping("/api/agent")
public class AgentAckController {

    @Autowired
    private AgentChatService agentChatService;

    /**
     * 前端确认收到事件
     */
    @PostMapping("/ack")
    public Result<Void> ack(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("session_id");
        Number seq = (Number) body.get("seq");
        if (sessionId == null || seq == null) {
            return Result.error("参数缺失");
        }
        SessionEventBuffer buffer = agentChatService.getEventBuffer();
        if (buffer != null) {
            buffer.ack(sessionId, seq.intValue());
        }
        return Result.success("ok", null);
    }

    /**
     * 断线恢复：获取 lastSeq 之后的缓存事件
     */
    @GetMapping("/replay")
    public Result<List<SessionEventBuffer.BufferedEvent>> replay(
            @RequestParam("session_id") String sessionId,
            @RequestParam("last_seq") int lastSeq) {
        SessionEventBuffer buffer = agentChatService.getEventBuffer();
        if (buffer == null) {
            return Result.error("Agent 服务未初始化");
        }
        List<SessionEventBuffer.BufferedEvent> events = buffer.getSince(sessionId, lastSeq);
        return Result.success("ok", events);
    }

    /**
     * 超时兜底：获取最终结果
     */
    @GetMapping("/result")
    public Result<Map> result(@RequestParam("session_id") String sessionId) {
        SessionEventBuffer buffer = agentChatService.getEventBuffer();
        if (buffer == null) {
            return Result.error("Agent 服务未初始化");
        }
        int lastSeq = buffer.getLastSeq(sessionId);
        int lastAcked = buffer.getLastAckedSeq(sessionId);
        List<SessionEventBuffer.BufferedEvent> pending = buffer.getSince(sessionId, lastAcked);

        // 找最后一个 REPLY_DONE 或 ERROR 事件
        String finalResult = null;
        String error = null;
        for (SessionEventBuffer.BufferedEvent e : pending) {
            if ("REPLY_DONE".equals(e.type())) {
                Object data = e.data();
                if (data instanceof Map) {
                    Object content = ((Map<?, ?>) data).get("content");
                    finalResult = content != null ? content.toString() : null;
                }
            } else if ("AGENT_ERROR".equals(e.type()) || "ERROR".equals(e.type())) {
                Object data = e.data();
                if (data instanceof Map) {
                    Object err = ((Map<?, ?>) data).get("error");
                    if (err == null) err = ((Map<?, ?>) data).get("message");
                    error = err != null ? err.toString() : null;
                }
            }
        }

        boolean hasResult = finalResult != null || error != null;
        return Result.success("ok", Map.of(
                "lastSeq", lastSeq,
                "lastAcked", lastAcked,
                "hasResult", hasResult,
                "reply", finalResult != null ? finalResult : "",
                "error", error != null ? error : ""
        ));
    }
}
