package com.tts.monitor.service;

import com.tts.monitor.entity.TtsProductMonitor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 告警服务测试类
 * 
 * 
 * @since 2026-01-27
 */
@SpringBootTest
public class AlertServiceTest {

    @Autowired
    private AlertService alertService;

    /**
     * 测试发送告警消息
     * 用于手动测试飞书告警功能是否正常
     */
    @Test
    void testSendAlert() {
        // 发送测试告警
        alertService.executeAlert();
    }
}
