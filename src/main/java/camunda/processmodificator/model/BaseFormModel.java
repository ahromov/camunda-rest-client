package camunda.processmodificator.model;

import lombok.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public abstract class BaseFormModel {

    protected String serverAddress;
    protected String engineLogin;
    protected String enginePassword;
}
