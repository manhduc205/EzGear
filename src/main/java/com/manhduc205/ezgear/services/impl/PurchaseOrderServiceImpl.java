package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.PurchaseOrderDTO;
import com.manhduc205.ezgear.dtos.PurchaseOrderItemDTO;
import com.manhduc205.ezgear.dtos.responses.PurchaseOrderItemResponse;
import com.manhduc205.ezgear.dtos.responses.PurchaseOrderResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.mapper.PurchaseOrderMapper;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.security.CustomUserDetails;
import com.manhduc205.ezgear.services.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final ProductStockRepository productStockRepository;
    private final StockTransactionRepository stockTransactionRepository;

    @Override
    public PurchaseOrderDTO createOrder(PurchaseOrderDTO purchaseOrderDTO) {
        Warehouse warehouse = warehouseRepository.findById(purchaseOrderDTO.getWarehouseId())
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.warehouse.not_found")));
        // Sử dụng hàm generatePOCode()
        String code = purchaseOrderDTO.getCode() != null ? purchaseOrderDTO.getCode() : generatePOCode();
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .code(code)
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
                            .orElseThrow(() -> new RequestException(Translator.toLocale("error.sku.not_found")));
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
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.purchase_order.not_found")));

        po.setStatus("CONFIRMED");
        purchaseOrderRepository.save(po);

        return purchaseOrderMapper.toDTO(po);
    }

    @Override
    @Transactional
    public PurchaseOrderDTO receiveOrder(Long id) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();

        Long userId = user.getId();

        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.purchase_order.not_found")));

        if (!"CONFIRMED".equals(po.getStatus())) {
            throw new RequestException(Translator.toLocale("error.purchase_order.must_confirmed_before_receive"));
        }
        po.setStatus("RECEIVED");

        Warehouse warehouse = po.getWarehouse();

        for (PurchaseOrderItem item : po.getItems()) {

            ProductSKU sku = item.getProductSKU();
            int importQty = item.getQuantity();
            // Lấy stock record dựa trên SKU + Warehouse

            ProductStock stock = productStockRepository
                    .findByProductSkuIdAndWarehouseId(sku.getId(), warehouse.getId())
                    .orElseGet(() -> ProductStock.builder()
                            .productSku(sku)
                            .warehouse(warehouse)
                            .qtyOnHand(0)
                            .qtyReserved(0)
                            .safetyStock(0)
                            .build()
                    );

            int before = stock.getQtyOnHand();
            int after  = before + importQty;
            // Update tồn kho

            stock.setQtyOnHand(after);
            productStockRepository.save(stock);
            // Lưu giao dịch kho

            StockTransaction st = StockTransaction.builder()
                    .skuId(sku.getId())
                    .warehouseId(warehouse.getId())
                    .direction(StockTransaction.Direction.IN)
                    .quantity(importQty)
                    .stockBefore(before)
                    .stockAfter(after)
                    .purchasePrice(item.getUnitPrice())
                    .refType("PO")
                    .refId(po.getId())
                    .createdBy(userId)
                    .build();

            stockTransactionRepository.save(st);
        }

        purchaseOrderRepository.save(po);

        return purchaseOrderMapper.toDTO(po);
    }



    @Override
    public PurchaseOrderDTO cancelOrder(Long id) {

        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.purchase_order.not_found")));

        if ("RECEIVED".equals(po.getStatus())) {
            throw new RequestException(Translator.toLocale("error.purchase_order.cannot_cancel_received"));
        }
        po.setStatus("CANCELLED");
        purchaseOrderRepository.save(po);

        return purchaseOrderMapper.toDTO(po);
    }

    @Override
    public List<PurchaseOrderResponse> getAll() {
        return purchaseOrderRepository.findAll().stream()
                .map(po -> {

                    long subtotal = po.getItems()
                            .stream()
                            .mapToLong(item -> item.getUnitPrice() * item.getQuantity())
                            .sum();

                    long total = subtotal;

                    // Map từng item sang response
                    List<PurchaseOrderItemResponse> itemResponses = po.getItems().stream()
                            .map(item -> PurchaseOrderItemResponse.builder()
                                    .sku(item.getProductSKU().getSku())
                                    .skuName(item.getProductSKU().getName())
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .lineTotal(item.getUnitPrice() * item.getQuantity())
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
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.purchase_order.not_found")));
        return purchaseOrderMapper.toDTO(po);
    }

    @Override
    @Transactional
    public PurchaseOrderDTO updateOrder(Long id, PurchaseOrderDTO dto) {

        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.purchase_order.not_found")));

        if (!"DRAFT".equals(po.getStatus())) {
            throw new RequestException(Translator.toLocale("error.purchase_order.only_draft_updatable"));
        }

        // ===== Update basic fields =====
        if (dto.getSupplierName() != null) po.setSupplierName(dto.getSupplierName());
        if (dto.getNote() != null) po.setNote(dto.getNote());

        // Update warehouse
        if (dto.getWarehouseId() != null) {
            Warehouse wh = warehouseRepository.findById(dto.getWarehouseId())
                    .orElseThrow(() -> new RequestException(Translator.toLocale("error.warehouse.not_found")));
            po.setWarehouse(wh);
        }

        Map<Long, PurchaseOrderItem> existing =
                po.getItems().stream().collect(Collectors.toMap(PurchaseOrderItem::getId, i -> i));

        List<PurchaseOrderItem> newList = new ArrayList<>();

        for (PurchaseOrderItemDTO itemDTO : dto.getItems()) {
            // existing item
            if (itemDTO.getId() != null && existing.containsKey(itemDTO.getId())) {
                PurchaseOrderItem item = existing.get(itemDTO.getId());
                item.setQuantity(itemDTO.getQuantity());
                item.setUnitPrice(itemDTO.getUnitPrice());
                newList.add(item);
                existing.remove(itemDTO.getId());
            } else {
                // thêm item mới
                ProductSKU sku = productSkuRepository.findById(itemDTO.getSkuId())
                        .orElseThrow(() -> new RequestException(Translator.toLocale("error.sku.not_found")));

                PurchaseOrderItem newItem = PurchaseOrderItem.builder()
                        .purchaseOrder(po)
                        .productSKU(sku)
                        .quantity(itemDTO.getQuantity())
                        .unitPrice(itemDTO.getUnitPrice())
                        .build();

                newList.add(newItem);
            }
        }

        for (PurchaseOrderItem removed : existing.values()) {
            po.getItems().remove(removed);
        }

        // Gán list mới
        po.getItems().clear();
        po.getItems().addAll(newList);

        // tính lại subtotal và total
        long subtotal = po.getItems().stream()
                .mapToLong(i -> i.getUnitPrice() * i.getQuantity())
                .sum();

        po.setSubtotal(subtotal);
        po.setTotal(subtotal);

        purchaseOrderRepository.save(po);

        return purchaseOrderMapper.toDTO(po);
    }

    private String generatePOCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return "PO" + datePart + sb.toString();
    }
}
