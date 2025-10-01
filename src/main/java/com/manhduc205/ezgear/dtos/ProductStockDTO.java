package com.manhduc205.ezgear.dtos;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ProductStockDTO {
    private Long id;
    private Long skuId;
    private Long warehouseId;
    private Integer qtyOnHand;
    private Integer qtyReserved;
    private Integer safetyStock;

}
