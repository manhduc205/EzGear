package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.ProductPaymentRequest;
import com.manhduc205.ezgear.dtos.responses.VNPayResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.models.order.Payment;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.PaymentRepository;
import com.manhduc205.ezgear.services.MailService;
import com.manhduc205.ezgear.services.PaymentService;
import com.manhduc205.ezgear.services.ProductStockService;
import com.manhduc205.ezgear.services.WarehouseService;
import com.manhduc205.ezgear.utils.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductStockService productStockService;
    private final WarehouseService warehouseService;
    private final MailService mailService;
    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.payUrl}")
    private String payUrl;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;


    @Override
    @Transactional
    public VNPayResponse createPaymentVNPay(ProductPaymentRequest req) {
        Order order = orderRepository.findByCode(req.getOrderCode())
                .orElseThrow(() -> new RequestException("Order not found: " + req.getOrderCode()));

        // ktra nếu đã thanh toán rồi thì chặn
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new RequestException("Order already paid");
        }

        // Lấy số tiền từ Database (Bảo mật: Không tin tưởng client gửi lên)
        if (order.getGrandTotal() == null) {
            throw new RequestException("Order has no grand total");
        }
        long amountVnd = order.getGrandTotal();
        BigDecimal amountBigDecimal = BigDecimal.valueOf(amountVnd);

        //Tạo bản ghi Payment với trạng thái PENDING
        String vnpTxnRef = "VNP" + System.currentTimeMillis();
        Payment payment = Payment.builder()
                .order(order)
                .method("VNPAY")
                .amount(amountBigDecimal)
                .status("PENDING")
                .vnpTxnRef(vnpTxnRef)
                .build();
        paymentRepository.save(payment);

        //tham số gửi sang VNPay
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", tmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountVnd * 100)); // VNPay yêu cầu nhân 100 (đơn vị cct)
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnpTxnRef);
        vnp_Params.put("vnp_OrderInfo", order.getCode());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", req.getIpAddr() != null ? req.getIpAddr() : "127.0.0.1");

        // Thời gian tạo
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Thời gian hết hạn (15 phút)
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // chuỗi hash và URL thanh toán
        // Sắp xếp params theo alphabet để hash đúng chuẩn VNPay
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        try {
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error building VNPay URL params", e);
            throw new RequestException("Lỗi khi tạo URL thanh toán");
        }

        String queryUrl = query.toString();
        // tạo chữ ký bảo mật (HMAC SHA512)
        String vnp_SecureHash = VnPayUtil.hmacSHA512(hashSecret, hashData.toString());
        String paymentUrl = payUrl + "?" + queryUrl + "&vnp_SecureHash=" + vnp_SecureHash;

        //Cập nhật lại Order (đảm bảo trạng thái đúng)
        order.setPaymentStatus("PENDING");
        orderRepository.save(order);

        log.info("VNPay URL created for Order: {}, TxnRef: {}", order.getCode(), vnpTxnRef);
        return new VNPayResponse(paymentUrl);
    }

    @Override
    public String handleVnPayCallback(HttpServletRequest request) {
        // lưu tham số từ request về Map
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                try {
                    fields.put(fieldName, fieldValue);
                } catch (Exception e) {
                    log.error("Error reading param: " + fieldName, e);
                }
            }
        }

        //Tách hash ra khỏi map để verify
        String vnp_SecureHash = fields.get("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }

        // Cần sort lại y hệt lúc tạo
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    hashData.append(fieldName);
                    hashData.append('=');
                    // Chú ý: VNPay trả về dữ liệu đã encode hay chưa?
                    // Thường HttpServletRequest.getParameter() đã decode.
                    // Để verify đúng, ta cần URLEncoder.encode lại theo logic VNPayUtil.
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                } catch (Exception e) {
                    log.error("Error encoding param for hash check", e);
                }
            }
        }

        String calculatedHash = VnPayUtil.hmacSHA512(hashSecret, hashData.toString());

        // So sánh checksum
        if (!calculatedHash.equals(vnp_SecureHash)) {
            log.error("Invalid Checksum! Received: {}, Calculated: {}", vnp_SecureHash, calculatedHash);
            return "INVALID_SIGNATURE";
        }

        // thông tin giao dịch
        String txnRef = fields.get("vnp_TxnRef");
        String responseCode = fields.get("vnp_ResponseCode");
        String amountStr = fields.get("vnp_Amount");
        String transactionNo = fields.get("vnp_TransactionNo");

        // Tìm Payment trong DB
        Payment payment = paymentRepository.findByVnpTxnRef(txnRef)
                .orElseThrow(() -> new RequestException("Payment transaction not found: " + txnRef));

        Order order = payment.getOrder();

        //
        if ("PAID".equalsIgnoreCase(payment.getStatus()) || "SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            log.info("Payment {} already processed.", txnRef);
            return "OK";
        }

        // Kiểm tra số tiền
        if (amountStr != null) {
            long vnpAmount = Long.parseLong(amountStr) / 100;
            if (vnpAmount != order.getGrandTotal()) {
                log.error("Amount Mismatch! Order: {}, VNPAY: {}", order.getGrandTotal(), vnpAmount);
                // Đánh dấu nghi vấn
                payment.setStatus("FRAUD_SUSPECTED");
                payment.setRawPayload(fields.toString());
                paymentRepository.save(payment);

                // Hủy đơn vì sai tiền
                order.setStatus("CANCELLED");
                order.setPaymentStatus("FAILED");
                orderRepository.save(order);

                // Nhả kho
                try {
                    productStockService.releaseReservation(order.getCode());
                } catch (Exception e) {
                    log.error("Failed to release reservation for fraud order {}", order.getCode());
                }
                return "AMOUNT_MISMATCH";
            }
        }

        // Xử lý kết quả giao dịch
        if ("00".equals(responseCode)) {
            // --- THANH TOÁN THÀNH CÔNG ---
            log.info("Payment Success for Order: {}", order.getCode());

            //Cập nhật Payment
            payment.setStatus("PAID");
            payment.setProviderTxnId(transactionNo);
            payment.setRawPayload(fields.toString());
            paymentRepository.save(payment);

            // Cập nhật Order
            order.setPaymentStatus("PAID");
            order.setStatus("PENDING_SHIPMENT");
            orderRepository.save(order);

            // CHỐT KHO (COMMIT RESERVATION)
            try {
                if (productStockService.hasReservation(order.getCode())) {
                    productStockService.commitReservation(order.getCode());
                } else {
                    // Fallback: Trừ trực tiếp nếu mất reservation
                    log.warn("Lost reservation for {}. Trying direct reduce.", order.getCode());

                    // Tìm đúng Warehouse ID
                    Long warehouseId = warehouseService.getWarehouseIdByBranch(order.getBranchId());

                    for(var item : order.getItems()) {
                        productStockService.reduceStockDirect(item.getSkuId(), warehouseId, item.getQuantity());
                    }
                }
            } catch (Exception e) {
                // Log lỗi nhưng vẫn trả về OK để VNPay không gọi lại
                log.error("STOCK ERROR: Failed to commit stock for order {}", order.getCode(), e);
            }
            try {
                mailService.sendOrderConfirmation(order);
            } catch (Exception e) {
                log.error("Không thể gửi email xác nhận sau khi thanh toán VNPay", e);
            }

            return "OK";
        } else {
            // --- THANH TOÁN THẤT BẠI / HỦY BỎ ---
            log.info("Payment Failed/Cancelled for Order: {}. ResponseCode: {}", order.getCode(), responseCode);

            // Cập nhật Payment
            payment.setStatus("FAILED");
            payment.setRawPayload(fields.toString());
            paymentRepository.save(payment);

            order.setPaymentStatus("UNPAID"); // Hoặc FAILED
            order.setStatus("WAITING_PAYMENT"); // Cho phép thanh toán lại hoặc Hủy luôn tùy logic
            orderRepository.save(order);

            try {
                productStockService.releaseReservation(order.getCode());
            } catch (Exception e) {
                log.error("Stock Release Failed for Order {}", order.getCode(), e);
            }

            return "FAILED";
        }
    }

    @Override
    @Transactional
    public void createCodPayment(ProductPaymentRequest req) {
        Order order = orderRepository.findByCode(req.getOrderCode())
                .orElseThrow(() -> new RequestException("Order not found: " + req.getOrderCode()));

        Payment payment = Payment.builder()
                .order(order)
                .method("COD")
                .amount(BigDecimal.valueOf(order.getGrandTotal()))
                .status("PENDING") // PENDING nếu muốn quản lý shipper thu tiền
                .build();

        paymentRepository.save(payment);
        log.info("Created COD Payment for Order: {}", order.getCode());
    }

}