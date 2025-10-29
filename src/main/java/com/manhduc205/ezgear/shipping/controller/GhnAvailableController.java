package com.manhduc205.ezgear.shipping.controller;

import com.manhduc205.ezgear.shipping.dto.request.GhnAvailableServiceRequest;
import com.manhduc205.ezgear.shipping.dto.response.GhnAvailableServiceResponse;
import com.manhduc205.ezgear.shipping.service.GhnAvailableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ghn")
@RequiredArgsConstructor
public class GhnAvailableController {
    private final GhnAvailableService ghnAvailableService;

    @PostMapping("/available-services")
    public ResponseEntity<GhnAvailableServiceResponse> getAvailableServices(@RequestBody GhnAvailableServiceRequest request){
        log.info(">>> [GHN] Request available services: {}", request);

        GhnAvailableServiceResponse response = ghnAvailableService.getAvailableServices(request);

        return ResponseEntity.ok(response);
    }
}
