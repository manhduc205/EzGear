package com.manhduc205.ezgear.shipping.controller;

import com.manhduc205.ezgear.shipping.dto.request.ShippingAvailableServiceRequest;
import com.manhduc205.ezgear.shipping.dto.request.ShippingFeeRequest;
import com.manhduc205.ezgear.shipping.dto.response.GhnShippingFeeResponse;
import com.manhduc205.ezgear.shipping.service.ShippingFeeCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                return ResponseEntity.badRequest().body(Map.of(
                        "code", 400,
                        "message", "Missing fields: branchId, addressId, skuId, serviceId",
                        "data", null
                ));
            }

            GhnShippingFeeResponse response =
                    shippingFeeService.calculateShippingFee(branchId, addressId, skuId, serviceId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", 500,
                    "message", e.getMessage(),
                    "data", null
            ));
        }
    }
    @PostMapping("/available-services")
    public ResponseEntity<?> getServices(@RequestBody ShippingAvailableServiceRequest req) {
        try {
            Long branchId = req.getBranchId();
            Long addressId = req.getAddressId();

            if (branchId == null || addressId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "code", 400,
                        "message", "Missing fields: branchId, addressId",
                        "data", null
                ));
            }

            var res = shippingFeeService.getAvailableServices(branchId, addressId);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", 500,
                    "message", e.getMessage(),
                    "data", null
            ));
        }
    }

}
