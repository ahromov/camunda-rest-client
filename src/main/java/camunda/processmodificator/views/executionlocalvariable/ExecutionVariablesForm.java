package camunda.processmodificator.views.executionlocalvariable;


import camunda.processmodificator.model.VariablesFormModel;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.views.MainForm;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class ExecutionVariablesForm extends MainForm {

    private TextField variableName;
    private Select<String> variableType;
    private TextField variableValue;

    public ExecutionVariablesForm(CamundaRestService camundaRestService, VariablesFormModel formModel) {
        super(camundaRestService, formModel);
        this.variableName = initVariableName();
        this.variableType = initVariableType();
        this.variableValue = initVariableValue();
        super.initFormLayout(variableName, variableValue, variableType);
        super.validateAndBindBean(this);
    }

    private TextField initVariableName() {
        TextField field = new TextField();
        field.setLabel("Variable name");
        field.setPlaceholder("application.end.date");
        return field;
    }

    private TextField initVariableValue() {
        TextField field = new TextField();
        field.setLabel("Variable value");
        field.setPlaceholder("30.04.2021/10:00");
        return field;
    }

    private Select<String> initVariableType() {
        Select<String> finalActivity = new Select();
        finalActivity.setLabel("Variable type");
        finalActivity.setItems(Arrays.asList("String"));
        return finalActivity;
    }
}
