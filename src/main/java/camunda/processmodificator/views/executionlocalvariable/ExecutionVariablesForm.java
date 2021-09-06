package camunda.processmodificator.views.executionlocalvariable;


import camunda.processmodificator.model.FormModel;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.views.MainForm;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;

public class ExecutionVariablesForm extends MainForm {

    private TextField variableName;
    private Select<String> variableType;
    private TextField variableValue;

    public ExecutionVariablesForm(CamundaRestService camundaRestService) {
        super(camundaRestService);
        this.variableName = initVariableName();
        this.variableType = initVariableType();
        this.variableValue = initVariableValue();
        super.initFormLayout(variableName, variableValue, variableType);
        validateAndBind();
    }

    private void validateAndBind() {
        super.formBinder.forField(variableName).bind(formModel1 -> variableName.getValue(), FormModel::setVariableName);
        super.formBinder.forField(variableValue).bind(formModel1 -> variableValue.getValue(), FormModel::setVariableValue);
        super.formBinder.forField(variableType).bind(formModel1 -> variableType.getValue(), FormModel::setVariableType);
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
        finalActivity.setItems(List.of("String"));
        return finalActivity;
    }
}
