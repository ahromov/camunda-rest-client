package camunda.processmodificator.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CamundaProcessMigrationRequest {

    private MigrationPlan migrationPlan;
    private List<String> processInstanceIds;
    private Boolean skipCustomListeners;

    @Data
    @Builder
    public static class MigrationPlan {
        private String sourceProcessDefinitionId;
        private String targetProcessDefinitionId;
        private List<Instructions> instructions;
    }

    @Data
    @Builder
    public static class Instructions {
        private List<String> sourceActivityIds;
        private List<String> targetActivityIds;
        private Boolean updateEventTrigger;
    }
}
