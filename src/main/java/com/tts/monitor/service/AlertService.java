package com.tts.monitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tts.monitor.config.AlertProperties;
import com.tts.monitor.entity.TtsProductMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警服务实现类
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService implements IAlertService {

    private final AlertProperties alertProperties;
    private final IProductService productService;
    private final ObjectMapper objectMapper;

    // 告警专用日志标记
    private static final Marker ALERT_MARKER = MarkerFactory.getMarker("ALERT");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 执行告警检查并发送
     */
    public void executeAlert() {
        if (!alertProperties.getFeishu().getEnabled()) {
            log.info("告警功能已禁用，跳过告警");
            return;
        }

        log.info("========== 开始执行告警检查 ==========");

        try {
            // 查询失效且未确认的商品
            List<TtsProductMonitor> products = productService.getInvalidAndUnconfirmedProducts();

            if (products.isEmpty()) {
                log.info("没有需要告警的商品（失效且未确认）");
                return;
            }

            log.info("发现 {} 个失效且未确认的商品，准备发送告警", products.size());

            // 发送告警
            for (TtsProductMonitor product : products) {
                sendAlert(product);
            }

            log.info("========== 告警检查完成，共发送 {} 条告警 ==========", products.size());

        } catch (Exception e) {
            log.error("执行告警检查失败", e);
        }
    }

    /**
     * 发送单个商品告警
     */
    private void sendAlert(TtsProductMonitor product) {
        try {
            String message = buildAlertMessage(product);
            
            // 记录告警日志（会自动写入 tts-alert.log）
            log.info(ALERT_MARKER, "告警发送：商品ID={}, 标题={}, 店铺={}", 
                product.getProductId(), product.getTitle(), product.getShopName());

            // 发送飞书消息
            boolean success = sendFeishuMessage(message);

            if (success) {
                log.info(ALERT_MARKER, "告警发送成功 - 商品ID: {}", product.getProductId());
            } else {
                log.error(ALERT_MARKER, "告警发送失败 - 商品ID: {}", product.getProductId());
            }

        } catch (Exception e) {
            log.error(ALERT_MARKER, "发送告警异常 - 商品ID: {}", product.getProductId(), e);
        }
    }

    /**
     * 构建告警消息
     */
    private String buildAlertMessage(TtsProductMonitor product) {
        String template = alertProperties.getMessage().getContentTemplate();
        
        // 格式化佣金信息
        String commission = "无";

        // 格式化最后校验时间
        String lastCheckTime = product.getLastCheckTime() != null 
            ? product.getLastCheckTime().format(DATE_FORMATTER) 
            : "未知";

        // 替换模板变量
        return template
            .replace("{productId}", product.getProductId() != null ? product.getProductId() : "")
            .replace("{title}", product.getTitle() != null ? product.getTitle() : "无标题")
            .replace("{shopName}", product.getShopName() != null ? product.getShopName() : "无店铺")
            .replace("{saleRegion}", product.getSaleRegion() != null ? product.getSaleRegion() : "无")
            .replace("{commission}", commission)
            .replace("{lastCheckTime}", lastCheckTime);
    }

    /**
     * 发送飞书消息
     */
    private boolean sendFeishuMessage(String content) {
        try {
            String webhookUrl = alertProperties.getFeishu().getWebhookUrl();
            
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                log.warn("飞书 Webhook URL 未配置");
                return false;
            }

            // 构建飞书消息体
            Map<String, Object> message = new HashMap<>();
            message.put("msg_type", "text");
            
            Map<String, String> contentMap = new HashMap<>();
            contentMap.put("text", alertProperties.getMessage().getTitle() + "\n" + content);
            message.put("content", contentMap);

            String jsonBody = objectMapper.writeValueAsString(message);

            // 创建 HttpClient
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            // 构建请求
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(10))
                .build();

            // 发送请求
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("飞书消息发送响应 - 状态码: {}, 响应: {}", response.statusCode(), response.body());

            return response.statusCode() == 200;

        } catch (Exception e) {
            log.error("发送飞书消息失败", e);
            return false;
        }
    }
}
