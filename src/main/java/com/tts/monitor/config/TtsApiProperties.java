package com.tts.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TTS API 配置属性
 * 
 */
@Data
@Component
@ConfigurationProperties(prefix = "tts.api")
public class TtsApiProperties {

    /**
     * TTS API 基础URL
     */
    private String baseUrl;

    /**
     * 应用唯一 Key
     */
    private String appKey;

    /**
     * 应用密钥
     */
    private String appSecret;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 接口版本
     */
    private String version;

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout;

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit;

    /**
     * 批量查询配置
     */
    private BatchConfig batch;

    /**
     * 线程池配置
     */
    private ThreadPoolConfig threadPool;

    @Data
    public static class RateLimitConfig {
        /**
         * 每秒最大请求数（QPS）
         */
        private Integer qps;
    }

    @Data
    public static class BatchConfig {
        /**
         * 每批次商品ID数量
         */
        private Integer size;

        /**
         * 分页查询每页大小
         */
        private Integer pageSize;
    }

    @Data
    public static class ThreadPoolConfig {
        /**
         * 核心线程数
         */
        private Integer coreSize;

        /**
         * 最大线程数
         */
        private Integer maxSize;

        /**
         * 队列容量
         */
        private Integer queueCapacity;

        /**
         * 线程名称前缀
         */
        private String threadNamePrefix;
    }
}
