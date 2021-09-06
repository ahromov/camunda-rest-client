package camunda.processmodificator.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CamundaProcessInstanceRequest {

    private String processInstanceBusinessKeyLike;
    private String processDefinitionKey;
    private Boolean finished;
}
