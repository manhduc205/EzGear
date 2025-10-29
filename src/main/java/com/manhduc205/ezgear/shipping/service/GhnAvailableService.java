package com.manhduc205.ezgear.shipping.service;

import com.manhduc205.ezgear.shipping.client.GhnRestClient;
import com.manhduc205.ezgear.shipping.dto.request.GhnAvailableServiceRequest;
import com.manhduc205.ezgear.shipping.dto.response.GhnAvailableServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GhnAvailableService {
    private final GhnRestClient client;

    public GhnAvailableServiceResponse getAvailableServices(GhnAvailableServiceRequest req) {
        return client.post("/v2/shipping-order/available-services",req, GhnAvailableServiceResponse.class);
    }
}
