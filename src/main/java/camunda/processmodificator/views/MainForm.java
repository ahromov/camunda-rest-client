package camunda.processmodificator.views;

import camunda.processmodificator.model.FormModel;
import camunda.processmodificator.service.CamundaRestService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class MainForm extends FormLayout {

    protected CamundaRestService camundaRestService;
    protected Binder<FormModel> formBinder = new Binder<>(FormModel.class);
    protected FormLayout formLayout;
    protected Notification notification;
    protected TextField serverAddress;
    protected TextField processDefinitionKey;
    protected TextArea taxIds;
    protected TextField login;
    protected TextField password;
    protected Button button;
    protected FormModel formModel;

    public MainForm(CamundaRestService camundaRestService) {
        this.camundaRestService = camundaRestService;
        formBinder.setBean(new FormModel());
        initNotification();
        this.setWidth("400px");
    }

    protected void initFormLayout(Component... components) {
        formLayout = new FormLayout();
        initServerAddressField();
        initLoginField();
        initPasswordField();
        initProcessDefinitionKey();
        initTaxIDsTextArea();
        formLayout.add(serverAddress, login, password, processDefinitionKey, taxIds);
        for (Component c : components) {
            formLayout.add(c);
        }
        initSendButton();
        formLayout.add(button);
        formLayout.setWidth("400px");
        validateAndBindBean();
        this.add(formLayout);
    }

    protected void initNotification() {
        notification = new Notification("Done!", 3000, Notification.Position.MIDDLE);
    }

    protected void initServerAddressField() {
        serverAddress = new TextField();
        serverAddress.setLabel("Server address");
        serverAddress.setPlaceholder("http://localhost:8080");
        serverAddress.setRequired(true);
    }

    protected void initProcessDefinitionKey() {
        processDefinitionKey = new TextField();
        processDefinitionKey.setLabel("Process definition key");
        processDefinitionKey.setPlaceholder("camunda-process-application");
        processDefinitionKey.setRequired(true);
    }

    protected void initTaxIDsTextArea() {
        taxIds = new TextArea();
        taxIds.setLabel("Contragents tax IDs (or full buiseness keys)");
        taxIds.setRequired(true);
        taxIds.setPlaceholder("01992268(380984632879:01992268:1)");
    }

    protected void initLoginField() {
        login = new TextField();
        login.setLabel("Login");
        login.setPlaceholder("user");
    }

    protected void initPasswordField() {
        password = new TextField();
        password.setLabel("Password");
        password.setPlaceholder("password");
    }

    protected void initSendButton() {
        button = new Button("Send");
        button.addClickListener(buttonClickEvent -> {
            FormModel bean = formBinder.getBean();
            if (formBinder.writeBeanIfValid(bean)) {
                camundaRestService.send(bean);
                notification.open();
            }
        });
    }

    protected void validateAndBindBean() {
        formBinder.forField(serverAddress).bind(FormModel::getServerAddress, FormModel::setServerAddress);
        formBinder.forField(processDefinitionKey).bind(FormModel::getProcessDefinitionKey, FormModel::setProcessDefinitionKey);
        formBinder.forField(login).bind(FormModel::getEngineLogin, FormModel::setEngineLogin);
        formBinder.forField(password).bind(FormModel::getEnginePassword, FormModel::setEnginePassword);
        formBinder.forField(taxIds).bind(formModel -> taxIds.getValue(), FormModel::setTaxIDs);
    }
}
