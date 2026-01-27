package com.tts.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 告警配置属性
 * 
 */
@Data
@Component
@ConfigurationProperties(prefix = "alert")
public class AlertProperties {

    /**
     * 飞书配置
     */
    private FeishuConfig feishu;

    /**
     * 消息配置
     */
    private MessageConfig message;

    @Data
    public static class FeishuConfig {
        /**
         * Webhook地址
         */
        private String webhookUrl;

        /**
         * 是否启用告警
         */
        private Boolean enabled;
    }

    @Data
    public static class MessageConfig {
        /**
         * 消息标题
         */
        private String title;

        /**
         * 消息内容模板
         */
        private String contentTemplate;
    }
}
