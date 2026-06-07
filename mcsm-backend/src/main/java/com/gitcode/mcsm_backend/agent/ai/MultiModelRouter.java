package com.gitcode.mcsm_backend.agent.ai;

/**
 * 多模型路由器 — 根据 Agent 类型选择 Pro/Flash 模型
 */
public class MultiModelRouter {

    private final String baseUrl;
    private final String apiKey;
    private final String proModel;
    private final String flashModel;
    private final String provider;
    private final ProviderFields providerFields;

    public MultiModelRouter(String baseUrl, String apiKey, String proModel, String flashModel) {
        this(baseUrl, apiKey, proModel, flashModel, "openai", (ProviderFields) null);
    }

    public MultiModelRouter(String baseUrl, String apiKey, String proModel, String flashModel,
                            String provider, String thinkingField) {
        this(baseUrl, apiKey, proModel, flashModel, provider,
             new ProviderFields(thinkingField, null, null, null, null));
    }

    public MultiModelRouter(String baseUrl, String apiKey, String proModel, String flashModel,
                            String provider, ProviderFields providerFields) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.proModel = proModel;
        this.flashModel = flashModel;
        this.provider = provider;
        this.providerFields = providerFields;
    }

    /**
     * 根据模型级别选择模型 ID
     * @param tier "PRO" 或 "FLASH"
     */
    public String selectModel(String tier) {
        if ("PRO".equalsIgnoreCase(tier)) {
            return proModel;
        }
        return flashModel;
    }

    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getProModel() { return proModel; }
    public String getFlashModel() { return flashModel; }
    public String getProvider() { return provider; }
    public String getThinkingField() { return providerFields.getThinkingField(); }
    public ProviderFields getProviderFields() { return providerFields; }
}
