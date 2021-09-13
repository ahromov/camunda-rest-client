package camunda.processmodificator.service.utils;

import camunda.processmodificator.dto.request.CamundaActivityInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceRequest;
import camunda.processmodificator.dto.response.CamundaProcessIncidentsCountResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceResponse;
import camunda.processmodificator.model.FormModel;
import camunda.processmodificator.service.routes.CamundaApiRoutes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CamundaApiUtils {

    public static void authenticate(HttpHeaders headers, FormModel formModel) {
        if (formModel.getEngineLogin() != null && formModel.getEnginePassword() != null) {
            headers.setBasicAuth(formModel.getEngineLogin(), formModel.getEnginePassword());
        }
    }

    public static String getUrl(FormModel formModel, String path) {
        return formModel.getServerAddress() + path;
    }

    public static HttpEntity<CamundaProcessInstanceRequest> prepareProcessInstanceRequestHttpEntity(HttpHeaders headers, String[] tax) {
        CamundaProcessInstanceRequest requestBody = CamundaProcessInstanceRequest.builder()
                .processInstanceBusinessKeyLike(tax[0])
                .processDefinitionKey("CreditConveyorSmallBusiness")
                .finished(false)
                .build();
        return new HttpEntity<>(requestBody, headers);
    }

    public static Boolean isProcessInstanceIncidents(FormModel formModel, HttpHeaders headers, RestTemplate restTemplate, ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse) {
        String processInstanceId = getObject(processInstanceResponse).getId();
        ResponseEntity<CamundaProcessIncidentsCountResponse> processInstanceIncidentsCountResponse =
                restTemplate.exchange(getUrl(formModel, CamundaApiRoutes.PROCESS_INSTANCE_INCIDENTS_COUNT + processInstanceId), HttpMethod.GET, new HttpEntity(headers), CamundaProcessIncidentsCountResponse.class);
        int incidentsCount = processInstanceIncidentsCountResponse.getBody().getCount().intValue();
        if (incidentsCount > 0) {
            log.info("Process instance {}:{} has a {} incidents and was be skipped", processInstanceId, getObject(processInstanceResponse).getBusinessKey(), incidentsCount);
            return true;
        }
        return false;
    }

    public static HttpEntity<CamundaActivityInstanceRequest> prepareActivityInstanceRequestHttpEntity(HttpHeaders headers, ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse) {
        List<Map<String, String>> sortingParams = getSortingParams();
        String id = getObject(processInstanceResponse).getId();
        CamundaActivityInstanceRequest camundaActivityInstanceRequest = CamundaActivityInstanceRequest.builder()
                .processInstanceId(id)
                .sorting(sortingParams)
                .build();
        HttpEntity<CamundaActivityInstanceRequest> activityInstanceRequestHttpEntity = new HttpEntity<>(camundaActivityInstanceRequest, headers);
        return activityInstanceRequestHttpEntity;
    }

    private static List<Map<String, String>> getSortingParams() {
        Map<String, String> sortBy = new HashMap<>();
        sortBy.put("sortBy", "endTime");
        Map<String, String> sortOrder = new HashMap<>();
        sortOrder.put("sortOrder", "desc");
        return Arrays.asList(sortBy, sortOrder);
    }

    public static <T> T getObject(ResponseEntity<T[]> processInstanceResponse) {
        return Arrays.stream(processInstanceResponse.getBody()).collect(Collectors.toList()).get(0);
    }
}
