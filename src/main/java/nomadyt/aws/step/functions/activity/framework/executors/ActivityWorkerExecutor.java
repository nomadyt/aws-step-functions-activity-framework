package nomadyt.aws.step.functions.activity.framework.executors;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.services.stepfunctions.model.SendTaskFailureRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.amazonaws.services.stepfunctions.model.TaskTimedOutException;
import nomadyt.aws.step.functions.activity.framework.Activity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

@Component
public class ActivityWorkerExecutor {
    private static Logger LOG = LogManager.getLogger(ActivityWorkerExecutor.class);

    private final AWSStepFunctions awsStepFunctionsClient;
    private final ThreadPoolExecutor activityWorkerThreadPoolExecutor;
    private final ActivityHeartbeatTaskExecutor heartbeatTaskExecutor;
    private final int activityWorkerQueueSoftCap;

    public ActivityWorkerExecutor(
            final AWSStepFunctions awsStepFunctionsClient,
            final ThreadPoolExecutor activityWorkerThreadPoolExecutor,
            final ActivityHeartbeatTaskExecutor heartbeatTaskExecutor,
            final int activityWorkerQueueSoftCap
    ) {
        this.awsStepFunctionsClient = awsStepFunctionsClient;
        this.activityWorkerThreadPoolExecutor = activityWorkerThreadPoolExecutor;
        this.heartbeatTaskExecutor = heartbeatTaskExecutor;
        this.activityWorkerQueueSoftCap = activityWorkerQueueSoftCap;
    }

    public boolean isActivityWorkerQueueSoftCapReached() {
        return activityWorkerThreadPoolExecutor.getQueue().size() >= activityWorkerQueueSoftCap;
    }

    public void startActivityWorkerTask(final Activity activity, final GetActivityTaskResult getActivityTaskResult) {
        activityWorkerThreadPoolExecutor.execute(new ActivityWorkerTask(activity, getActivityTaskResult));
    }

    private class ActivityWorkerTask implements Runnable {
        private final GetActivityTaskResult getActivityTaskResult;
        private final Activity activity;

        public ActivityWorkerTask(
                final Activity activity,
                final GetActivityTaskResult getActivityTaskResult
        ) {
            this.activity = activity;
            this.getActivityTaskResult = getActivityTaskResult;
        }

        public void run() {
            LOG.info("Start running activity " + activity.getArn() + " with token " + getActivityTaskResult.getTaskToken());

            try {
                heartbeatTaskExecutor.startActivityHeartbeat(activity, getActivityTaskResult.getTaskToken());
                String result = activity.execute(getActivityTaskResult.getInput());
                awsStepFunctionsClient.sendTaskSuccess(
                        new SendTaskSuccessRequest().withOutput(
                                result).withTaskToken(getActivityTaskResult.getTaskToken()));
            } catch (TaskTimedOutException ex) {
                LOG.error("Activity " + activity.getArn() + " already timed out with token " + getActivityTaskResult.getTaskToken(), ex);
            } catch (Exception ex) {
                LOG.error(
                        "Exception thrown when running activity " + activity.getArn() + " with token " + getActivityTaskResult.getTaskToken(),
                        ex
                );
                awsStepFunctionsClient.sendTaskFailure(new SendTaskFailureRequest().withTaskToken(
                        getActivityTaskResult.getTaskToken()));
            }
        }
    }
}
