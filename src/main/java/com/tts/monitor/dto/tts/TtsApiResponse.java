package com.tts.monitor.dto.tts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * TTS API 响应DTO
 * 
 */
@Data
public class TtsApiResponse {

    /**
     * 错误码。0 表示请求成功，非 0 表示请求失败
     */
    private Integer code;

    /**
     * 响应消息描述
     */
    private String message;

    /**
     * 响应数据
     */
    private DataWrapper data;

    /**
     * 请求唯一 ID，用于链路追踪和问题排查
     */
    @JsonProperty("request_id")
    private String requestId;

    @Data
    public static class DataWrapper {
        /**
         * 商品列表
         */
        private List<ProductInfo> products;
    }

    @Data
    public static class ProductInfo {
        /**
         * 商品 ID
         */
        private String id;

        /**
         * 商品标题
         */
        private String title;

        /**
         * 是否有库存
         */
        @JsonProperty("has_inventory")
        private Boolean hasInventory;

        /**
         * 销量
         */
        @JsonProperty("units_sold")
        private Integer unitsSold;

        /**
         * 销售地区代码
         */
        @JsonProperty("sale_region")
        private String saleRegion;

        /**
         * 商品主图链接
         */
        @JsonProperty("main_image_url")
        private String mainImageUrl;

        /**
         * 商品详情页链接
         */
        @JsonProperty("detail_link")
        private String detailLink;

        /**
         * 店铺信息
         */
        private ShopInfo shop;

        /**
         * 商品原价
         */
        @JsonProperty("original_price")
        private PriceInfo originalPrice;

        /**
         * 销售价格
         */
        @JsonProperty("sales_price")
        private PriceInfo salesPrice;

        /**
         * 佣金详情
         */
        private CommissionInfo commission;

        /**
         * 店铺广告佣金
         */
        @JsonProperty("shop_ads_commission")
        private ShopAdsCommission shopAdsCommission;

        /**
         * 类目链
         */
        @JsonProperty("category_chains")
        private List<CategoryChain> categoryChains;
    }

    @Data
    public static class ShopInfo {
        /**
         * 店铺名称
         */
        private String name;
    }

    @Data
    public static class PriceInfo {
        /**
         * 货币单位
         */
        private String currency;

        /**
         * 最小金额
         */
        @JsonProperty("minimum_amount")
        private String minimumAmount;

        /**
         * 最大金额
         */
        @JsonProperty("maximum_amount")
        private String maximumAmount;
    }

    @Data
    public static class CommissionInfo {
        /**
         * 佣金率（万分比，如 100 代表 1%）
         */
        private Integer rate;

        /**
         * 佣金货币单位
         */
        private String currency;

        /**
         * 预估佣金金额
         */
        private String amount;
    }

    @Data
    public static class ShopAdsCommission {
        /**
         * 广告佣金率（万分比）
         */
        private Integer rate;
    }

    @Data
    public static class CategoryChain {
        /**
         * 类目 ID
         */
        private String id;

        /**
         * 类目名称
         */
        @JsonProperty("local_name")
        private String localName;

        /**
         * 是否为叶子类目
         */
        @JsonProperty("is_leaf")
        private Boolean isLeaf;

        /**
         * 父类目 ID
         */
        @JsonProperty("parent_id")
        private String parentId;
    }

    /**
     * 判断请求是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 0;
    }
}
