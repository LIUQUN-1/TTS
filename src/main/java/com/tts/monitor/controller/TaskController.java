package com.tts.monitor.controller;

import com.tts.monitor.dto.Result;
import com.tts.monitor.service.IAlertService;
import com.tts.monitor.service.IProductCheckService;
import com.tts.monitor.service.ProductCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 任务控制器
 * 
 */
@Slf4j
@RestController
@RequestMapping("/TTS/monitor/task")
@RequiredArgsConstructor
public class TaskController {

    private final IProductCheckService productCheckService;
    private final IAlertService alertService;

    /**
     * 手动触发商品校验任务
     * 
     * @return 执行结果
     */
    @PostMapping("/execute")
    public Result<Map<String, Object>> executeProductCheck() {
        log.info("手动触发商品校验任务");
        
        // 异步执行，避免接口超时
        CompletableFuture.runAsync(() -> {
            try {
                ProductCheckService.CheckResult result = productCheckService.executeProductCheck();
                log.info("手动校验任务完成 - 成功: {}, 总数: {}, 有效: {}, 失效: {}, 失败: {}, 耗时: {}ms",
                    result.isSuccess(), result.getTotalCount(), result.getValidCount(),
                    result.getInvalidCount(), result.getFailedCount(), result.getDuration());
                
                // 校验完成后执行告警检查
                if (result.isSuccess() && result.getInvalidCount() > 0) {
                    alertService.executeAlert();
                }
            } catch (Exception e) {
                log.error("手动校验任务执行异常", e);
            }
        });
        
        Map<String, Object> data = new HashMap<>();
        data.put("status", "started");
        data.put("message", "商品校验任务已启动，请查看日志了解执行进度");
        
        return Result.success("任务已提交", data);
    }
}
