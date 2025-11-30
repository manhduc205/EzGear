package com.manhduc205.ezgear.dtos.responses;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ShipmentHistoryResponse {
    private Long id;
    private String status;
    private String description;
    private LocalDateTime time;
}