package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.models.Shipment;
import com.manhduc205.ezgear.services.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shipments")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentService shipmentService;

    /**
     * API: Tạo đơn vận chuyển mới (Đẩy đơn sang GHN)
     * gọi sau khi kho đã đóng gói hàng xong.
     */
    @PostMapping("/create-ghn-order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    public ResponseEntity<?> createGhnOrder(@PathVariable Long orderId) {
        try {
            Shipment shipment = shipmentService.createShipment(orderId);
            return ResponseEntity.ok(shipment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Tạo vận đơn thất bại: " + e.getMessage());
        }
    }

}