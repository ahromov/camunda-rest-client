package camunda.processmodificator.service.impl;

import camunda.processmodificator.dto.request.*;
import camunda.processmodificator.dto.response.*;
import camunda.processmodificator.model.FormModel;
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
import java.util.Map;

@Service
@Slf4j
public class ExecutionLocalVariablesRestService implements CamundaRestService {

    private RestTemplate restTemplate;
    private String variableValue;

    public ExecutionLocalVariablesRestService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void send(FormModel formModel) {
        HttpHeaders headers = new HttpHeaders();

        CamundaApiUtils.authenticate(headers, formModel);

        formModel.getTaxIDs().forEach(tax -> {
            HttpEntity<CamundaProcessInstanceRequest> processInstanceRequestHttpEntity = CamundaApiUtils.prepareProcessInstanceRequestHttpEntity(headers, tax, formModel);

            ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse =
                    restTemplate.exchange(CamundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_PROCESS_INSTANCE_RESOURCE_PATH), HttpMethod.POST, processInstanceRequestHttpEntity, CamundaProcessInstanceResponse[].class);

            HttpEntity<CamundaExecutionSetVariableRequest> camundaExecutionSetVariableRequestHttpEntity = prepareProcessInstanceModificationRequestHttpEntity(formModel, headers, tax);

            String url = CamundaApiUtils.getUrl(formModel, CamundaApiRoutes.EXECUTION_RESOURCE_PATH)+ "/" + CamundaApiUtils.getObject(processInstanceResponse).getId() + "/localVariables";

            ResponseEntity<CamundaExecutionLocalVariablesResponse> camundaProcessInstanceModificationResponseResponse =
                    restTemplate.exchange(url, HttpMethod.POST, camundaExecutionSetVariableRequestHttpEntity, CamundaExecutionLocalVariablesResponse.class);

            logResponse(formModel, processInstanceResponse, camundaProcessInstanceModificationResponseResponse.getStatusCodeValue());
        });
    }

    private HttpEntity<CamundaExecutionSetVariableRequest> prepareProcessInstanceModificationRequestHttpEntity(FormModel formModel, HttpHeaders headers, String[] tax) {
        variableValue = formModel.getVariableValue();
        if (variableValue == null || variableValue.isEmpty()) {
            variableValue = tax[1];
        }
        CamundaExecutionSetVariableRequest.Modifications modifications = CamundaExecutionSetVariableRequest.Modifications.builder()
                .value(variableValue)
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

    private void logResponse(FormModel formModel, ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse, Integer statusCode) {
        if (statusCode == 204) {
            log.info("Variable={}:{} added to process execution={}:{}",
                    formModel.getVariableName(),
                    variableValue,
                    CamundaApiUtils.getObject(processInstanceResponse).getId(),
                    CamundaApiUtils.getObject(processInstanceResponse).getBusinessKey());
        }
    }
}
