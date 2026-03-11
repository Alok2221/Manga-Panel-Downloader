package com.mangapanel.downloader.config;

import com.mangapanel.downloader.config.properties.DownloadExecutorProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "downloadExecutor")
    public Executor downloadExecutor(DownloadExecutorProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int core = Math.max(1, props.corePoolSize());
        int max = props.maxPoolSize() > 0 ? Math.max(core, props.maxPoolSize()) : core;
        int queue = Math.max(1, props.queueCapacity());
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix(props.threadNamePrefix() != null ? props.threadNamePrefix() : "download-");
        executor.initialize();
        return executor;
    }
}
