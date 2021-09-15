package camunda.processmodificator.service.impl;

import camunda.processmodificator.dto.request.CamundaActivityInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceModificationRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceRequest;
import camunda.processmodificator.dto.response.CamundaActivityInstanceResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceModificationResponse;
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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProcessModificationRestService implements CamundaRestService {

    private RestTemplate restTemplate;
    private CamundaApiUtils camundaApiUtils;

    public ProcessModificationRestService(RestTemplate restTemplate, CamundaApiUtils camundaApiUtils) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        this.restTemplate = restTemplate;
        this.camundaApiUtils = camundaApiUtils;
    }

    public void send(FormModel formModel) {
        HttpHeaders headers = new HttpHeaders();

        camundaApiUtils.authenticate(headers, formModel);

        for (String[] tax : formModel.getTaxIDs()) {
            HttpEntity<CamundaProcessInstanceRequest> processInstanceRequestHttpEntity = CamundaApiUtils.prepareProcessInstanceRequestHttpEntity(headers, tax);

            ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse =
                    restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_PROCESS_INSTANCE_RESOURCE_PATH), HttpMethod.POST, processInstanceRequestHttpEntity, CamundaProcessInstanceResponse[].class);


            if (camundaApiUtils.getObject(processInstanceResponse).isPresent()) {
                CamundaProcessInstanceResponse processInstance = camundaApiUtils.getObject(processInstanceResponse).get();

                if (camundaApiUtils.isProcessInstanceIncidents(formModel, headers, restTemplate, processInstance)) {
                    break;
                }

                HttpEntity<CamundaActivityInstanceRequest> activityInstanceRequestHttpEntity = camundaApiUtils.prepareActivityInstanceRequestHttpEntity(headers, processInstanceResponse);

                ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse =
                        restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_ACTIVITY_RESOURCE_PATH), HttpMethod.POST, activityInstanceRequestHttpEntity, CamundaActivityInstanceResponse[].class);

                CamundaActivityInstanceResponse camundaActivityInstance = camundaApiUtils.getObject(activityInstanceResponse).get();

                HttpEntity<CamundaProcessInstanceModificationRequest> camundaProcessInstanceModificationRequestHttpEntity = prepareProcessInstanceModificationRequestHttpEntity(formModel, headers, activityInstanceResponse);

                String url = camundaApiUtils.getUrl(formModel, constructProcessInstanceModificationPath(processInstanceResponse));

                ResponseEntity<CamundaProcessInstanceModificationResponse> camundaProcessInstanceModificationResponseResponse =
                        restTemplate.exchange(url, HttpMethod.POST, camundaProcessInstanceModificationRequestHttpEntity, CamundaProcessInstanceModificationResponse.class);

                logResponse(formModel, processInstance, camundaActivityInstance, camundaProcessInstanceModificationResponseResponse.getStatusCodeValue());
            }
        }
    }

    private String constructProcessInstanceModificationPath(ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse) {
        return CamundaApiRoutes.PROCESS_INSTANCE_RESOURCE_PATH + camundaApiUtils.getObject(processInstanceResponse).get().getId() + "/modification";
    }

    private HttpEntity<CamundaProcessInstanceModificationRequest> prepareProcessInstanceModificationRequestHttpEntity(FormModel formModel, HttpHeaders headers, ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse) {
        List<Map<String, String>> instructions = getInstruction(formModel, activityInstanceResponse);
        CamundaProcessInstanceModificationRequest camundaProcessInstanceModificationRequest = CamundaProcessInstanceModificationRequest.builder()
                .skipCustomListeners(false)
                .skipIoMappings(false)
                .instructions(instructions)
                .build();
        HttpEntity<CamundaProcessInstanceModificationRequest> camundaProcessInstanceModificationRequestHttpEntity = new HttpEntity<>(camundaProcessInstanceModificationRequest, headers);
        return camundaProcessInstanceModificationRequestHttpEntity;
    }

    private List<Map<String, String>> getInstruction(FormModel formModel, ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse) {
        Map<String, String> currentActivity = new HashMap<>();
        currentActivity.put("type", "cancel");
        currentActivity.put("activityId", camundaApiUtils.getObject(activityInstanceResponse).get().getActivityId());
        Map<String, String> finalActivity = new HashMap<>();
        finalActivity.put("type", formModel.getTargetActivityPosition());
        finalActivity.put("activityId", formModel.getTargetActivityID());
        return Arrays.asList(currentActivity, finalActivity);
    }

    private void logResponse(FormModel formModel, CamundaProcessInstanceResponse processInstanceResponse, CamundaActivityInstanceResponse activityInstanceResponse, Integer statusCode) {
        if (statusCode == 204) {
            log.info("Token of process={}:{} moved from activity={}:{} on activity id={}",
                    processInstanceResponse.getId(),
                    processInstanceResponse.getBusinessKey(),
                    activityInstanceResponse.getActivityId(),
                    activityInstanceResponse.getActivityName(),
                    formModel.getTargetActivityID());
        }
    }
}
