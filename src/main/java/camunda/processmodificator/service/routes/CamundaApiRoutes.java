package camunda.processmodificator.service.routes;

public class CamundaApiRoutes {

    public static final String BASE_ROUTE = "/engine-rest";
    public static final String PROCESS_INSTANCE_RESOURCE_PATH = BASE_ROUTE + "/process-instance/";
    public static final String HISTORY_PROCESS_INSTANCE_RESOURCE_PATH = BASE_ROUTE + "/process-instance";
    public static final String HISTORY_ACTIVITY_RESOURCE_PATH = BASE_ROUTE + "/history/activity-instance";
    public static final String PROCESS_MIGRATION_RESOURCE_PATH = BASE_ROUTE + "/migration/execute";
    public static final String EXECUTION_RESOURCE_PATH = BASE_ROUTE + "/execution";
    public static final String PROCESS_INSTANCE_INCIDENTS_COUNT = BASE_ROUTE + "/incident/count?processInstanceId=";
}
