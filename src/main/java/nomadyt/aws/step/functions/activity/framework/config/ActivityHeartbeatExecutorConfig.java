package nomadyt.aws.step.functions.activity.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ActivityHeartbeatExecutorConfig {
    @Value("${activity.heartbeat.pool.size:1}")
    private int activityHeartbeatWorkerPoolSize;

    @Bean
    public ScheduledExecutorService activityHeartbeatExecutorService() {
        return Executors.newScheduledThreadPool(activityHeartbeatWorkerPoolSize);
    }
}

