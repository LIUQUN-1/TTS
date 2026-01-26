package com.tts.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TTS商品监控实体类
 * 
 * @author TTS Monitor System
 * @since 2026-01-23
 */
@Data
@TableName("tts_product_monitor")
public class TtsProductMonitor implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * TTS 商品唯一 ID（唯一键）
     */
    @TableField("product_id")
    private String productId;

    /**
     * 商品标题（用于列表展示）
     */
    @TableField("title")
    private String title;

    /**
     * 店铺名称
     */
    @TableField("shop_name")
    private String shopName;

    /**
     * 产品销售的国家
     */
    @TableField("sale_region")
    private String saleRegion;

    /**
     * 是否有效：1-有效, 0-失效
     */
    @TableField("is_valid")
    private Integer isValid;

    /**
     * 确认状态：0-待处理, 1-运营已确认(跳过告警)
     */
    @TableField("confirm_status")
    private Integer confirmStatus;

    /**
     * 佣金率（万分比，如 100 代表 1%）
     */
    @TableField("commission_rate")
    private Integer commissionRate;

    /**
     * 预估佣金金额
     */
    @TableField("commission_amount")
    private BigDecimal commissionAmount;

    /**
     * 佣金货币单位 (如 USD, IDR)
     */
    @TableField("commission_currency")
    private String commissionCurrency;

    /**
     * 最后一次系统校验的时间
     */
    @TableField("last_check_time")
    private LocalDateTime lastCheckTime;

    /**
     * 记录创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 商品状态枚举
     */
    public static class ValidStatus {
        public static final int VALID = 1;      // 有效
        public static final int INVALID = 0;    // 失效
    }

    /**
     * 确认状态枚举
     */
    public static class ConfirmStatus {
        public static final int PENDING = 0;    // 待处理
        public static final int CONFIRMED = 1;  // 已确认
    }
}
