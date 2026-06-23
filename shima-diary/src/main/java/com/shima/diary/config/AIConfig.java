package com.shima.diary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI配置类
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIConfig {
    
    /**
     * 是否启用AI功能
     */
    private boolean enabled = false;
    
    /**
     * AI提供商: openai, wenxin, qianwen, mock
     */
    private String provider = "mock";
    
    /**
     * OpenAI配置
     */
    private OpenAIConfig openai = new OpenAIConfig();
    
    /**
     * 文心一言配置
     */
    private WenxinConfig wenxin = new WenxinConfig();
    
    /**
     * 通义千问配置
     */
    private QianwenConfig qianwen = new QianwenConfig();
    
    /**
     * 模拟响应配置
     */
    private MockConfig mock = new MockConfig();
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public OpenAIConfig getOpenai() {
        return openai;
    }
    
    public void setOpenai(OpenAIConfig openai) {
        this.openai = openai;
    }
    
    public WenxinConfig getWenxin() {
        return wenxin;
    }
    
    public void setWenxin(WenxinConfig wenxin) {
        this.wenxin = wenxin;
    }
    
    public QianwenConfig getQianwen() {
        return qianwen;
    }
    
    public void setQianwen(QianwenConfig qianwen) {
        this.qianwen = qianwen;
    }
    
    public MockConfig getMock() {
        return mock;
    }
    
    public void setMock(MockConfig mock) {
        this.mock = mock;
    }
    
    /**
     * OpenAI配置
     */
    public static class OpenAIConfig {
        private String apiKey;
        private String model = "gpt-3.5-turbo";
        private String baseUrl = "https://api.openai.com/v1";
        private int maxTokens = 2000;
        private double temperature = 0.7;
        
        // Getters and Setters
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
        
        public int getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
        
        public double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
    
    /**
     * 文心一言配置
     */
    public static class WenxinConfig {
        private String apiKey;
        private String secretKey;
        private String model = "ernie-bot-turbo";
        private int maxTokens = 2000;
        private double temperature = 0.7;
        
        // Getters and Setters
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getSecretKey() {
            return secretKey;
        }
        
        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public int getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
        
        public double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
    
    /**
     * 通义千问配置
     */
    public static class QianwenConfig {
        private String apiKey;
        private String model = "qwen-turbo";
        private int maxTokens = 2000;
        private double temperature = 0.7;
        
        // Getters and Setters
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public int getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
        
        public double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
    
    /**
     * 模拟响应配置
     */
    public static class MockConfig {
        private int responseDelay = 500; // 模拟延迟（毫秒）
        
        // Getters and Setters
        public int getResponseDelay() {
            return responseDelay;
        }
        
        public void setResponseDelay(int responseDelay) {
            this.responseDelay = responseDelay;
        }
    }
}