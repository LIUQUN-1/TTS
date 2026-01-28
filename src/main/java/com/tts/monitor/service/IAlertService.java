package com.tts.monitor.service;

/**
 * 告警服务接口
 * 
 */
public interface IAlertService {

    /**
     * 执行告警检查并发送
     */
    void executeAlert();
}
