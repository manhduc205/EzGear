package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.PurchaseOrderDTO;
import com.manhduc205.ezgear.mapper.PurchaseOrderMapper;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.repositories.StockTransactionRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
import com.manhduc205.ezgear.repositories.PurchaseOrderRepository;
import com.manhduc205.ezgear.services.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {
    private final WarehouseRepository warehouseRepository;
    private final ProductSkuRepository productSkuRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderMapper  purchaseOrderMapper;
    private final StockTransactionRepository stockTransactionRepository;
    @Override
    public PurchaseOrderDTO createOrder(PurchaseOrderDTO purchaseOrderDTO) {
        Warehouse warehouse = warehouseRepository.findById(purchaseOrderDTO.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .code(purchaseOrderDTO.getCode() != null ? purchaseOrderDTO.getCode() : "PO-" + UUID.randomUUID())
                .supplierName(purchaseOrderDTO.getSupplierName())
                .warehouse(warehouse)
                .status("DRAFT")
                .subtotal(purchaseOrderDTO.getSubtotal())
                .total(purchaseOrderDTO.getTotal())
                .note(purchaseOrderDTO.getNote())
                .createdBy(purchaseOrderDTO.getCreatedBy())
                .build();
        // thêm danh sách po item
        List<PurchaseOrderItem> items = purchaseOrderDTO.getItems()
                .stream().map(i -> {
                    ProductSKU productSKU = productSkuRepository.findById(i.getSkuId())
                            .orElseThrow(() -> new RuntimeException("SKU not found"));
                    return PurchaseOrderItem.builder()
                            .purchaseOrder(purchaseOrder)
                            .productSKU(productSKU)
                            .quantity(i.getQuantity())
                            .unitPrice(i.getUnitPrice())
                            .build();
                }).collect(Collectors.toList());
        purchaseOrder.setItems(items);
        purchaseOrderRepository.save(purchaseOrder);
        return purchaseOrderDTO;
    }

    @Override
    public PurchaseOrderDTO confirmOrder(Long id) {
        PurchaseOrder  po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
        po.setStatus("CONFIRMED");
        return purchaseOrderMapper.toDTO(po);
    }

    @Override
    public PurchaseOrderDTO receiveOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        po.setStatus("RECEIVED");

        // save cho mỗi item
        for(PurchaseOrderItem items : po.getItems()) {
            StockTransaction st = StockTransaction.builder()
                    .skuId(items.getProductSKU().getId())
                    .warehouseId(po.getWarehouse().getId())
                    .direction(StockTransaction.Direction.IN)
                    .quantity(items.getQuantity())
                    .refType("PURCHASE_ORDER")
                    .refId(po.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            stockTransactionRepository.save(st);
        }
        return purchaseOrderMapper.toDTO(po);
    }

    @Override
    public PurchaseOrderDTO cancelOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if ("RECEIVED".equals(po.getStatus())) {
            throw new RuntimeException("Cannot cancel an order that has already been received");
        }

        po.setStatus("CANCELLED");
        purchaseOrderRepository.save(po);

        return purchaseOrderMapper.toDTO(po);
    }
    @Override
    public List<PurchaseOrderDTO> getAll() {
        return purchaseOrderRepository.findAll().stream()
                .map(purchaseOrderMapper::toDTO)
                .toList();
    }

    @Override
    public PurchaseOrderDTO getById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
        return purchaseOrderMapper.toDTO(po);
    }

}
