package com.manhduc205.ezgear.shipping.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AvailableServiceResponse {
    private Integer defaultServiceId;
    private List<GhnAvailableServiceResponse.ServiceData> services;
}
