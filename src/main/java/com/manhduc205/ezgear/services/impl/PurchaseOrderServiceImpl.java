package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.PurchaseOrderDTO;
import com.manhduc205.ezgear.dtos.PurchaseOrderItemDTO;
import com.manhduc205.ezgear.dtos.responses.PurchaseOrderItemResponse;
import com.manhduc205.ezgear.dtos.responses.PurchaseOrderResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
    public List<PurchaseOrderResponse> getAll() {
        return purchaseOrderRepository.findAll().stream()
                .map(po -> {

                    // Tính lại subtotal
                    BigDecimal subtotal = po.getItems().stream()
                            .map(item -> item.getUnitPrice()
                                    .multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal total = subtotal;

                    // Map từng item sang response
                    List<PurchaseOrderItemResponse> itemResponses = po.getItems().stream()
                            .map(item -> PurchaseOrderItemResponse.builder()
                                    .sku(item.getProductSKU().getSku())
                                    .skuName(item.getProductSKU().getName())
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .lineTotal(item.getUnitPrice()
                                            .multiply(BigDecimal.valueOf(item.getQuantity())))
                                    .build()
                            ).toList();

                    // Trả về response đầy đủ
                    return PurchaseOrderResponse.builder()
                            .id(po.getId())
                            .code(po.getCode())
                            .supplierName(po.getSupplierName())
                            .warehouseName(po.getWarehouse().getName())
                            .status(po.getStatus())
                            .subtotal(subtotal)
                            .total(total)
                            .note(po.getNote())
                            .createdBy(po.getCreatedBy())
                            .items(itemResponses)
                            .build();

                }).toList();
    }


    @Override
    public PurchaseOrderDTO getById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
        return purchaseOrderMapper.toDTO(po);
    }

    @Override
    @Transactional
    public PurchaseOrderDTO updateOrder(Long id, PurchaseOrderDTO dto) {

        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if (!"DRAFT".equals(po.getStatus())) {
            throw new RuntimeException("Only DRAFT purchase orders can be updated");
        }

        // ===== Update basic fields =====
        if (dto.getSupplierName() != null) po.setSupplierName(dto.getSupplierName());
        if (dto.getNote() != null) po.setNote(dto.getNote());

        // Update warehouse
        if (dto.getWarehouseId() != null) {
            Warehouse wh = warehouseRepository.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new RuntimeException("Warehouse not found"));
            po.setWarehouse(wh);
        }

        // ===== Convert existing items to Map for fast lookup =====
        Map<Long, PurchaseOrderItem> existing =
                po.getItems().stream().collect(Collectors.toMap(PurchaseOrderItem::getId, i -> i));

        List<PurchaseOrderItem> newList = new ArrayList<>();

        for (PurchaseOrderItemDTO itemDTO : dto.getItems()) {

            // ========== CASE B: UPDATE existing item ==========
            if (itemDTO.getId() != null && existing.containsKey(itemDTO.getId())) {

                PurchaseOrderItem item = existing.get(itemDTO.getId());

                item.setQuantity(itemDTO.getQuantity());
                item.setUnitPrice(itemDTO.getUnitPrice());

                newList.add(item);

                existing.remove(itemDTO.getId()); // Mark as handled
            }

            // ========== CASE A: ADD new item ==========
            else {
                ProductSKU sku = productSkuRepository.findById(itemDTO.getSkuId())
                        .orElseThrow(() -> new RuntimeException("SKU not found"));

                PurchaseOrderItem newItem = PurchaseOrderItem.builder()
                        .purchaseOrder(po)
                        .productSKU(sku)
                        .quantity(itemDTO.getQuantity())
                        .unitPrice(itemDTO.getUnitPrice())
                        .build();

                newList.add(newItem);
            }
        }

        // ========== CASE C: DELETE items removed by user ==========
        for (PurchaseOrderItem removed : existing.values()) {
            po.getItems().remove(removed);
        }

        // Gán list mới
        po.getItems().clear();
        po.getItems().addAll(newList);

        // ===== Recalculate subtotal / total =====
        BigDecimal subtotal = po.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        po.setSubtotal(subtotal);
        po.setTotal(subtotal);

        purchaseOrderRepository.save(po);

        return purchaseOrderMapper.toDTO(po);
    }


}
