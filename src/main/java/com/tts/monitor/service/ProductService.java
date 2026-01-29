package com.tts.monitor.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tts.monitor.dto.PageResult;
import com.tts.monitor.dto.ProductQueryDTO;
import com.tts.monitor.dto.tts.TtsApiResponse;
import com.tts.monitor.entity.TtsProductMonitor;
import com.tts.monitor.exception.BusinessException;
import com.tts.monitor.mapper.TtsProductMonitorMapper;
import com.tts.monitor.util.TtsApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品监控服务实现类
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final TtsProductMonitorMapper productMapper;
    private final TtsApiClient ttsApiClient;

    /**
     * 分页查询商品列表
     * 
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    public PageResult<TtsProductMonitor> getProductList(ProductQueryDTO queryDTO) {
        log.debug("查询商品列表 - 参数: {}", queryDTO);

        // 构建查询条件
        LambdaQueryWrapper<TtsProductMonitor> wrapper = Wrappers.lambdaQuery();
        
        // 按状态筛选
        if (queryDTO.getIsValid() != null) {
            wrapper.eq(TtsProductMonitor::getIsValid, queryDTO.getIsValid());
        }
        
        // 按地区筛选
        if (StringUtils.hasText(queryDTO.getSaleRegion())) {
            wrapper.eq(TtsProductMonitor::getSaleRegion, queryDTO.getSaleRegion());
        }
        
        // 按确认状态筛选
        if (queryDTO.getConfirmStatus() != null) {
            wrapper.eq(TtsProductMonitor::getConfirmStatus, queryDTO.getConfirmStatus());
        }
        
        // 按商品ID模糊查询
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.like(TtsProductMonitor::getProductId, queryDTO.getKeyword());
        }
        
        // 按更新时间倒序
        wrapper.orderByDesc(TtsProductMonitor::getUpdatedAt);
        
        // 分页查询
        Page<TtsProductMonitor> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        IPage<TtsProductMonitor> resultPage = productMapper.selectPage(page, wrapper);
        
        log.info("查询商品列表成功 - 总数: {}, 当前页: {}", resultPage.getTotal(), queryDTO.getPage());
        return new PageResult<>(queryDTO.getPage(), queryDTO.getSize(), resultPage.getTotal(), resultPage.getRecords());
    }

    /**
     * 批量新增监控商品
     * 
     * @param productIds 商品ID列表
     * @return 新增数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int addProducts(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new BusinessException("商品ID列表不能为空");
        }

        log.info("批量新增商品 - 数量: {}", productIds.size());

        // 去重
        List<String> uniqueProductIds = productIds.stream()
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());

        if (uniqueProductIds.isEmpty()) {
            throw new BusinessException("有效的商品ID列表为空");
        }

        log.debug("去重后商品ID数量: {}", uniqueProductIds.size());

        // 查询已存在的商品
        List<TtsProductMonitor> existingProducts = productMapper.selectByProductIds(uniqueProductIds);
        Set<String> existingProductIds = existingProducts.stream()
            .map(TtsProductMonitor::getProductId)
            .collect(Collectors.toSet());

        // 过滤出新商品ID
        List<String> newProductIds = uniqueProductIds.stream()
            .filter(id -> !existingProductIds.contains(id))
            .collect(Collectors.toList());

        if (newProductIds.isEmpty()) {
            log.warn("所有商品ID已存在，无需新增");
            return 0;
        }

        log.info("需要新增的商品数量: {}, 已存在的商品数量: {}", newProductIds.size(), existingProductIds.size());

        // 调用 TTS API 验证并获取商品信息
        TtsApiResponse apiResponse = ttsApiClient.getProductsByIds(newProductIds);

        if (!apiResponse.isSuccess()) {
            throw new BusinessException("调用 TTS API 失败: " + apiResponse.getMessage());
        }

        if (apiResponse.getData() == null || apiResponse.getData().getProducts() == null) {
            throw new BusinessException("TTS API 返回数据为空");
        }

        List<TtsApiResponse.ProductInfo> products = apiResponse.getData().getProducts();

        if (products.isEmpty()) {
            throw new BusinessException("商品ID无效，未查询到任何商品信息");
        }

        log.info("TTS API 返回商品数量: {}", products.size());

        // 转换为实体类并批量插入
        List<TtsProductMonitor> monitorList = products.stream()
            .map(this::convertToEntity)
            .collect(Collectors.toList());

        int insertCount = productMapper.batchInsertIgnore(monitorList);
        log.info("批量新增商品完成 - 成功: {}", insertCount);

        return insertCount;
    }

    /**
     * 删除监控商品
     * 
     * @param productId 商品ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(String productId) {
        if (!StringUtils.hasText(productId)) {
            throw new BusinessException("商品ID不能为空");
        }

        log.info("删除商品 - ID: {}", productId);

        // 查询商品
        TtsProductMonitor product = productMapper.selectOne(
            Wrappers.lambdaQuery(TtsProductMonitor.class)
                .eq(TtsProductMonitor::getProductId, productId)
        );

        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 校验：失效且未确认的商品不允许删除
        if (product.getIsValid() == TtsProductMonitor.ValidStatus.INVALID 
            && product.getConfirmStatus() == TtsProductMonitor.ConfirmStatus.PENDING) {
            throw new BusinessException("失效且未确认的商品不允许删除，请先确认后再删除");
        }

        // 删除商品
        int deleteCount = productMapper.deleteById(product.getId());

        if (deleteCount > 0) {
            log.info("删除商品成功 - ID: {}", productId);
        } else {
            throw new BusinessException("删除商品失败");
        }
    }

    /**
     * 确认商品失效
     * 
     * @param productId 商品ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmProduct(String productId) {
        if (!StringUtils.hasText(productId)) {
            throw new BusinessException("商品ID不能为空");
        }

        log.info("确认商品失效 - ID: {}", productId);

        // 查询商品
        TtsProductMonitor product = productMapper.selectOne(
            Wrappers.lambdaQuery(TtsProductMonitor.class)
                .eq(TtsProductMonitor::getProductId, productId)
        );

        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 校验：有效商品不允许确认
        if (product.getIsValid() == TtsProductMonitor.ValidStatus.VALID) {
            throw new BusinessException("有效商品无需确认");
        }

        // 已确认的商品无需重复确认
        if (product.getConfirmStatus() == TtsProductMonitor.ConfirmStatus.CONFIRMED) {
            log.warn("商品已确认，无需重复操作 - ID: {}", productId);
            return;
        }

        // 更新确认状态
        product.setConfirmStatus(TtsProductMonitor.ConfirmStatus.CONFIRMED);
        int updateCount = productMapper.updateById(product);

        if (updateCount > 0) {
            log.info("确认商品失效成功 - ID: {}, 操作: 确认失效", productId);
        } else {
            throw new BusinessException("确认商品失效失败");
        }
    }

    /**
     * 将 TTS API 响应转换为实体类
     */
    private TtsProductMonitor convertToEntity(TtsApiResponse.ProductInfo productInfo) {
        TtsProductMonitor monitor = new TtsProductMonitor();
        monitor.setProductId(productInfo.getId());
        monitor.setTitle(productInfo.getTitle());
        monitor.setSaleRegion(productInfo.getSaleRegion());

        // 店铺名称
        if (productInfo.getShop() != null) {
            monitor.setShopName(productInfo.getShop().getName());
        }

        // 佣金信息
        if (productInfo.getCommission() != null 
            && productInfo.getCommission().getRate() != null
            && StringUtils.hasText(productInfo.getCommission().getCurrency()) 
            && StringUtils.hasText(productInfo.getCommission().getAmount())) {
            monitor.setIsValid(TtsProductMonitor.ValidStatus.VALID);
            monitor.setCommissionRate(productInfo.getCommission().getRate());
            monitor.setCommissionCurrency(productInfo.getCommission().getCurrency());
            monitor.setCommissionAmount(productInfo.getCommission().getAmount());
        } else {
            // 佣金信息不完整或佣金率为0，标记为失效
            monitor.setIsValid(TtsProductMonitor.ValidStatus.INVALID);
        }

        monitor.setConfirmStatus(TtsProductMonitor.ConfirmStatus.PENDING);
        monitor.setLastCheckTime(LocalDateTime.now());

        return monitor;
    }

    /**
     * 查询失效且未确认的商品列表（用于告警）
     */
    public List<TtsProductMonitor> getInvalidAndUnconfirmedProducts() {
        return productMapper.selectInvalidAndUnconfirmedProducts();
    }

    /**
     * 统计总商品数量
     */
    public Long countTotalProducts() {
        return productMapper.selectCount(null);
    }
}
