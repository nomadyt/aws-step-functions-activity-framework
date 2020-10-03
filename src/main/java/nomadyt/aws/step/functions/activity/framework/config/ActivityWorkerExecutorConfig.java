package nomadyt.aws.step.functions.activity.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ActivityWorkerExecutorConfig {
    @Value("${activity.worker.pool.size:1}")
    private int activityWorkerPoolSize;

    @Value("${activity.worker.queue.softCap:1}")
    private int activityWorkerQueueSoftCap;

    @Bean
    public ThreadPoolExecutor activityWorkerThreadPoolExecutor() {
        return new ThreadPoolExecutor(
                activityWorkerPoolSize, activityWorkerPoolSize,
                0L, TimeUnit.SECONDS, new LinkedBlockingDeque<>()
        );
    }

    @Bean
    public int activityWorkerQueueSoftCap() {
        return activityWorkerQueueSoftCap;
    }
}
