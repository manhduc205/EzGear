package com.manhduc205.ezgear.services;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FCMService {

    // @Async để chạy ngầm, không bắt khách hàng phải chờ gửi thông báo xong mới nhận được response
    @Async
    public void sendOrderNotification(String orderCode, Long orderId, Long grandTotal) {
        try {
            String formattedPrice = String.format("%,d", grandTotal);

            Notification notification = Notification.builder()
                    .setTitle("🎉 Có đơn hàng mới!")
                    .setBody("Đơn " + orderCode + " trị giá " + formattedPrice + " đ vừa được đặt.")
                    .build();

            Message message = Message.builder()
                    .setNotification(notification)
                    .putData("orderId", orderId.toString())
                    .putData("orderCode", orderCode)
                    .putData("type", "NEW_ORDER")
                    .setTopic("admin_orders")
                    .build();

            // Gửi đi
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM Sent message ID: " + response);

        } catch (Exception e) {
            // Chỉ log lỗi, không throw exception để tránh rollback đơn hàng
            log.error("FCM Error: " + e.getMessage());
        }
    }
}