package com.tts.monitor.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tts.monitor.config.TtsApiProperties;
import com.tts.monitor.dto.tts.TtsApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TTS API 客户端工具类
 * 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TtsApiClient {

    private final TtsApiProperties ttsApiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * API 路径常量
     */
    private static final String API_PATH = "/affiliate_creator/202509/open_collaborations/products";
    private static final String HEADER_ACCESS_TOKEN = "x-tts-access-token";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * 查询商品信息（批量）
     * 
     * @param productIds 商品ID列表
     * @return TTS API 响应
     */
    public TtsApiResponse getProductsByIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            log.warn("商品ID列表为空，跳过查询");
            return createEmptyResponse();
        }

        try {
            // 生成时间戳（秒级）
            long timestamp = System.currentTimeMillis() / 1000;

            // 构建查询参数
            Map<String, String> params = new HashMap<>();
            params.put("app_key", ttsApiProperties.getAppKey());
            params.put("timestamp", String.valueOf(timestamp));
            params.put("product_ids", String.join(",", productIds));

            // 生成签名
            String sign = TtsSignatureUtil.generateSign(
                params,
                ttsApiProperties.getAppSecret(),
                API_PATH
            );
            params.put("sign", sign);

            // 构建完整URL
            String queryString = TtsSignatureUtil.buildQueryString(params);
            String fullUrl = ttsApiProperties.getBaseUrl() + API_PATH + "?" + queryString;

            log.debug("发送 TTS API 请求 - URL: {}, 商品数量: {}", fullUrl, productIds.size());

            // 构建请求
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                .header(HEADER_ACCESS_TOKEN, ttsApiProperties.getAccessToken())
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofMillis(ttsApiProperties.getTimeout()))
                .build();

            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 记录响应
            log.debug("TTS API 响应 - 状态码: {}, 响应体: {}", response.statusCode(), response.body());

            // 解析响应
            if (response.statusCode() == 200) {
                TtsApiResponse apiResponse = objectMapper.readValue(response.body(), TtsApiResponse.class);
                if (apiResponse.isSuccess()) {
                    log.info("TTS API 查询成功 - 请求商品数: {}, 返回商品数: {}", 
                        productIds.size(), 
                        apiResponse.getData() != null && apiResponse.getData().getProducts() != null 
                            ? apiResponse.getData().getProducts().size() : 0);
                } else {
                    log.warn("TTS API 返回错误 - code: {}, message: {}", 
                        apiResponse.getCode(), apiResponse.getMessage());
                }
                return apiResponse;
            } else {
                log.error("TTS API 请求失败 - HTTP状态码: {}, 响应: {}", response.statusCode(), response.body());
                return createErrorResponse("HTTP请求失败: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("调用 TTS API 异常 - 商品ID: {}", productIds, e);
            return createErrorResponse("调用API异常: " + e.getMessage());
        }
    }

    /**
     * 创建空响应
     */
    private TtsApiResponse createEmptyResponse() {
        TtsApiResponse response = new TtsApiResponse();
        response.setCode(0);
        response.setMessage("Success");
        TtsApiResponse.DataWrapper dataWrapper = new TtsApiResponse.DataWrapper();
        dataWrapper.setProducts(List.of());
        response.setData(dataWrapper);
        return response;
    }

    /**
     * 创建错误响应
     */
    private TtsApiResponse createErrorResponse(String message) {
        TtsApiResponse response = new TtsApiResponse();
        response.setCode(-1);
        response.setMessage(message);
        return response;
    }

}
