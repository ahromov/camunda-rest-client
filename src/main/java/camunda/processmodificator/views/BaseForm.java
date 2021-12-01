package camunda.processmodificator.views;

import camunda.processmodificator.model.*;
import camunda.processmodificator.service.CamundaRestService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.springframework.web.client.HttpClientErrorException;


public abstract class BaseForm extends FormLayout {

    protected CamundaRestService camundaRestService;
    protected Binder<BaseFormModel> formBinder = new Binder<>(BaseFormModel.class);
    protected FormLayout formLayout;
    protected Notification notification;
    protected TextField serverAddress;
    protected TextField processDefinitionKey;
    protected TextArea taxIds;
    protected TextField login;
    protected TextField password;
    protected Button button;
    protected BaseFormModel formModel;
    private final String errorMessage = "Field must not be empty";

    public BaseForm(CamundaRestService camundaRestService, BaseFormModel formModel) {
        this.camundaRestService = camundaRestService;
        this.formModel = formModel;
        formBinder.setBean(this.formModel);
        this.setWidth("400px");
    }

    protected void initFormLayout(Component... components) {
        formLayout = new FormLayout();
        initNotification();
        initServerAddressField();
        initLoginField();
        initPasswordField();
        formLayout.add(serverAddress, login, password);
        initSendButton();
        formLayout.add(button);
        formLayout.setWidth("400px");
        this.add(formLayout);
    }

    protected void initNotification() {
        notification = new Notification("", 5000, Notification.Position.MIDDLE);
    }

    protected void initServerAddressField() {
        serverAddress = new TextField();
        serverAddress.setLabel("Server address");
        serverAddress.setPlaceholder("http://localhost:8080");
        serverAddress.setRequired(true);
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
            BaseFormModel bean = formBinder.getBean();
            if (formBinder.writeBeanIfValid(bean)) {
                send(bean);
            }
        });
    }

    protected void validateAndBindBean(BaseForm component) {
        validateCommonsFields();
    }

    private void validateCommonsFields() {
        formBinder
                .forField(serverAddress)
                .withValidator(address -> address.startsWith("http"), "The address must be starts on \"http://\" or \"https://\"")
                .withValidator(address -> !address.endsWith("/"), "Remove \"/\" from the end address")
                .asRequired(errorMessage)
                .bind(formModel1 -> serverAddress.getValue(), (baseFormModel, s) -> this.formModel.setServerAddress(s));
        formBinder
                .forField(login)
                .bind(formModel1 -> login.getValue(), (baseFormModel, s) -> this.formModel.setEngineLogin(s.trim()));
        formBinder
                .forField(password)
                .bind(formModel1 -> password.getValue(), (baseFormModel, s) -> this.formModel.setEnginePassword(s.trim()));
    }

    private boolean isAuthorize(Exception e) {
        if (e instanceof HttpClientErrorException) {
            int rawStatusCode = ((HttpClientErrorException) e).getRawStatusCode();
            if (rawStatusCode == 401) {
                return false;
            }
        }
        return true;
    }

    private void showNotification(String s) {
        notification.setText(s);
        notification.open();
    }

    private void send(BaseFormModel bean) {
        try {
            camundaRestService.send(bean);
            showNotification("Done!");
        } catch (Exception e) {
            if (isAuthorize(e) == false) {
                showNotification("Not authenticated: " + e.getMessage());
            } else {
                showNotification(e.getMessage());
            }
        }
    }
}
