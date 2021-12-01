package camunda.processmodificator.service.impl;

import camunda.processmodificator.dto.request.*;
import camunda.processmodificator.dto.response.*;
import camunda.processmodificator.model.BaseFormModel;
import camunda.processmodificator.model.VariablesFormModel;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.service.routes.CamundaApiRoutes;
import camunda.processmodificator.service.utils.CamundaApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ExecutionLocalVariablesRestService implements CamundaRestService {

    private final RestTemplate restTemplate;
    private final CamundaApiUtils camundaApiUtils;
    private String executionVariableValue;

    public ExecutionLocalVariablesRestService(RestTemplate restTemplate, CamundaApiUtils camundaApiUtils) {
        this.restTemplate = restTemplate;
        this.camundaApiUtils = camundaApiUtils;
    }

    public void send(BaseFormModel formModel) {
        VariablesFormModel variablesFormModel = (VariablesFormModel) formModel;

        HttpHeaders headers = new HttpHeaders();

        camundaApiUtils.authenticate(headers, variablesFormModel);

        List<String[]> taxIDs = camundaApiUtils.parse(variablesFormModel.getTaxIDs());

        for (String[] tax : taxIDs) {
            HttpEntity<CamundaProcessInstanceRequest> processInstanceRequestHttpEntity = CamundaApiUtils.prepareProcessInstanceRequestHttpEntity(headers, tax, variablesFormModel);

            ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse =
                    restTemplate.exchange(camundaApiUtils.getUrl(variablesFormModel, CamundaApiRoutes.HISTORY_PROCESS_INSTANCE_RESOURCE_PATH), HttpMethod.POST, processInstanceRequestHttpEntity, CamundaProcessInstanceResponse[].class);

            if (camundaApiUtils.getObject(processInstanceResponse).isPresent()) {
                CamundaProcessInstanceResponse processInstance = camundaApiUtils.getObject(processInstanceResponse).get();

                if (camundaApiUtils.isProcessInstanceIncidents(variablesFormModel, headers, restTemplate, processInstance)) {
                    break;
                }

                String url = camundaApiUtils.getUrl(variablesFormModel, CamundaApiRoutes.EXECUTION_RESOURCE_PATH) + "/" + camundaApiUtils.getObject(processInstanceResponse).get().getId() + "/localVariables";

                HttpEntity<CamundaExecutionSetVariableRequest> camundaExecutionSetVariableRequestHttpEntity = prepareProcessInstanceModificationRequestHttpEntity(variablesFormModel, headers, tax);
                ResponseEntity<CamundaExecutionLocalVariablesResponse> camundaProcessInstanceModificationResponseResponse =
                        restTemplate.exchange(url, HttpMethod.POST, camundaExecutionSetVariableRequestHttpEntity, CamundaExecutionLocalVariablesResponse.class);

                logResponse(variablesFormModel, processInstance, camundaProcessInstanceModificationResponseResponse.getStatusCodeValue());
            } else {
                camundaApiUtils.logOperationException(tax[0], formModel.getProcessDefinitionKey());
            }
        }
    }

    private HttpEntity<CamundaExecutionSetVariableRequest> prepareProcessInstanceModificationRequestHttpEntity(VariablesFormModel formModel, HttpHeaders headers, String[] tax) {
        executionVariableValue = formModel.getVariableValue();
        if (executionVariableValue == null || executionVariableValue.isEmpty()) {
            executionVariableValue = tax[1];
        }
        CamundaExecutionSetVariableRequest.Modifications modifications = CamundaExecutionSetVariableRequest.Modifications.builder()
                .value(executionVariableValue)
                .type(formModel.getVariableType())
                .build();
        Map<String, CamundaExecutionSetVariableRequest.Modifications> modificationsMap = new HashMap();
        modificationsMap.put(formModel.getVariableName(), modifications);
        CamundaExecutionSetVariableRequest camundaExecutionSetVariableRequest = CamundaExecutionSetVariableRequest.builder()
                .modifications(modificationsMap)
                .build();
        HttpEntity<CamundaExecutionSetVariableRequest> camundaExecutionSetVariableRequestHttpEntity = new HttpEntity<>(camundaExecutionSetVariableRequest, headers);
        return camundaExecutionSetVariableRequestHttpEntity;
    }

    private void logResponse(VariablesFormModel formModel, CamundaProcessInstanceResponse processInstanceResponse, Integer statusCode) {
        if (statusCode == 204) {
            log.info("Variable={}:{} added to process execution={}:{}",
                    formModel.getVariableName(),
                    executionVariableValue,
                    processInstanceResponse.getId(),
                    processInstanceResponse.getBusinessKey());
        }
    }
}
