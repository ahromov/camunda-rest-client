package camunda.processmodificator.dto.response;

import lombok.Data;

@Data
public class CamundaProcessInstanceResponse {

    private String id;
    private String businessKey;
    private String processDefinitionId;
}
