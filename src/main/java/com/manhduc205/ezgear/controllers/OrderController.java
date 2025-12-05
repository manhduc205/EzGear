package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.request.order.CreateOrderRequest;
import com.manhduc205.ezgear.dtos.responses.order.OrderListResponse;
import com.manhduc205.ezgear.dtos.responses.order.OrderPlacementResponse;
import com.manhduc205.ezgear.dtos.responses.order.OrderResponse;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @GetMapping("/{orderCode}")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable String orderCode, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(orderService.getOrderDetail(user.getId(), orderCode));
    }
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderListResponse>> getMyOrders(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(orderService.getMyOrders(user.getId()));
    }
}