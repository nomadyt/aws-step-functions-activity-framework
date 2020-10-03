package nomadyt.aws.step.functions.activity.framework.executors;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import nomadyt.aws.step.functions.activity.framework.Activity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ActivityPollerExecutor {
    private static final Logger LOG = LogManager.getLogger(ActivityPollerExecutor.class);

    private final AWSStepFunctions awsStepFunctionsClient;
    private final ScheduledExecutorService activityPollerExecutorService;
    private final ActivityWorkerExecutor activityWorkerExecutor;
    private final List<Activity> activityList;

    public ActivityPollerExecutor(
            final AWSStepFunctions awsStepFunctionsClient,
            final ScheduledExecutorService activityPollerExecutorService,
            final ActivityWorkerExecutor activityWorkerExecutor,
            final List<Activity> activityList
    ) {
        this.awsStepFunctionsClient = awsStepFunctionsClient;
        this.activityPollerExecutorService = activityPollerExecutorService;
        this.activityWorkerExecutor = activityWorkerExecutor;
        this.activityList = activityList;
        startPollers();
    }

    private void startPollers() {
        this.activityList.forEach(
                a -> this.activityPollerExecutorService.execute(new ActivityPollerTask(a))
        );
    }

    private class ActivityPollerTask implements Runnable {
        private final Activity activity;

        public ActivityPollerTask(final Activity activity) {
            this.activity = activity;
        }

        public void run() {
            if (activityWorkerExecutor.isActivityWorkerQueueSoftCapReached()) {
                // if there are too many activities in queue waiting for picking up, don't poll a new activity
                activityPollerExecutorService.schedule(this, 1000, TimeUnit.MILLISECONDS);
            }

            try {
                LOG.info("Start polling activity " + activity.getArn());
                GetActivityTaskResult getActivityTaskResult =
                        awsStepFunctionsClient.getActivityTask(
                                new GetActivityTaskRequest().withActivityArn(
                                        activity.getArn()
                                ).withWorkerName(System.getProperty("indeed.instance"))
                        );

                if (getActivityTaskResult.getTaskToken() != null) {
                    LOG.info("Successfully polled activity " + activity.getArn() + " with token " + getActivityTaskResult.getTaskToken());
                    activityWorkerExecutor.startActivityWorkerTask(
                            activity, getActivityTaskResult
                    );
                }
                activityPollerExecutorService.execute(this);
            } catch (Exception ex) {
                LOG.error("Exception thrown when polling activity " + activity.getArn(), ex);
                LOG.info("Retry activity polling task after 1000ms.");
                activityPollerExecutorService.schedule(this, 1000, TimeUnit.MILLISECONDS);
            }
        }
    }
}
