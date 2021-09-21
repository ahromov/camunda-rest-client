package camunda.processmodificator.model;

import lombok.*;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MigrateFormModel extends BaseFormModel {

    private String targetProcessDefinitionId;
}
