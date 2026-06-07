package com.gitcode.mcsm_backend.agent.ai;

/**
 * LLM 厂商字段映射 — 从 llm-providers.yml 加载
 * 所有厂商共用 OpenAI 兼容格式，通过字段映射适配不同返回体结构
 */
public class ProviderFields {

    private final String thinkingField;       // 思考内容字段（如 reasoning_content、reasoning）
    private final String contentField;        // choices[0].message.{contentField}
    private final String toolCallsField;      // choices[0].message.{toolCallsField}
    private final String deltaContentField;   // delta.{deltaContentField}（流式）
    private final String deltaToolCallsField; // delta.{deltaToolCallsField}（流式）

    public ProviderFields(String thinkingField, String contentField, String toolCallsField,
                           String deltaContentField, String deltaToolCallsField) {
        this.thinkingField = thinkingField != null ? thinkingField : "reasoning_content";
        this.contentField = contentField != null ? contentField : "content";
        this.toolCallsField = toolCallsField != null ? toolCallsField : "tool_calls";
        this.deltaContentField = deltaContentField != null ? deltaContentField : "content";
        this.deltaToolCallsField = deltaToolCallsField != null ? deltaToolCallsField : "tool_calls";
    }

    public String getThinkingField() { return thinkingField; }
    public String getContentField() { return contentField; }
    public String getToolCallsField() { return toolCallsField; }
    public String getDeltaContentField() { return deltaContentField; }
    public String getDeltaToolCallsField() { return deltaToolCallsField; }
}
