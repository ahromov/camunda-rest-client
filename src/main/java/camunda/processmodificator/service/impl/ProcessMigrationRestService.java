package camunda.processmodificator.service.impl;

import camunda.processmodificator.dto.request.CamundaActivityInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessMigrationRequest;
import camunda.processmodificator.dto.response.CamundaActivityInstanceResponse;
import camunda.processmodificator.dto.response.CamundaProcessIncidentsCountResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceMigrationResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceResponse;
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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
public class ProcessMigrationRestService implements CamundaRestService {

    private RestTemplate restTemplate;

    public ProcessMigrationRestService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void send(FormModel formModel) {
        HttpHeaders headers = new HttpHeaders();

        CamundaApiUtils.authenticate(headers, formModel);

        for (String[] tax : formModel.getTaxIDs()) {
            HttpEntity<CamundaProcessInstanceRequest> processInstanceRequestHttpEntity = CamundaApiUtils.prepareProcessInstanceRequestHttpEntity(headers, tax);

            ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse =
                    restTemplate.exchange(CamundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_PROCESS_INSTANCE_RESOURCE_PATH), HttpMethod.POST, processInstanceRequestHttpEntity, CamundaProcessInstanceResponse[].class);

            if (CamundaApiUtils.isProcessInstanceIncidents(formModel, headers, restTemplate, processInstanceResponse)) {
                break;
            }

            HttpEntity<CamundaActivityInstanceRequest> activityInstanceRequestHttpEntity = CamundaApiUtils.prepareActivityInstanceRequestHttpEntity(headers, processInstanceResponse);

            ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse =
                    restTemplate.exchange(CamundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_ACTIVITY_RESOURCE_PATH), HttpMethod.POST, activityInstanceRequestHttpEntity, CamundaActivityInstanceResponse[].class);

            HttpEntity<CamundaProcessMigrationRequest> processInstanceMigrationRequestHttpEntity = prepareProcessInstanceMigrationRequestHttpEntity(headers, formModel, processInstanceResponse, activityInstanceResponse);

            ResponseEntity<CamundaProcessInstanceMigrationResponse> processInstanceMigrationResponse =
                    restTemplate.exchange(CamundaApiUtils.getUrl(formModel, CamundaApiRoutes.PROCESS_MIGRATION_RESOURCE_PATH), HttpMethod.POST, processInstanceMigrationRequestHttpEntity, CamundaProcessInstanceMigrationResponse.class);

            logResponse(formModel, processInstanceResponse, processInstanceMigrationResponse.getStatusCodeValue());
        }
    }

    public static HttpEntity<CamundaProcessMigrationRequest> prepareProcessInstanceMigrationRequestHttpEntity(HttpHeaders headers, FormModel formModel, ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse, ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse) {
        String processInstanceId = CamundaApiUtils.getObject(processInstanceResponse).getId();
        String processDefinitionId = CamundaApiUtils.getObject(processInstanceResponse).getProcessDefinitionId();
        CamundaProcessMigrationRequest.Instructions instructions = getInstructions(activityInstanceResponse);
        CamundaProcessMigrationRequest.MigrationPlan migrationPlan = CamundaProcessMigrationRequest.MigrationPlan.builder()
                .sourceProcessDefinitionId(processDefinitionId)
                .targetProcessDefinitionId(formModel.getTargetProcessDefinitionId())
                .instructions(Arrays.asList(instructions))
                .build();
        CamundaProcessMigrationRequest camundaProcessInstanceMigrationRequest = CamundaProcessMigrationRequest.builder()
                .migrationPlan(migrationPlan)
                .processInstanceIds(Arrays.asList(processInstanceId))
                .skipCustomListeners(true)
                .build();
        HttpEntity<CamundaProcessMigrationRequest> activityInstanceRequestHttpEntity = new HttpEntity<>(camundaProcessInstanceMigrationRequest, headers);
        return activityInstanceRequestHttpEntity;
    }

    private static CamundaProcessMigrationRequest.Instructions getInstructions(ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse) {
        List<String> sourceActivityIds = new LinkedList<>();
        sourceActivityIds.add(CamundaApiUtils.getObject(activityInstanceResponse).getActivityId());
        List<String> targetActivityIds = new LinkedList<>();
        targetActivityIds.add(CamundaApiUtils.getObject(activityInstanceResponse).getActivityId());
        CamundaProcessMigrationRequest.Instructions instructions = CamundaProcessMigrationRequest.Instructions.builder()
                .sourceActivityIds(sourceActivityIds)
                .targetActivityIds(targetActivityIds)
                .updateEventTrigger(false)
                .build();
        return instructions;
    }

    private void logResponse(FormModel formModel, ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse, Integer statusCode) {
        if (statusCode == 204) {
            log.info("Process instance={}:{} moved from definition={} to definition={}",
                    CamundaApiUtils.getObject(processInstanceResponse).getId(),
                    CamundaApiUtils.getObject(processInstanceResponse).getBusinessKey(),
                    CamundaApiUtils.getObject(processInstanceResponse).getProcessDefinitionId(),
                    formModel.getTargetProcessDefinitionId());
        }
    }
}
