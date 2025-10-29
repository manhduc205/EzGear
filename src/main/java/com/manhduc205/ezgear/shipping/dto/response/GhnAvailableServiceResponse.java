package com.manhduc205.ezgear.shipping.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class GhnAvailableServiceResponse {
    private int code;
    private List<ServiceData> data;

    @Data
    public static class ServiceData {
        @JsonProperty("service_id")
        private int serviceId;

        @JsonProperty("short_name")
        private String shortName;

        @JsonProperty("service_type_id")
        private int serviceTypeId;
    }
}
