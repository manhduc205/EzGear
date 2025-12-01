package com.manhduc205.ezgear.shipping.controller;

import com.manhduc205.ezgear.services.ShipmentHistoryService;
import com.manhduc205.ezgear.shipping.dto.request.GhnWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/ghn")
@RequiredArgsConstructor
@Slf4j
public class GhnWebhookController {

    private final ShipmentHistoryService historyService;

    /** TODO cần liên hệ GHN để kích hoạt Webhook vói url chính
     * Endpoint nhận Webhook từ GHN
     * URL Public: https://your-domain.com/api/webhook/ghn
     * vào trang quản trị GHN  Test , cấu hình Webhook, dán URL này vào.
     */

    @PostMapping
    public ResponseEntity<String> handleGhnWebhook(@RequestBody GhnWebhookRequest request) {
        log.info(">>> Received GHN Webhook: {}", request);
        try {
            historyService.processWebhook(request);

            // trả về 200 OK, nếu không GHN sẽ gửi lại liên tục
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing GHN webhook", e);
            // Vẫn trả về 200 để GHN không spam retryy
            return ResponseEntity.ok("Error logged");
        }
    }
}
