package camunda.processmodificator.dto.response;

import lombok.Data;

@Data
public class CamundaActivityInstanceResponse {

    private String activityId;
    private String activityName;
}
