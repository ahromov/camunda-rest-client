package camunda.processmodificator.service.impl;

import camunda.processmodificator.dto.request.CamundaActivityInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceModificationRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceRequest;
import camunda.processmodificator.dto.response.CamundaActivityInstanceResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceModificationResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceResponse;
import camunda.processmodificator.model.BaseFormModel;
import camunda.processmodificator.model.MultipleModificateFormModel;
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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProcessMultipleModificationRestService implements CamundaRestService {

    private RestTemplate restTemplate;
    private CamundaApiUtils camundaApiUtils;

    public ProcessMultipleModificationRestService(RestTemplate restTemplate, CamundaApiUtils camundaApiUtils) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        this.restTemplate = restTemplate;
        this.camundaApiUtils = camundaApiUtils;
    }

    public void send(BaseFormModel formModel) {
        MultipleModificateFormModel modificationFormModel = (MultipleModificateFormModel) formModel;

        HttpHeaders headers = new HttpHeaders();

        camundaApiUtils.authenticate(headers, modificationFormModel);

        List<String[]> taxIDs = camundaApiUtils.parse(modificationFormModel.getTaxIDs());
        List<String[]> activityIDs = camundaApiUtils.parse(modificationFormModel.getActivityIDs());

        for (String[] tax : taxIDs) {
            HttpEntity<CamundaProcessInstanceRequest> processInstanceRequestHttpEntity = CamundaApiUtils.prepareProcessInstanceRequestHttpEntity(headers, tax, modificationFormModel);

            ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse =
                    restTemplate.exchange(camundaApiUtils.getUrl(modificationFormModel, CamundaApiRoutes.HISTORY_PROCESS_INSTANCE_RESOURCE_PATH), HttpMethod.POST, processInstanceRequestHttpEntity, CamundaProcessInstanceResponse[].class);

            if (camundaApiUtils.getObject(processInstanceResponse).isPresent()) {
                CamundaProcessInstanceResponse processInstance = camundaApiUtils.getObject(processInstanceResponse).get();

                if (camundaApiUtils.isProcessInstanceIncidents(modificationFormModel, headers, restTemplate, processInstance)) {
                    continue;
                }

                HttpEntity<CamundaActivityInstanceRequest> activityInstanceRequestHttpEntity = camundaApiUtils.prepareActivityInstanceRequestHttpEntity(headers, processInstanceResponse);

                ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse =
                        restTemplate.exchange(camundaApiUtils.getUrl(modificationFormModel, CamundaApiRoutes.HISTORY_ACTIVITY_RESOURCE_PATH), HttpMethod.POST, activityInstanceRequestHttpEntity, CamundaActivityInstanceResponse[].class);

                CamundaActivityInstanceResponse camundaActivityInstance = camundaApiUtils.getObject(activityInstanceResponse).get();

                String targetActivity = checkTargetActivity(processInstance, activityIDs, camundaActivityInstance);
                if (targetActivity == null) continue;

                HttpEntity<CamundaProcessInstanceModificationRequest> camundaProcessInstanceModificationRequestHttpEntity = prepareProcessInstanceModificationRequestHttpEntity(modificationFormModel, headers, activityInstanceResponse, targetActivity);

                String url = camundaApiUtils.getUrl(modificationFormModel, constructProcessInstanceModificationPath(processInstanceResponse));

                ResponseEntity<CamundaProcessInstanceModificationResponse> camundaProcessInstanceModificationResponseResponse =
                        restTemplate.exchange(url, HttpMethod.POST, camundaProcessInstanceModificationRequestHttpEntity, CamundaProcessInstanceModificationResponse.class);

                logResponse(processInstance, camundaActivityInstance, camundaProcessInstanceModificationResponseResponse.getStatusCodeValue(), targetActivity);
            }
        }
    }

    private String constructProcessInstanceModificationPath(ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse) {
        return CamundaApiRoutes.PROCESS_INSTANCE_RESOURCE_PATH + camundaApiUtils.getObject(processInstanceResponse).get().getId() + "/modification";
    }

    private HttpEntity<CamundaProcessInstanceModificationRequest> prepareProcessInstanceModificationRequestHttpEntity(MultipleModificateFormModel formModel, HttpHeaders headers, ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse, String targetActivity) {
        List<Map<String, String>> instructions = getInstruction(formModel, activityInstanceResponse, targetActivity);
        CamundaProcessInstanceModificationRequest camundaProcessInstanceModificationRequest = CamundaProcessInstanceModificationRequest.builder()
                .skipCustomListeners(false)
                .skipIoMappings(false)
                .instructions(instructions)
                .build();
        HttpEntity<CamundaProcessInstanceModificationRequest> camundaProcessInstanceModificationRequestHttpEntity = new HttpEntity<>(camundaProcessInstanceModificationRequest, headers);
        return camundaProcessInstanceModificationRequestHttpEntity;
    }

    private List<Map<String, String>> getInstruction(MultipleModificateFormModel formModel, ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse, String targetActivity) {
        Map<String, String> currentActivity = new HashMap<>();
        currentActivity.put("type", "cancel");
        currentActivity.put("activityId", camundaApiUtils.getObject(activityInstanceResponse).get().getActivityId());
        Map<String, String> finalActivity = new HashMap<>();
        finalActivity.put("type", formModel.getTargetActivityPosition());
        finalActivity.put("activityId", targetActivity);
        return Arrays.asList(currentActivity, finalActivity);
    }

    private void logResponse(CamundaProcessInstanceResponse processInstanceResponse, CamundaActivityInstanceResponse activityInstanceResponse, Integer statusCode, String targetActivity) {
        if (statusCode == 204) {
            log.info("Token of process={}:{} moved from activity={}:{} on activity id={}",
                    processInstanceResponse.getId(),
                    processInstanceResponse.getBusinessKey(),
                    activityInstanceResponse.getActivityId(),
                    activityInstanceResponse.getActivityName(),
                    targetActivity);
        }
    }

    private String checkTargetActivity(CamundaProcessInstanceResponse processInstance, List<String[]> activityIDs, CamundaActivityInstanceResponse camundaActivityInstance) {
        String targetActivity = getTargetActivity(activityIDs, camundaActivityInstance);
        if (targetActivity == null) {
            log.info("Process instance {}:{} was skipped, beacause his position are on another activity", processInstance.getId(), processInstance.getBusinessKey());
        }
        return targetActivity;
    }

    private String getTargetActivity(List<String[]> activityIDs, CamundaActivityInstanceResponse camundaActivityInstance) {
        String targetActivity = null;
        for (String [] activity: activityIDs) {
            if (camundaActivityInstance.getActivityId().equals(activity[0])){
                targetActivity = activity[1];
            }
        }
        return targetActivity;
    }
}
