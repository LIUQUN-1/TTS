package com.tts.monitor.task;

import com.tts.monitor.service.AlertService;
import com.tts.monitor.service.ProductCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商品校验定时任务
 * 
 * @author TTS Monitor System
 * @since 2026-01-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "schedule.product-check", name = "enabled", havingValue = "true")
public class ProductCheckTask {

    private final ProductCheckService productCheckService;
    private final AlertService alertService;

    /**
     * 每日定时校验任务
     * 默认每天凌晨2点执行
     */
    @Scheduled(cron = "${schedule.product-check.cron:0 0 2 * * ?}")
    public void scheduledProductCheck() {
        log.info("触发定时商品校验任务");
        
        try {
            // 执行商品校验
            ProductCheckService.CheckResult result = productCheckService.executeProductCheck();
            
            if (result.isSuccess()) {
                log.info("定时商品校验任务完成 - 总数: {}, 有效: {}, 失效: {}, 耗时: {}ms",
                    result.getTotalCount(), result.getValidCount(), 
                    result.getInvalidCount(), result.getDuration());
                
                // 校验完成后执行告警
                alertService.executeAlert();
            } else {
                log.error("定时商品校验任务失败 - 错误: {}", result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("定时商品校验任务执行异常", e);
        }
    }
}
