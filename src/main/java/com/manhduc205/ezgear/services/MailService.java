package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.models.order.OrderItem;
import com.manhduc205.ezgear.services.UserService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final SendGrid sendGrid;
    private final UserService userService;

    @Value("${spring.sendgrid.from-email}")
    private String fromEmail;

    @Value("${spring.sendgrid.templateOrderSuccessfulId}")
    private String orderSuccessTemplateId;

    @Value("${spring.sendgrid.templateOrderShippingId}")
    private String deliverySuccessTemplateId;

    // chạy luồng riêng
    @Async
    public void sendOrderConfirmation(Order order) {
        try {
            String toEmail = userService.getUserEmail(order.getUserId());

            if ("default@example.com".equals(toEmail) || toEmail == null) {
                log.warn("Skip sending email: No valid email for User ID {}", order.getUserId());
                return;
            }

            Map<String, Object> dynamicData = buildOrderData(order);

            sendEmailSafe(toEmail, orderSuccessTemplateId, dynamicData);

        } catch (Exception e) {
            log.error("Could not send Order Confirmation Email for Order {}. Reason: {}",
                    order.getCode(), e.getMessage());
        }
    }

    @Async
    public void sendDeliverySuccess(Order order) {
        try {
            String toEmail = userService.getUserEmail(order.getUserId());
            if ("default@example.com".equals(toEmail) || toEmail == null) return;

            Map<String, Object> dynamicData = buildOrderData(order);
            /* TODO: link đánh giá sản phẩm - cần thay đổi theo domain thực tế */
            dynamicData.put("review_url", "https://ezgear.vn/user/orders/" + order.getCode()); // Ví dụ

            sendEmailSafe(toEmail, deliverySuccessTemplateId, dynamicData);

        } catch (Exception e) {
            log.error("⚠️ Ignored Error: Could not send Delivery Success Email. Reason: {}", e.getMessage());
        }
    }

    private void sendEmailSafe(String toEmail, String templateId, Map<String, Object> dynamicData) {
        try {
            Mail mail = new Mail();
            mail.setFrom(new Email(fromEmail, "EzGear Shop"));
            mail.setTemplateId(templateId);

            Personalization personalization = new Personalization();
            personalization.addTo(new Email(toEmail));

            for (Map.Entry<String, Object> entry : dynamicData.entrySet()) {
                personalization.addDynamicTemplateData(entry.getKey(), entry.getValue());
            }
            mail.addPersonalization(personalization);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 400) {
                log.warn("SendGrid Warning: Email failed with status {}. Body: {}", response.getStatusCode(), response.getBody());
            } else {
                log.info("Email sent to {}", toEmail);
            }

        } catch (IOException ex) {
            log.error("SendGrid IO Error: {}", ex.getMessage());
        }
    }

    private Map<String, Object> buildOrderData(Order order) {
        Map<String, Object> data = new HashMap<>();

        try {
            data.put("customer_name", order.getShippingAddress() != null ? order.getShippingAddress().getReceiverName() : "Khách hàng");
            data.put("order_code", order.getCode());
            data.put("order_date", order.getCreatedAt() != null ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
            data.put("payment_method", order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "");
            data.put("shipping_address", order.getShippingAddress() != null ? order.getShippingAddress().getAddressLine() : "");

            data.put("subtotal", String.format("%,d", order.getSubtotal()));
            data.put("shipping_fee", String.format("%,d", order.getShippingFee()));
            data.put("discount", String.format("%,d", order.getDiscountTotal()));
            data.put("grand_total", String.format("%,d", order.getGrandTotal()));

            List<Map<String, Object>> items = new ArrayList<>();
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("productName", item.getProductNameSnapshot());
                    itemData.put("skuName", item.getSkuNameSnapshot());
                    itemData.put("imageUrl", item.getImageUrlSnapshot());
                    itemData.put("quantity", item.getQuantity());
                    itemData.put("price", String.format("%,d", item.getUnitPrice()));
                    items.add(itemData);
                }
            }
            data.put("items", items);
        } catch (Exception e) {
            log.warn("Error building email data: {}", e.getMessage());
        }
        return data;
    }
}