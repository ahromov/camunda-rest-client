package camunda.processmodificator.dto.request;

import lombok.Builder;
import lombok.Data;

import javax.naming.directory.ModificationItem;
import java.util.Map;

@Data
@Builder
public class CamundaExecutionSetVariableRequest {

    private Map<String, Modifications> modifications;

    @Data
    @Builder
    public static class Modifications {
        private String value;
        private String type;
    }
}
