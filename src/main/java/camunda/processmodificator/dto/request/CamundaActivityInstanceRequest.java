package camunda.processmodificator.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CamundaActivityInstanceRequest {

    private String processInstanceId;
    private List<Map<String, String>> sorting;
}
