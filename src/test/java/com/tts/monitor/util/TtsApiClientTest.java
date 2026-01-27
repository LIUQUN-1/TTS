package com.tts.monitor.util;

import com.tts.monitor.dto.tts.TtsApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TtsApiClient 集成测试
 * 使用真实的 API 调用和配置
 *
 */
@SpringBootTest
public class TtsApiClientTest {

    @Autowired
    private TtsApiClient ttsApiClient;

    @Test
    void testGetProductsByIds_RealApiCall() throws Exception {
        // ...existing code...
        List<String> productIds = List.of("1730187224794695525", "1730280206432765372");

        // 调用真实 API
        TtsApiResponse result = ttsApiClient.getProductsByIds(productIds);
        // ...existing code...
        // 验证响应结构
        assertNotNull(result);
        assertNotNull(result.getCode());
        assertNotNull(result.getMessage());

        // 如果成功，验证数据
        if (result.isSuccess()) {
            assertNotNull(result.getData(), "响应数据不应为空"); // 增加校验信息
            assertNotNull(result.getRequestId());
            if (result.getData().getProducts() != null) {
                for (TtsApiResponse.ProductInfo product : result.getData().getProducts()) {
                    assertNotNull(product.getId());
                    assertNotNull(product.getTitle());
                    // 可以添加更多字段验证
                }
            }
        } else {
            // 如果失败，记录原因
            System.out.println("API 调用失败: " + result.getMessage());
        }
    }
}