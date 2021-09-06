package camunda.processmodificator.views.executionlocalvariable;

import camunda.processmodificator.model.FormModel;
import camunda.processmodificator.service.ExecutionLocalVariablesRestService;
import camunda.processmodificator.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import java.util.List;

@PageTitle("Local variable")
@Route(value = "variable", layout = MainLayout.class)
public class ExecutionlocalvariableView extends HorizontalLayout {

    private static ExecutionLocalVariablesRestService restService;

    private FormLayout formLayout;
    private Notification notification;
    private TextField serverAddress;
    private TextArea taxIds;
    private TextField variableName;
    private Select<String> variableType;
    private TextField variableValue;
    private TextField login;
    private TextField password;
    private Button add;
    private ProgressBar progressBar;

    public ExecutionlocalvariableView(ExecutionLocalVariablesRestService restService) {
        addClassName("executionvariables-view");
        this.restService = restService;
        formLayout = initFormLayout();
        notification = initNotification();
        add(formLayout);
    }

    private FormLayout initFormLayout() {
        FormLayout formLayout = new FormLayout();
        serverAddress = initServerAddressField();
        taxIds = initTaxIDsTextArea();
        variableName = initVariableName();
        variableType = initVariableType();
        variableValue = initVariableValue();
        login = initLoginField();
        password = initPasswordField();
        add = initMoveButton();
        progressBar = initProgressBar();
        formLayout.add(serverAddress, taxIds, variableName, variableValue, variableType, login, password, add, progressBar);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("1px", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("700px", 3));
        formLayout.setWidth("400px");
        return formLayout;
    }

    private Notification initNotification() {
        return new Notification(
                "All variables injected!", 3000,
                Notification.Position.MIDDLE);
    }

    private TextField initServerAddressField() {
        TextField serverAddress = new TextField();
        serverAddress.setLabel("Server address");
        serverAddress.setPlaceholder("http://localhost:8080");
        serverAddress.setValue("https://kpayegapu01.alfa.bank.int:8343");
        return serverAddress;
    }

    private TextArea initTaxIDsTextArea() {
        TextArea taxIds = new TextArea();
        taxIds.setLabel("Contragents tax IDs (or full buiseness keys)");
        return taxIds;
    }

    private TextField initVariableName() {
        TextField field = new TextField();
        field.setLabel("Variable name");
        field.setPlaceholder("variable name");
        field.setValue("application.end.date");
        return field;
    }

    private TextField initVariableValue() {
        TextField field = new TextField();
        field.setLabel("Variable value");
        field.setPlaceholder("variable value");
        return field;
    }

    private Select<String> initVariableType() {
        Select<String> finalActivity = new Select();
        finalActivity.setLabel("Variable type");
        finalActivity.setItems(List.of("String"));
        return finalActivity;
    }

    private TextField initLoginField() {
        TextField login = new TextField();
        login.setLabel("Login");
        login.setPlaceholder("user");
        return login;
    }

    private TextField initPasswordField() {
        TextField password = new TextField();
        password.setLabel("Password");
        password.setPlaceholder("password");
        return password;
    }

    private Button initMoveButton() {
        Button move = new Button("Add");
        move.addClickListener(buttonClickEvent -> {
            send();
        });
        return move;
    }

    private ProgressBar initProgressBar() {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setVisible(false);
        return progressBar;
    }

    private void send() {
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        FormModel formModel = FormModel.builder()
                .serverAddress(serverAddress.getValue())
                .variableName(variableName.getValue())
                .variableValue(variableValue.getValue())
                .variableType(variableType.getValue())
                .engineLogin(login.getValue())
                .enginePassword(password.getValue())
                .build();
        formModel.setTaxIDs(taxIds.getValue());
        restService.setVariable(formModel);
        progressBar.setVisible(false);
        notification.open();
    }
}
