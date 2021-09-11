package camunda.processmodificator.service.utils;

import camunda.processmodificator.dto.request.CamundaActivityInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceRequest;
import camunda.processmodificator.dto.response.CamundaProcessInstanceResponse;
import camunda.processmodificator.model.FormModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CamundaApiUtils {

    public static void authenticate(HttpHeaders headers, FormModel formModel) {
        if (formModel.getEngineLogin() != null && formModel.getEnginePassword() != null) {
            headers.setBasicAuth(formModel.getEngineLogin(), formModel.getEnginePassword());
        }
    }

    public static String getUrl(FormModel formModel, String path) {
        return formModel.getServerAddress() + path;
    }

    public static HttpEntity<CamundaProcessInstanceRequest> prepareProcessInstanceRequestHttpEntity(HttpHeaders headers, String[] tax, FormModel formModel) {
        CamundaProcessInstanceRequest requestBody = CamundaProcessInstanceRequest.builder()
                .processInstanceBusinessKeyLike(tax[0])
                .processDefinitionKey(formModel.getProcessDefinitionKey())
                .finished(false)
                .build();
        return new HttpEntity<>(requestBody, headers);
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
        return List.of(sortBy, sortOrder);
    }

    public static <T> T getObject(ResponseEntity<T[]> processInstanceResponse) {
        return Arrays.stream(processInstanceResponse.getBody()).collect(Collectors.toList()).get(0);
    }
}
