package com.manhduc205.ezgear.mapper;

import com.manhduc205.ezgear.dtos.PurchaseOrderDTO;
import com.manhduc205.ezgear.dtos.PurchaseOrderItemDTO;
import com.manhduc205.ezgear.models.PurchaseOrder;
import com.manhduc205.ezgear.models.PurchaseOrderItem;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderMapper {


    public PurchaseOrderDTO toDTO(PurchaseOrder po) {
        if (po == null) return null;

        return PurchaseOrderDTO.builder()
                .id(po.getId())
                .code(po.getCode())
                .supplierName(po.getSupplierName())
                .status(po.getStatus())
                .warehouseId(po.getWarehouse() != null ? po.getWarehouse().getId() : null)
                .note(po.getNote())
                .items(po.getItems() != null ? po.getItems().stream().map(i ->
                        PurchaseOrderItemDTO.builder()
                                .id(i.getId())
                                .purchaseOrderId(po.getId())
                                .skuId(i.getProductSKU() != null ? i.getProductSKU().getId() : null)
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .build()
                ).toList() : null)
                .build();
    }
    public PurchaseOrderItemDTO toItemDTO(PurchaseOrderItem item) {
        return PurchaseOrderItemDTO.builder()
                .id(item.getId())
                .purchaseOrderId(item.getPurchaseOrder() != null ? item.getPurchaseOrder().getId() : null)
                .skuId(item.getProductSKU() != null ? item.getProductSKU().getId() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build();
    }
}

