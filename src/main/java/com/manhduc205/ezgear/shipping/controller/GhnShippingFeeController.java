package com.manhduc205.ezgear.shipping.controller;

import com.manhduc205.ezgear.shipping.dto.request.ShippingAvailableServiceRequest;
import com.manhduc205.ezgear.shipping.dto.request.ShippingFeeRequest;
import com.manhduc205.ezgear.shipping.dto.response.GhnShippingFeeResponse;
import com.manhduc205.ezgear.shipping.service.ShippingFeeCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class GhnShippingFeeController {

    private final ShippingFeeCalculatorService shippingFeeService;

    @PostMapping("/fee")
    public ResponseEntity<?> calculateFee(@RequestBody ShippingFeeRequest request) {
        try {
            Long branchId = request.getBranchId();
            Long addressId = request.getAddressId();
            Long skuId = request.getSkuId();
            Integer serviceId = request.getServiceId();

            if (branchId == null || addressId == null || skuId == null || serviceId == null) {

                Map<String, Object> body = new HashMap<>();
                body.put("code", 400);
                body.put("message", "Missing fields: branchId, addressId, skuId, serviceId");
                body.put("data", null);

                return ResponseEntity.badRequest().body(body);
            }

            GhnShippingFeeResponse response =
                    shippingFeeService.calculateShippingFee(branchId, addressId, skuId, serviceId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            Map<String, Object> body = new HashMap<>();
            body.put("code", 500);
            body.put("message", e.getMessage());
            body.put("data", null);

            return ResponseEntity.internalServerError().body(body);
        }
    }

    @PostMapping("/available-services")
    public ResponseEntity<?> getServices(@RequestBody ShippingAvailableServiceRequest req) {
        try {
            Long branchId = req.getBranchId();
            Long addressId = req.getAddressId();

            if (branchId == null || addressId == null) {

                Map<String, Object> body = new HashMap<>();
                body.put("code", 400);
                body.put("message", "Missing fields: branchId, addressId");
                body.put("data", null);

                return ResponseEntity.badRequest().body(body);
            }

            var res = shippingFeeService.getAvailableServices(branchId, addressId);
            return ResponseEntity.ok(res);

        } catch (Exception e) {

            Map<String, Object> body = new HashMap<>();
            body.put("code", 500);
            body.put("message", e.getMessage());
            body.put("data", null);

            return ResponseEntity.internalServerError().body(body);
        }
    }
}
