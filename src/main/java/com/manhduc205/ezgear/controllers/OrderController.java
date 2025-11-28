package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.order.CreateOrderRequest;
import com.manhduc205.ezgear.dtos.responses.order.OrderPlacementResponse;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<OrderPlacementResponse> placeOrder(@RequestBody @Valid CreateOrderRequest req, @AuthenticationPrincipal CustomUserDetails user, HttpServletRequest httpServletRequest) {
        Long userId = user.getId();
        OrderPlacementResponse response = orderService.createOrder(
                req,
                userId,
                req.getPaymentMethod(),
                httpServletRequest
        );

        return ResponseEntity.ok(response);
    }
}