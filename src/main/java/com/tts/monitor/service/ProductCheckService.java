package com.tts.monitor.service;

import com.google.common.util.concurrent.RateLimiter;
import com.tts.monitor.config.TtsApiProperties;
import com.tts.monitor.dto.tts.TtsApiResponse;
import com.tts.monitor.entity.TtsProductMonitor;
import com.tts.monitor.mapper.TtsProductMonitorMapper;
import com.tts.monitor.util.TtsApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 商品校验服务
 * 
 */
@Slf4j
@Service
public class ProductCheckService {

    private final TtsProductMonitorMapper productMapper;
    private final TtsApiClient ttsApiClient;
    private final TtsApiProperties ttsApiProperties;
    private final ThreadPoolExecutor productCheckExecutor;
    
    // 令牌桶限流器（50 QPS）
    private final RateLimiter rateLimiter;

    public ProductCheckService(
            TtsProductMonitorMapper productMapper,
            TtsApiClient ttsApiClient,
            TtsApiProperties ttsApiProperties,
            @Qualifier("productCheckExecutor") ThreadPoolExecutor productCheckExecutor) {
        this.productMapper = productMapper;
        this.ttsApiClient = ttsApiClient;
        this.ttsApiProperties = ttsApiProperties;
        this.productCheckExecutor = productCheckExecutor;
        
        // 初始化令牌桶，每秒固定产生50个令牌
        int qps = ttsApiProperties.getRateLimit().getQps();
        this.rateLimiter = RateLimiter.create(qps);
        log.info("初始化令牌桶限流器 - QPS: {}", qps);
    }

    /**
     * 执行全量商品校验
     * 
     * @return 校验结果统计
     */
    public CheckResult executeProductCheck() {
        log.info("========== 开始执行商品校验任务 ==========");
        long startTime = System.currentTimeMillis();
        
        CheckResult result = new CheckResult();
        
        try {
            // 统计总商品数
            Long totalCount = productMapper.countTotalProducts();
            log.info("商品总数: {}", totalCount);
            
            if (totalCount == 0) {
                log.warn("没有需要校验的商品");
                return result;
            }
            
            result.setTotalCount(totalCount.intValue());
            
            // 分页参数
            int pageSize = ttsApiProperties.getBatch().getPageSize();
            int batchSize = ttsApiProperties.getBatch().getSize();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);
            
            log.info("分页参数 - 每页: {}, 批次大小: {}, 总页数: {}", pageSize, batchSize, totalPages);
            
            // 分页加载并处理
            for (int page = 0; page < totalPages; page++) {
                int offset = page * pageSize;
                log.info("处理第 {}/{} 页 - offset: {}, limit: {}", page + 1, totalPages, offset, pageSize);
                
                // 查询当前页的商品ID列表
                List<String> productIds = productMapper.selectProductIdsByPage(offset, pageSize);
                
                if (productIds.isEmpty()) {
                    log.warn("第 {} 页没有查询到商品", page + 1);
                    continue;
                }
                
                log.debug("第 {} 页查询到 {} 个商品ID", page + 1, productIds.size());
                
                // 处理当前页的商品
                CheckResult pageResult = processProductBatch(productIds, batchSize);
                result.merge(pageResult);
                
                log.info("第 {}/{} 页处理完成 - 已处理: {}, 有效: {}, 失效: {}", 
                    page + 1, totalPages, pageResult.getCheckedCount(), 
                    pageResult.getValidCount(), pageResult.getInvalidCount());
            }
            
            long duration = System.currentTimeMillis() - startTime;
            result.setDuration(duration);
            
            log.info("========== 商品校验任务完成 ==========");
            log.info("总计 - 商品总数: {}, 已校验: {}, 有效: {}, 失效: {}, 失败: {}, 耗时: {}ms", 
                result.getTotalCount(), result.getCheckedCount(), 
                result.getValidCount(), result.getInvalidCount(), 
                result.getFailedCount(), duration);
            
            return result;
            
        } catch (Exception e) {
            log.error("商品校验任务执行失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            return result;
        }
    }

