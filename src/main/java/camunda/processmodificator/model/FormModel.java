package camunda.processmodificator.model;

import lombok.*;

import java.util.LinkedList;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class FormModel {

    private String serverAddress;
    private List<String[]> taxIDs;
    private String targetProcessDefinitionId;
    private String processDefinitionKey;
    private String variableName;
    private String variableValue;
    private String variableType;
    private String targetActivityID;
    private String targetActivityPosition;
    private String engineLogin;
    private String enginePassword;

    public void setTaxIDs(String taxIds) {
        List<String[]> listIDs = new LinkedList<>();
        String[] split1 = taxIds.split("\n");
        for (String s : split1) {
            String[] splitEntry = s.split(">>");
            listIDs.add(splitEntry);
        }
        this.taxIDs = listIDs;
    }
}
