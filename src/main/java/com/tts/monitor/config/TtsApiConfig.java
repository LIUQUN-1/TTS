package com.tts.monitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * TTS API 配置类
 *
 * @author TTS Monitor System
 * @since 2026-01-26
 */
@Configuration
public class TtsApiConfig {

    /**
     * 创建 HttpClient Bean
     */
    @Bean
    public HttpClient ttsHttpClient(TtsApiProperties ttsApiProperties) {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(ttsApiProperties.getTimeout()))
            .build();
    }
}