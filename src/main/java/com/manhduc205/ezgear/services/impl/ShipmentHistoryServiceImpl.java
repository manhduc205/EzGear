package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.responses.ShipmentHistoryResponse;
import com.manhduc205.ezgear.exceptions.AccessDeniedException;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.Shipment;
import com.manhduc205.ezgear.models.ShipmentHistory;
import com.manhduc205.ezgear.repositories.ShipmentHistoryRepository;
import com.manhduc205.ezgear.repositories.ShipmentRepository;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.ShipmentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentHistoryServiceImpl implements ShipmentHistoryService {

    private final ShipmentHistoryRepository shipmentHistoryRepository;
    private final ShipmentRepository shipmentRepository;
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
}
