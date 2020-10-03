package nomadyt.aws.step.functions.activity.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ActivityPollerExecutorConfig {
    @Value("${activity.poller.pool.size:1}")
    private int activityPollerPoolSize;

    @Bean
    public ScheduledExecutorService activityPollerExecutorService() {
        return Executors.newScheduledThreadPool(activityPollerPoolSize);
    }
}
