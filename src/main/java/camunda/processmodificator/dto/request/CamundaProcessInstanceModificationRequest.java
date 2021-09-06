package camunda.processmodificator.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CamundaProcessInstanceModificationRequest {

    private Boolean skipCustomListeners;
    private Boolean skipIoMappings;
    private List<Map<String, String>> instructions;
}
