package com.tts.monitor.service;

import com.tts.monitor.entity.TtsProductMonitor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 告警服务测试类
 * 
 * @author TTS Monitor System
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
        TtsProductMonitor testProduct = new TtsProductMonitor();
        testProduct.setProductId("TEST_123456");
        testProduct.setTitle("测试商品");
        testProduct.setShopName("测试店铺");
        testProduct.setSaleRegion("ID");
        testProduct.setCommissionRate(100);
        testProduct.setLastCheckTime(java.time.LocalDateTime.now());

        // 发送测试告警
        alertService.executeAlert();
    }
}
