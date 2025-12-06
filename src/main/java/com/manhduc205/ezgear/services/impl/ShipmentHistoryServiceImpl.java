package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.responses.ShipmentHistoryResponse;
import com.manhduc205.ezgear.dtos.responses.TrackingResponse;
import com.manhduc205.ezgear.enums.GhnOrderStatus;
import com.manhduc205.ezgear.enums.OrderStatus;
import com.manhduc205.ezgear.enums.PaymentMethod;
import com.manhduc205.ezgear.enums.ShipmentStatus;
import com.manhduc205.ezgear.exceptions.AccessDeniedException;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.Shipment;
import com.manhduc205.ezgear.models.ShipmentHistory;
import com.manhduc205.ezgear.models.order.Order;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.PaymentRepository;
import com.manhduc205.ezgear.repositories.ShipmentHistoryRepository;
import com.manhduc205.ezgear.repositories.ShipmentRepository;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.ShipmentHistoryService;
import com.manhduc205.ezgear.shipping.dto.request.GhnWebhookRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentHistoryServiceImpl implements ShipmentHistoryService {

    private final ShipmentHistoryRepository shipmentHistoryRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    @Override
    public void addHistory(Shipment shipment, String status, String note, LocalDateTime eventTime) {
        ShipmentHistory shipmentHistory = ShipmentHistory.builder()
                .shipment(shipment)
                .status(status)
                .note(note)
                .eventTime(eventTime != null ? eventTime : LocalDateTime.now())
                .build();
        shipmentHistoryRepository.save(shipmentHistory);
    }

    @Override
    public List<ShipmentHistoryResponse> getHistoryByShipmentId(Long shipmentId, CustomUserDetails user)  {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RequestException("Vận đơn không tồn tại"));
        boolean isAdminOrStaff = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_SYS_ADMIN"));

        // Nếu là khách hàng thường, chỉ được xem đơn của chính mình
        if (!isAdminOrStaff && !shipment.getOrder().getUserId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem lịch sử hành trình của đơn hàng này.");
        }
        return shipmentHistoryRepository.findByShipmentIdOrderByEventTimeDesc(shipmentId)
                .stream()
                .map(h -> ShipmentHistoryResponse.builder()
                        .id(h.getId())
                        .status(h.getStatus())
                        .description(h.getNote())
                        .time(h.getEventTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingResponse getTrackingDetails(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RequestException("Đơn hàng không tồn tại"));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xem hành trình đơn hàng này.");
        }

        Shipment shipment = shipmentRepository.findByOrderId(orderId).orElse(null);

        List<TrackingResponse.TrackingStep> timeline = new ArrayList<>();

        // Đặt hàng thành công
        timeline.add(TrackingResponse.TrackingStep.builder()
                .title("Đơn hàng đã đặt")
                .description("EzGear đang chuẩn bị hàng")
                .time(order.getCreatedAt())
                .isCompleted(true)
                .build());

        // Lịch sử vận chuyển
        if (shipment != null) {
            List<ShipmentHistory> histories = shipmentHistoryRepository.findByShipmentIdOrderByEventTimeDesc(shipment.getId());

            for (ShipmentHistory h : histories) {
                String displayTitle = h.getStatus();
                // Map Enum sang tiếng Việt
                GhnOrderStatus ghnStatus = GhnOrderStatus.fromCode(h.getStatus());
                if (ghnStatus != null) {
                    displayTitle = ghnStatus.getDescription();
                } else if (ShipmentStatus.READY_TO_PICK.name().equals(h.getStatus())) {
                    displayTitle = "Đang chờ lấy hàng";
                }

                timeline.add(TrackingResponse.TrackingStep.builder()
                        .title(displayTitle)
                        .description(h.getNote())
                        .time(h.getEventTime())
                        .isCompleted(true)
                        .build());
            }
        }

        // Sắp xếp lại Timeline (Mới nhất lên đầu)
        timeline.sort((s1, s2) -> s2.getTime().compareTo(s1.getTime()));

        String currentStatusStr = timeline.isEmpty() ? "Đang xử lý" : timeline.get(0).getTitle();
        String trackingCodeStr = shipment != null ? shipment.getTrackingCode() : null;
        String receiverAddr = order.getShippingAddress() != null ? order.getShippingAddress().getAddressLine() : "";

        // Lấy thời gian giao dự kiến (nếu cần có thể lưu vào Shipment khi tạo đơn)
        String expectedTime = "Đang cập nhật";

        return TrackingResponse.builder()
                .orderCode(order.getCode())
                .trackingCode(trackingCodeStr)
                .currentStatus(currentStatusStr)
                .receiverAddress(receiverAddr)
                .expectedDeliveryTime(expectedTime)
                .timeline(timeline)
                .build();
    }
    @Override
    @Transactional
    public void processWebhook(GhnWebhookRequest req) {
        if (!"switch_status".equalsIgnoreCase(req.getType())) return;

        Shipment shipment = shipmentRepository.findByTrackingCode(req.getOrderCode())
                .orElseThrow(() -> new RequestException("Vận đơn không tồn tại: " + req.getOrderCode()));

        // Map trạng thái GHN sang Enum của mình
        GhnOrderStatus ghnStatus = GhnOrderStatus.fromCode(req.getStatus());

        String statusString = (ghnStatus != null) ? ghnStatus.name() : req.getStatus().toUpperCase();
        String noteString = (ghnStatus != null) ? ghnStatus.getDescription() : req.getDescription();

        // Bổ sung thông tin kho nếu có
        if (req.getWarehouse() != null) {
            noteString += " tại " + req.getWarehouse();
        }

        // Lưu Lịch sử
        ShipmentHistory history = ShipmentHistory.builder()
                .shipment(shipment)
                .status(statusString)
                .note(noteString)
                .eventTime(parseGhnTime(req.getTime()))
                .build();
        shipmentHistoryRepository.save(history);

        //Cập nhật trạng thái Shipment
        shipment.setStatus(statusString);
        shipmentRepository.save(shipment);

        // cập nhật trạng thái đơn hàng
        updateOrderStatusFromGhn(shipment.getOrder(), ghnStatus);
    }


    // Logic mapping quan trọng nhất
    private void updateOrderStatusFromGhn(Order order, GhnOrderStatus ghnStatus) {
        if (ghnStatus == null) return;

        switch (ghnStatus) {
            case DELIVERED:
                order.setStatus(OrderStatus.COMPLETED.name());
                // Nếu là COD -> Đánh dấu đã thanh toán
                if (PaymentMethod.COD == order.getPaymentMethod()) {
                    order.setPaymentStatus("PAID");
                }
                break;

            case PICKED:
            case STORING:
            case TRANSPORTING:
            case SORTING:
            case DELIVERING:
            case MONEY_COLLECT_DELIVERING:
                // Tất cả các trạng thái này đều quy về SHIPPING (Đang giao hàng)
                if (!OrderStatus.SHIPPING.name().equals(order.getStatus())) {
                    order.setStatus(OrderStatus.SHIPPING.name());
                }
                break;

            case CANCEL:
                order.setStatus(OrderStatus.CANCELLED.name());
                break;

            case RETURN:
            case RETURNED:
            case WAITING_TO_RETURN:
                order.setStatus(OrderStatus.CANCELLED.name());
                break;

            default:
                break;
        }
        orderRepository.save(order);
    }

    // Helper: Parse thời gian từ chuỗi ISO của GHN (2021-11-11T03:52:50.158Z)
    private LocalDateTime parseGhnTime(String timeStr) {
        try {
            // Cắt bỏ phần mili giây nếu cần hoặc dùng Formatter chuẩn ISO
            return LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
