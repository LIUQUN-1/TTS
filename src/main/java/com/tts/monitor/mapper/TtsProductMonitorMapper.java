package com.tts.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tts.monitor.entity.TtsProductMonitor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TTS商品监控 Mapper 接口
 * 
 * @author TTS Monitor System
 * @since 2026-01-23
 */
@Mapper
public interface TtsProductMonitorMapper extends BaseMapper<TtsProductMonitor> {

    /**
     * 批量查询商品ID列表（用于分页校验）
     * 
     * @param offset 偏移量
     * @param limit 每页数量
     * @return 商品ID列表
     */
    List<String> selectProductIdsByPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 批量更新商品有效性状态和最后校验时间
     * 
     * @param productIds 商品ID列表
     * @param isValid 有效性状态
     * @param lastCheckTime 最后校验时间
     * @return 影响行数
     */
    int batchUpdateValidStatus(@Param("productIds") List<String> productIds, 
                                @Param("isValid") Integer isValid,
                                @Param("lastCheckTime") LocalDateTime lastCheckTime);

    /**
     * 查询失效且未确认的商品列表（用于告警）
     * 
     * @return 失效且未确认的商品列表
     */
    List<TtsProductMonitor> selectInvalidAndUnconfirmedProducts();

    /**
     * 统计总商品数量
     * 
     * @return 总数量
     */
    Long countTotalProducts();

    /**
     * 根据商品ID批量查询（用于校验是否已存在）
     * 
     * @param productIds 商品ID列表
     * @return 已存在的商品列表
     */
    List<TtsProductMonitor> selectByProductIds(@Param("productIds") List<String> productIds);

    /**
     * 批量插入商品（忽略重复）
     * 
     * @param products 商品列表
     * @return 插入数量
     */
    int batchInsertIgnore(@Param("products") List<TtsProductMonitor> products);
}
