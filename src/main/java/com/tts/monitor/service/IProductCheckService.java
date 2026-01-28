package com.tts.monitor.service;

/**
 * 商品校验服务接口
 * 
 */
public interface IProductCheckService {

    /**
     * 执行全量商品校验
     * 
     * @return 校验结果统计
     */
    ProductCheckService.CheckResult executeProductCheck();
}