    /**
     * 处理一批商品（分批请求）
     */
    private CheckResult processProductBatch(List<String> productIds, int batchSize) {
        CheckResult result = new CheckResult();
        
        // 将商品ID列表分成多个批次
        List<List<String>> batches = partitionList(productIds, batchSize);
        log.debug("将 {} 个商品分成 {} 个批次", productIds.size(), batches.size());
        
        // 使用 CountDownLatch 等待所有批次完成
        CountDownLatch latch = new CountDownLatch(batches.size());
        
        // 并发结果收集（线程安全）
        List<BatchCheckResult> batchResults = new CopyOnWriteArrayList<>();
        
        // 提交所有批次任务到线程池
        for (int i = 0; i < batches.size(); i++) {
            final List<String> batch = batches.get(i);
            final int batchIndex = i;
            
            // 提交任务（队列满时会阻塞）
            productCheckExecutor.execute(() -> {
                try {
                    BatchCheckResult batchResult = processSingleBatch(batch, batchIndex);
                    batchResults.add(batchResult);
                } catch (Exception e) {
                    log.error("批次 {} 处理失败", batchIndex, e);
                    BatchCheckResult errorResult = new BatchCheckResult();
                    errorResult.setFailedCount(batch.size());
                    batchResults.add(errorResult);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有批次完成
        try {
            boolean finished = latch.await(30, TimeUnit.MINUTES);
            if (!finished) {
                log.error("批次处理超时（30分钟）");
            }
        } catch (InterruptedException e) {
            log.error("等待批次完成时被中断", e);
            Thread.currentThread().interrupt();
        }
        
        // 汇总结果
        for (BatchCheckResult batchResult : batchResults) {
            result.setCheckedCount(result.getCheckedCount() + batchResult.getCheckedCount());
            result.setValidCount(result.getValidCount() + batchResult.getValidCount());
            result.setInvalidCount(result.getInvalidCount() + batchResult.getInvalidCount());
            result.setFailedCount(result.getFailedCount() + batchResult.getFailedCount());
        }
        
        return result;
    }

    /**
     * 处理单个批次
     */
    private BatchCheckResult processSingleBatch(List<String> productIds, int batchIndex) {
        BatchCheckResult result = new BatchCheckResult();
        
        try {
            // 获取令牌（阻塞等待）
            rateLimiter.acquire();
            log.debug("批次 {} 获取令牌成功，开始请求 TTS API - 商品数: {}", batchIndex, productIds.size());
            
            // 调用 TTS API
            TtsApiResponse apiResponse = ttsApiClient.getProductsByIds(productIds);
            
            if (!apiResponse.isSuccess()) {
                log.error("批次 {} TTS API 调用失败 - code: {}, message: {}", 
                    batchIndex, apiResponse.getCode(), apiResponse.getMessage());
                result.setFailedCount(productIds.size());
                return result;
            }
            
            // 解析响应并更新状态
            List<TtsApiResponse.ProductInfo> products = apiResponse.getData() != null 
                && apiResponse.getData().getProducts() != null 
                ? apiResponse.getData().getProducts() 
                : new ArrayList<>();
            
            log.debug("批次 {} 返回 {} 个商品信息", batchIndex, products.size());
            
            // 构建商品ID -> 商品信息的映射
            Map<String, TtsApiResponse.ProductInfo> productMap = products.stream()
                .collect(Collectors.toMap(
                    TtsApiResponse.ProductInfo::getId, 
                    p -> p, 
                    (p1, p2) -> p1
                ));
            
            LocalDateTime now = LocalDateTime.now();
            
            // 根据API返回结果更新商品状态
            List<String> validProductIds = new ArrayList<>();
            List<String> invalidProductIds = new ArrayList<>();
            
            for (String productId : productIds) {
                TtsApiResponse.ProductInfo productInfo = productMap.get(productId);
                
                if (productInfo != null && productInfo.getCommission() != null) {
                    // 有佣金信息，标记为有效
                    validProductIds.add(productId);
                } else {
                    // 无佣金信息或未返回，标记为失效
                    invalidProductIds.add(productId);
                }
            }
            
            // 批量更新有效商品状态
            if (!validProductIds.isEmpty()) {
                int validCount = productMapper.batchUpdateValidStatus(
                    validProductIds, 
                    TtsProductMonitor.ValidStatus.VALID, 
                    now
                );
                result.setValidCount(validCount);
                log.debug("批次 {} 更新 {} 个有效商品", batchIndex, validCount);
            }
            
            // 批量更新失效商品状态
            if (!invalidProductIds.isEmpty()) {
                int invalidCount = productMapper.batchUpdateValidStatus(
                    invalidProductIds, 
                    TtsProductMonitor.ValidStatus.INVALID, 
                    now
                );
                result.setInvalidCount(invalidCount);
                log.debug("批次 {} 更新 {} 个失效商品", batchIndex, invalidCount);
            }
            
            result.setCheckedCount(productIds.size());
            
        } catch (Exception e) {
            log.error("批次 {} 处理异常", batchIndex, e);
            result.setFailedCount(productIds.size());
        }
        
        return result;
    }

    /**
     * 将列表分割成多个批次
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    /**
     * 校验结果统计
     */
    public static class CheckResult {
        private boolean success = true;
        private int totalCount = 0;
        private int checkedCount = 0;
        private int validCount = 0;
        private int invalidCount = 0;
        private int failedCount = 0;
        private long duration = 0;
        private String errorMessage;

        public void merge(CheckResult other) {
            this.checkedCount += other.checkedCount;
            this.validCount += other.validCount;
            this.invalidCount += other.invalidCount;
            this.failedCount += other.failedCount;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getCheckedCount() { return checkedCount; }
        public void setCheckedCount(int checkedCount) { this.checkedCount = checkedCount; }
        public int getValidCount() { return validCount; }
        public void setValidCount(int validCount) { this.validCount = validCount; }
        public int getInvalidCount() { return invalidCount; }
        public void setInvalidCount(int invalidCount) { this.invalidCount = invalidCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * 批次校验结果
     */
    private static class BatchCheckResult {
        private int checkedCount = 0;
        private int validCount = 0;
        private int invalidCount = 0;
        private int failedCount = 0;

        public int getCheckedCount() { return checkedCount; }
        public void setCheckedCount(int checkedCount) { this.checkedCount = checkedCount; }
        public int getValidCount() { return validCount; }
        public void setValidCount(int validCount) { this.validCount = validCount; }
        public int getInvalidCount() { return invalidCount; }
        public void setInvalidCount(int invalidCount) { this.invalidCount = invalidCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    }
}
