package com.manhduc205.ezgear.dtos.responses;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BranchStockResponse {
    private Long warehouseId;
    private String branchName;
    private String fullAddress;
    private String phone;
    private String mapUrl;
    private Integer quantity;
}