package com.manhduc205.ezgear.controllers;

import com.google.firebase.messaging.FirebaseMessaging;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribeToOrderTopic(@RequestBody String fcmToken) {
        try {
            // Frontend đôi khi gửi lên dạng thô có cả dấu ngoặc kép, cần lọc sạch
            String cleanToken = fcmToken;
            if (cleanToken.startsWith("\"") && cleanToken.endsWith("\"")) {
                cleanToken = cleanToken.substring(1, cleanToken.length() - 1);
            }

            log.info("Đang đăng ký Token vào topic admin_orders: {}", cleanToken);

            FirebaseMessaging.getInstance().subscribeToTopic(
                    Collections.singletonList(cleanToken),
                    "admin_orders"
            );

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Đã đăng ký nhận thông báo đơn hàng thành công.")
                    .build());
        } catch (Exception e) {
            log.error("Lỗi đăng ký FCM: ", e);
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Lỗi đăng ký FCM: " + e.getMessage())
                    .build());
        }
    }
}