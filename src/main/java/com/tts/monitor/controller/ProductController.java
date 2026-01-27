package com.tts.monitor.controller;

import com.tts.monitor.dto.PageResult;
import com.tts.monitor.dto.ProductAddRequest;
import com.tts.monitor.dto.ProductQueryDTO;
import com.tts.monitor.dto.Result;
import com.tts.monitor.entity.TtsProductMonitor;
import com.tts.monitor.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 商品监控控制器
 * 
 */
@Slf4j
@RestController
@RequestMapping("/TTS/monitor/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 获取商品监控列表
     * 
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<TtsProductMonitor>> getProductList(ProductQueryDTO queryDTO) {
        log.info("查询商品列表 - 参数: {}", queryDTO);
        PageResult<TtsProductMonitor> result = productService.getProductList(queryDTO);
        return Result.success(result);
    }

    /**
     * 新增监控商品
     * 
     * @param request 新增请求
     * @return 新增数量
     */
    @PostMapping("/add")
    public Result<Integer> addProducts(@Valid @RequestBody ProductAddRequest request) {
        log.info("新增监控商品 - 数量: {}", request.getProductIds().size());
        int count = productService.addProducts(request.getProductIds());
        return Result.success("成功新增 " + count + " 个商品", count);
    }

    /**
     * 删除监控商品
     * 
     * @param productId 商品ID
     * @return 操作结果
     */
    @DeleteMapping("/{productId}")
    public Result<Void> deleteProduct(@PathVariable String productId) {
        log.info("删除监控商品 - ID: {}", productId);
        productService.deleteProduct(productId);
        return Result.success();
    }

    /**
     * 确认商品失效
     * 
     * @param productId 商品ID
     * @return 操作结果
     */
    @PostMapping("/{productId}/confirm")
    public Result<Void> confirmProduct(@PathVariable String productId) {
        log.info("确认商品失效 - ID: {}", productId);
        productService.confirmProduct(productId);
        return Result.success();
    }

    /**
     * 统计商品总数
     * 
     * @return 商品总数
     */
    @GetMapping("/count")
    public Result<Long> countProducts() {
        Long count = productService.countTotalProducts();
        return Result.success(count);
    }
}
