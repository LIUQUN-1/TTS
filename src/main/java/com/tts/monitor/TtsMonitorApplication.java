package com.tts.monitor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TTS Product Monitor 应用启动类
 * 
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.tts.monitor.mapper")
public class TtsMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TtsMonitorApplication.class, args);
    }
}
