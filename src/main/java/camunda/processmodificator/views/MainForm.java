package camunda.processmodificator.views;

import camunda.processmodificator.model.*;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.views.executionlocalvariable.ExecutionVariablesForm;
import camunda.processmodificator.views.processmigrate.MigrationForm;
import camunda.processmodificator.views.processmodificate.ProcessModificationForm;
import camunda.processmodificator.views.processmodificate.ProcessMultipleModificationForm;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.springframework.web.client.HttpClientErrorException;


public abstract class MainForm extends FormLayout {

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

    public MainForm(CamundaRestService camundaRestService, BaseFormModel formModel) {
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
        initProcessDefinitionKey();
        initTaxIDsTextArea();
        formLayout.add(serverAddress, login, password, processDefinitionKey, taxIds);
        for (Component c : components) {
            formLayout.add(c);
        }
        initSendButton();
        formLayout.add(button);
        formLayout.setWidth("400px");
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
            BaseFormModel bean = formBinder.getBean();
            if (formBinder.writeBeanIfValid(bean)) {
                try {
                    camundaRestService.send(bean);
                } catch (Exception e) {
                    if (isAuthorize(e)) {
                        showNotification("Done!");
                    } else {
                        showNotification("Not authorized");
                    }
                }
            }
        });
    }

    protected void validateAndBindBean(MainForm component) {
        validateCommonsFields();
        if (component instanceof ExecutionVariablesForm) {
            validateVariablesFormFields((ExecutionVariablesForm) component);
            return;
        }
        if (component instanceof MigrationForm) {
            validateMigrationFormFields((MigrationForm) component);
            return;
        }
        if (component instanceof ProcessModificationForm) {
            validateModificationFormFields((ProcessModificationForm) component);
            return;
        }
        if (component instanceof ProcessMultipleModificationForm) {
            validateMultipleModificationFormFields((ProcessMultipleModificationForm) component);
            return;
        }
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
        formBinder
                .forField(processDefinitionKey)
                .asRequired(errorMessage)
                .bind(formModel1 -> processDefinitionKey.getValue(), (baseFormModel, s) -> this.formModel.setProcessDefinitionKey(s.trim()));
        formBinder
                .forField(taxIds)
                .asRequired(errorMessage)
                .bind(formModel1 -> taxIds.getValue(), (baseFormModel, s) -> this.formModel.setTaxIDs(s.trim()));
    }

    private void validateVariablesFormFields(ExecutionVariablesForm form) {
        formBinder
                .forField(form.getVariableName())
                .asRequired(errorMessage)
                .bind(formModel1 -> form.getVariableName().getValue(), (baseFormModel, s) -> ((VariablesFormModel) baseFormModel).setVariableName(s.trim()));
        formBinder
                .forField(form.getVariableValue())
                .bind(formModel1 -> form.getVariableValue().getValue(), (baseFormModel, s) -> ((VariablesFormModel) baseFormModel).setVariableValue(s.trim()));
        formBinder
                .forField(form.getVariableType())
                .asRequired(errorMessage)
                .bind(formModel1 -> form.getVariableType().getValue(), (baseFormModel, s) -> ((VariablesFormModel) baseFormModel).setVariableType(s));
    }

    private void validateMigrationFormFields(MigrationForm form) {
        formBinder
                .forField(form.getTargetProcessDefinition())
                .asRequired(errorMessage)
                .bind(formModel1 -> form.getTargetProcessDefinition().getValue(), (baseFormModel, s) -> ((MigrateFormModel) baseFormModel).setTargetProcessDefinitionId(s.trim()));
    }

    private void validateModificationFormFields(ProcessModificationForm form) {
        formBinder
                .forField(form.getPosition())
                .asRequired()
                .bind(formModel1 -> form.getPosition().getValue(), (baseFormModel, s) -> ((ModificateFormModel) baseFormModel).setTargetActivityPosition(s));
        formBinder
                .forField(form.getTargetActivity())
                .asRequired(errorMessage)
                .bind(formModel1 -> form.getTargetActivity().getValue(), (baseFormModel, s) -> ((ModificateFormModel) baseFormModel).setTargetActivityID(s.trim()));
    }

    private void validateMultipleModificationFormFields(ProcessMultipleModificationForm form) {
        formBinder
                .forField(form.getTargetActivityPosition())
                .asRequired()
                .bind(formModel1 -> form.getTargetActivityPosition().getValue(), (baseFormModel, s) -> ((MultipleModificateFormModel) baseFormModel).setTargetActivityPosition(s));
        formBinder
                .forField(form.getActivityIDs())
                .asRequired(errorMessage)
                .bind(formModel1 -> form.getActivityIDs().getValue(), (baseFormModel, s) -> ((MultipleModificateFormModel) baseFormModel).setActivityIDs(s.trim()));
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
}
