package camunda.processmodificator.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CamundaProcessInstanceRequest {

    private String businessKeyLike;
    private String processDefinitionKey;
}
