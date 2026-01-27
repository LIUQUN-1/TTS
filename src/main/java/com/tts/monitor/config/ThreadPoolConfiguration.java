package com.tts.monitor.config;

import com.tts.monitor.config.TtsApiProperties.ThreadPoolConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 线程池配置类
 * 
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ThreadPoolConfiguration {

    private final TtsApiProperties ttsApiProperties;

    /**
     * 创建商品校验专用线程池
     * 使用有界队列 + CallerRunsPolicy 阻塞策略
     */
    @Bean(name = "productCheckExecutor")
    public ThreadPoolExecutor productCheckExecutor() {
        ThreadPoolConfig config = ttsApiProperties.getThreadPool();
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            config.getCoreSize(),
            config.getMaxSize(),
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(config.getQueueCapacity()),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName(config.getThreadNamePrefix() + counter++);
                    thread.setDaemon(false);
                    return thread;
                }
            },
            // 队列满时，由调用者线程执行，实现阻塞效果
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        log.info("初始化商品校验线程池 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
            config.getCoreSize(), config.getMaxSize(), config.getQueueCapacity());
        
        return executor;
    }

    /**
     * 应用关闭时优雅关闭线程池
     */
    @Bean
    public ThreadPoolShutdownHook threadPoolShutdownHook(ThreadPoolExecutor productCheckExecutor) {
        return new ThreadPoolShutdownHook(productCheckExecutor);
    }

    /**
     * 线程池关闭钩子
     */
    static class ThreadPoolShutdownHook {
        private final ThreadPoolExecutor executor;

        public ThreadPoolShutdownHook(ThreadPoolExecutor executor) {
            this.executor = executor;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("开始关闭线程池...");
                this.executor.shutdown();
                try {
                    if (!this.executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        this.executor.shutdownNow();
                        log.warn("线程池未能在60秒内正常关闭，强制关闭");
                    } else {
                        log.info("线程池已正常关闭");
                    }
                } catch (InterruptedException e) {
                    this.executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }));
        }
    }
}
