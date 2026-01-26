package com.tts.monitor.dto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 新增商品请求DTO
 * 
 * @author TTS Monitor System
 * @since 2026-01-23
 */
@Data
public class ProductAddRequest {

    /**
     * 商品ID列表，批量新增的TTS商品唯一ID
     */
    @NotEmpty(message = "商品ID列表不能为空")
    private List<String> productIds;
}
