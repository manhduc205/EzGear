package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.responses.ShipmentHistoryResponse;
import com.manhduc205.ezgear.dtos.responses.TrackingResponse;
import com.manhduc205.ezgear.models.Shipment;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.shipping.dto.request.GhnWebhookRequest;


import java.time.LocalDateTime;
import java.util.List;

public interface ShipmentHistoryService {
    void addHistory(Shipment shipment, String status, String note, LocalDateTime eventTime);
    void processWebhook(GhnWebhookRequest req);
    List<ShipmentHistoryResponse> getHistoryByShipmentId(Long shipmentId, CustomUserDetails user) ;
    TrackingResponse getTrackingDetails(Long orderId, Long userId);
}