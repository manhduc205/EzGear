package com.manhduc205.ezgear.shipping.controller;

import com.manhduc205.ezgear.shipping.dto.request.ShippingFeeRequest;
import com.manhduc205.ezgear.shipping.dto.response.GhnShippingFeeResponse;
import com.manhduc205.ezgear.shipping.service.ShippingFeeCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("ghn/shipping/fee")
@RequiredArgsConstructor
public class GhnShippingFeeController {

    private final ShippingFeeCalculatorService shippingFeeService;
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateFee(@RequestBody ShippingFeeRequest request) {
        try {
            Long branchId = request.getBranchId();
            Long addressId = request.getAddressId();
            Long skuId = request.getSkuId();

            if (branchId == null || addressId == null || skuId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "code", 400,
                        "message", "Missing required fields: branchId, addressId, skuId",
                        "data", null
                ));
            }

            GhnShippingFeeResponse response =
                    shippingFeeService.calculateShippingFee(branchId, addressId, skuId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", 500,
                    "message", e.getMessage(),
                    "data", null
            ));
        }
    }
}
