package com.tts.monitor.service;

import com.tts.monitor.dto.PageResult;
import com.tts.monitor.dto.ProductQueryDTO;
import com.tts.monitor.entity.TtsProductMonitor;

import java.util.List;

/**
 * 商品监控服务接口
 * 
 */
public interface IProductService {

    /**
     * 分页查询商品列表
     * 
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<TtsProductMonitor> getProductList(ProductQueryDTO queryDTO);

    /**
     * 批量新增监控商品
     * 
     * @param productIds 商品ID列表
     * @return 新增数量
     */
    int addProducts(List<String> productIds);

    /**
     * 删除监控商品
     * 
     * @param productId 商品ID
     */
    void deleteProduct(String productId);

    /**
     * 确认商品失效
     * 
     * @param productId 商品ID
     */
    void confirmProduct(String productId);

    /**
     * 查询失效且未确认的商品列表（用于告警）
     * 
     * @return 失效且未确认的商品列表
     */
    List<TtsProductMonitor> getInvalidAndUnconfirmedProducts();

    /**
     * 统计总商品数量
     * 
     * @return 商品总数
     */
    Long countTotalProducts();
}
