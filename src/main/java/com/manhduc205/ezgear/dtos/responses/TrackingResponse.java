package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TrackingResponse {
    private String orderCode;
    private String trackingCode;
    private String currentStatus;
    private String receiverAddress;
    private String expectedDeliveryTime;

    private List<TrackingStep> timeline;

    @Data
    @Builder
    public static class TrackingStep {
        private String title;
        private String description;
        private LocalDateTime time;
        private boolean isCompleted;
    }
}