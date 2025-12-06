package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.responses.ShipmentHistoryResponse;
import com.manhduc205.ezgear.dtos.responses.TrackingResponse;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.ShipmentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shipment-history")
@RequiredArgsConstructor
public class ShipmentHistoryController {

    private final ShipmentHistoryService historyService;

    @GetMapping("/{shipmentId}")
    public ResponseEntity<?> getHistory(@PathVariable Long shipmentId, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(historyService.getHistoryByShipmentId(shipmentId, user));
    }
    @GetMapping("/tracking/{orderId}")
    public ResponseEntity<TrackingResponse> getTracking(@PathVariable Long orderId, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(historyService.getTrackingDetails(orderId, user.getId()));
    }
}