package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.PaymentResultRequest;
import com.manhduc205.ezgear.dtos.request.ProductPaymentRequest;
import com.manhduc205.ezgear.dtos.responses.VNPayResponse;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.models.order.Payment;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.PaymentRepository;
import com.manhduc205.ezgear.services.PaymentService;
import com.manhduc205.ezgear.utils.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

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
    public VNPayResponse createPayment(ProductPaymentRequest req) {
        // 1. Kiểm tra đơn hàng tồn tại
        Order order = orderRepository.findByCode(req.getOrderCode())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getPaymentStatus() != null && order.getPaymentStatus().equals("PAID")) {
            throw new RuntimeException("Order already paid");
        }
        // Sinh mã giao dịch riêng cho VNPay
        String vnpTxnRef = "VNP" + System.currentTimeMillis();

        Payment payment = Payment.builder()
                .order(order)
                .method("VNPAY")
                .amount(BigDecimal.valueOf(req.getAmount()))
                .status("PENDING")
                .vnpTxnRef(vnpTxnRef)
                .build();
        paymentRepository.save(payment);

        // param gửi VNPay
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(req.getAmount() * 100)); // nhân 100
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", vnpTxnRef);
        params.put("vnp_OrderInfo", order.getCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", "127.0.0.1");
        String createDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        params.put("vnp_CreateDate", createDate);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        cal.add(Calendar.MINUTE, 15); // hết hạn sau 15 phút
        String expireDate = new SimpleDateFormat("yyyyMMddHHmmss").format(cal.getTime());
        params.put("vnp_ExpireDate", expireDate);


        //Tạo chữ ký
        String query = VnPayUtil.buildQuery(params);
        String secureHash = VnPayUtil.hmacSHA512(hashSecret, query);
        String fullUrl = payUrl + "?" + query + "&vnp_SecureHash=" + secureHash;

        //Cập nhật trạng thái đơn hàng sang “PENDING_PAYMENT”
        order.setPaymentStatus("PENDING");
        orderRepository.save(order);

        return new VNPayResponse(fullUrl);
    }

    @Override
    @Transactional
    public String handleVnPayCallback(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();

        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                fields.put(fieldName, fieldValue);
            }
        }

        // Loại bỏ 2 trường không được hash
        fields.remove("vnp_SecureHashType");
        String vnp_SecureHash = fields.remove("vnp_SecureHash");

        // Build chuỗi và tính chữ ký
        String signData = VnPayUtil.buildQuery(fields);
        String expectedHash = VnPayUtil.hmacSHA512(hashSecret, signData);

        System.out.println("=== VNPay Signature Debug ===");
        System.out.println("Raw query : " + signData);
        System.out.println("Expected  : " + expectedHash);
        System.out.println("Received  : " + vnp_SecureHash);

        if (!expectedHash.equalsIgnoreCase(vnp_SecureHash)) {
            return "INVALID_SIGNATURE";
        }

        // Nếu ok, xử lý order
        String txnRef = fields.get("vnp_TxnRef");
        String responseCode = fields.get("vnp_ResponseCode");

        Payment payment = paymentRepository.findByVnpTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        Order order = payment.getOrder();

        if ("00".equals(responseCode)) {
            order.setPaymentStatus("PAID");
            payment.setStatus("PAID");
        } else {
            order.setPaymentStatus("FAILED");
            payment.setStatus("FAILED");
        }

        payment.setProviderTxnId(fields.get("vnp_TransactionNo"));
        payment.setRawPayload(signData);
        paymentRepository.save(payment);
        orderRepository.save(order);

        return "OK";
    }

}
