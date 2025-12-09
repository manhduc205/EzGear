package com.manhduc205.ezgear.controllers;


import com.manhduc205.ezgear.dtos.PurchaseOrderDTO;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.models.PurchaseOrder;
import com.manhduc205.ezgear.services.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/purchase-orders")
@PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping("")
    public ResponseEntity<?> createPO(@RequestBody PurchaseOrderDTO purchaseOrderDTO) {
        try {
            PurchaseOrderDTO created = purchaseOrderService.createOrder(purchaseOrderDTO);
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Purchase order created successfully")
                            .payload(created)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Error while creating purchase order")
                            .error(e.getMessage())
                            .build()
            );
        }
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmPO(@PathVariable Long id) {
        try {
            PurchaseOrderDTO confirmed = purchaseOrderService.confirmOrder(id);
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Purchase order confirmed successfully")
                            .payload(confirmed)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Error while confirming purchase order")
                            .error(e.getMessage())
                            .build()
            );
        }
    }

    @PutMapping("/{id}/receive")
    public ResponseEntity<?> receivePO(@PathVariable Long id) {
        try{
            PurchaseOrderDTO receive = purchaseOrderService.receiveOrder(id);
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Purchase order received successfully")
                            .payload(receive)
                            .build()

            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Error while receiving purchase order")
                            .error(e.getMessage())
                            .build()
            );
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelPO(@PathVariable Long id) {
        try {
            PurchaseOrderDTO cancelled = purchaseOrderService.cancelOrder(id);
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Purchase order cancelled successfully")
                            .payload(cancelled)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Error while cancelling purchase order")
                            .error(e.getMessage())
                            .build()
            );
        }
    }
    @GetMapping("")
    public ResponseEntity<?> getAllPOs() {
        try {
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Fetched all purchase orders successfully")
                            .payload(purchaseOrderService.getAll())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Error while fetching purchase orders")
                            .error(e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPurchaseOrderById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Fetched purchase order successfully")
                            .payload(purchaseOrderService.getById(id))
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Error while fetching purchase order")
                            .error(e.getMessage())
                            .build()
            );
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePO(
            @PathVariable Long id,
            @RequestBody PurchaseOrderDTO dto) {
        try {
            PurchaseOrderDTO updated = purchaseOrderService.updateOrder(id, dto);
            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("Purchase order updated successfully")
                            .payload(updated)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Error while updating purchase order")
                            .error(e.getMessage())
                            .build()
            );
        }
    }

}
