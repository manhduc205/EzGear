package com.manhduc205.ezgear.shipping.controller;

import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.shipping.dto.request.ShippingAvailableServiceRequest;
import com.manhduc205.ezgear.shipping.dto.request.ShippingFeeRequest;
import com.manhduc205.ezgear.shipping.dto.response.GhnShippingFeeResponse;
import com.manhduc205.ezgear.shipping.service.ShippingFeeCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class GhnShippingFeeController {

    private final ShippingFeeCalculatorService shippingFeeService;

    @PostMapping("/fee")
    public ResponseEntity<ApiResponse<GhnShippingFeeResponse>> calculateFee(@RequestBody ShippingFeeRequest request) {
        try {
            Long branchId = request.getBranchId();
            Long addressId = request.getAddressId();
            List<CartItemRequest> cartItems = request.getCartItems();
            Integer serviceId = request.getServiceId();

            if (branchId == null || addressId == null || cartItems == null || cartItems.isEmpty() || serviceId == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.<GhnShippingFeeResponse>builder()
                                .success(false)
                                .message("Missing fields: branchId, addressId, cartItems or serviceId")
                                .build()
                );
            }

            GhnShippingFeeResponse response =
                    shippingFeeService.calculateShippingFee(branchId, addressId, cartItems, serviceId);

            return ResponseEntity.ok(
                    ApiResponse.<GhnShippingFeeResponse>builder()
                            .success(true)
                            .message("Shipping fee calculated successfully")
                            .payload(response)
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<GhnShippingFeeResponse>builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/available-services")
    public ResponseEntity<ApiResponse<?>> getServices(@RequestBody ShippingAvailableServiceRequest req) {
        try {
            Long branchId = req.getBranchId();
            Long addressId = req.getAddressId();

            if (branchId == null || addressId == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .success(false)
                                .message("Missing fields: branchId, addressId")
                                .build()
                );
            }

            var res = shippingFeeService.getAvailableServices(branchId, addressId);

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Available services fetched successfully")
                            .payload(res)
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );
        }
    }
}
