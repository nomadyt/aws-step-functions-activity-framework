package nomadyt.aws.step.functions.activity.framework.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
public class ActivityHeartbeatExecutorConfig {
    @Value("${activity.heartbeat.pool.size:1}")
    private int activityHeartbeatWorkerPoolSize;

    @Bean
    public ScheduledExecutorService activityHeartbeatExecutorService() {
        return Executors.newScheduledThreadPool(activityHeartbeatWorkerPoolSize);
    }

    @Bean
    public AWSStepFunctions awsStepFunctionsClient(
            final AWSCredentialsProvider awsCredentialsProvider
    ) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSocketTimeout((int) TimeUnit.SECONDS.toMillis(70));

        return AWSStepFunctionsClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .withCredentials(awsCredentialsProvider)
                .withClientConfiguration(clientConfiguration)
                .build();
    }
}

