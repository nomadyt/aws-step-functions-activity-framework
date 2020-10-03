package nomadyt.aws.step.functions.activity.framework.executors;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.SendTaskHeartbeatRequest;
import com.amazonaws.services.stepfunctions.model.TaskTimedOutException;
import nomadyt.aws.step.functions.activity.framework.Activity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ActivityHeartbeatTaskExecutor {
    private static final Logger LOG = LogManager.getLogger(ActivityHeartbeatTaskExecutor.class);

    private final ScheduledExecutorService activityHeartbeatExecutorService;
    private final AWSStepFunctions awsStepFunctionsClient;

    public ActivityHeartbeatTaskExecutor(
            final ScheduledExecutorService activityHeartbeatExecutorService,
            final AWSStepFunctions awsStepFunctionsClient
    ) {
        this.activityHeartbeatExecutorService = activityHeartbeatExecutorService;
        this.awsStepFunctionsClient = awsStepFunctionsClient;
    }

    public void startActivityHeartbeat(final Activity activity, final String taskToken) {
        activityHeartbeatExecutorService.execute(new ActivityHeartbeatTask(activity, taskToken));
    }

    private class ActivityHeartbeatTask implements Runnable {
        private final static int MAX_RETRY = 3;

        private final Activity activity;
        private final String taskToken;

        private int cur_retry = 0;

        public ActivityHeartbeatTask(
                final Activity activity,
                final String taskToken
        ) {
            this.activity = activity;
            this.taskToken = taskToken;
        }

        public void run() {
            try {
                LOG.info("Send heartbeat for activity " + activity.getArn() + " with token " + taskToken);

                awsStepFunctionsClient.sendTaskHeartbeat(new SendTaskHeartbeatRequest().withTaskToken(taskToken));
                activityHeartbeatExecutorService.schedule(this, activity.getHeartbeatIntervalSeconds(), TimeUnit.SECONDS);

                // reset retry count
                cur_retry = 0;
            } catch (TaskTimedOutException ex) {
                LOG.error("Activity " + activity.getArn() + " already timed out with token " + taskToken, ex);
            } catch (Exception ex) {
                LOG.error("Exception thrown when running heartbeat task for activity " + activity.getArn() + " with token " + taskToken, ex);
                if (cur_retry < MAX_RETRY) {
                    cur_retry += 1;
                    activityHeartbeatExecutorService.schedule(this, 10, TimeUnit.MILLISECONDS);
                } else {
                    throw ex;
                }
            }
        }
    }
}
