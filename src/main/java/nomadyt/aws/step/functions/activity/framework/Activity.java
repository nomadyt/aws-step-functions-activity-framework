package nomadyt.aws.step.functions.activity.framework;


public interface Activity {
    String execute(String input) throws Exception;
    String getArn();

    int getHeartbeatIntervalSeconds();
}
