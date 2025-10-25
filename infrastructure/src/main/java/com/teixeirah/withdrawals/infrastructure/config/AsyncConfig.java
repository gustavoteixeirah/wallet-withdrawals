package com.teixeirah.withdrawals.infrastructure.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            // Capture MDC context *before* task execution is scheduled
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    // Set the captured MDC context *before* the task runs
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    // Clear the MDC context *after* the task runs
                    MDC.clear();
                }
            };
        };
    }


    @Bean("taskExecutor") // Explicitly naming the bean can be helpful
    public ThreadPoolTaskExecutor taskExecutor(TaskDecorator mdcTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // --- Configure Executor Properties ---
        // These are examples, adjust based on your application's needs
        executor.setCorePoolSize(5);   // Minimum number of threads
        executor.setMaxPoolSize(10);  // Maximum number of threads
        executor.setQueueCapacity(25); // Number of tasks to queue before rejecting
        executor.setThreadNamePrefix("async-task-"); // Useful for logging/debugging
        // ------------------------------------
        executor.setTaskDecorator(mdcTaskDecorator); // Apply the decorator
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        // By default, Spring looks for a bean named "taskExecutor" or a unique
        // TaskExecutor bean. We return our specifically configured one.
        // If you autowire it, ensure you use the correct bean name/qualifier if needed.
        return taskExecutor(mdcTaskDecorator());
    }
}