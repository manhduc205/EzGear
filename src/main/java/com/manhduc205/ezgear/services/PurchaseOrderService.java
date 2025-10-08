package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.PurchaseOrderDTO;

import java.util.List;

public interface PurchaseOrderService {
    PurchaseOrderDTO createOrder(PurchaseOrderDTO purchaseOrderDTO);
    PurchaseOrderDTO confirmOrder(Long id);
    PurchaseOrderDTO receiveOrder(Long id);
    PurchaseOrderDTO cancelOrder(Long id);
    List<PurchaseOrderDTO> getAll();
    PurchaseOrderDTO getById(Long id);
}
