package com.tts.monitor.dto;

import lombok.Data;

/**
 * 商品查询参数DTO
 * 
 */
@Data
public class ProductQueryDTO {

    /**
     * 页码，默认1
     */
    private Integer page = 1;

    /**
     * 每页条数，默认20
     */
    private Integer size = 20;

    /**
     * 状态筛选：1-有效，0-失效
     */
    private Integer isValid;

    /**
     * 销售国家筛选，如 IDR
     */
    private String saleRegion;

    /**
     * 搜索关键词（商品ID）
     */
    private String keyword;

    /**
     * 确认状态筛选：0-待处理，1-已确认
     */
    private Integer confirmStatus;

}
