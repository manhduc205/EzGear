package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.request.StockTransferRequest;
import com.manhduc205.ezgear.dtos.responses.StockTransferResponse;
import com.manhduc205.ezgear.enums.TransferStatus;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.services.ProductStockService;
import com.manhduc205.ezgear.services.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockTransferServiceImpl implements StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final WarehouseRepository warehouseRepo;
    private final ProductSkuRepository skuRepo;
    private final UserRepository userRepo;
    private final ProductStockService productStockService;

    // ycau chuyển kho thủ công

    @Override
    @Transactional
    public StockTransfer createTransfer(StockTransferRequest req, Long userId) {
        if (req.getFromWarehouseId().equals(req.getToWarehouseId())) {
            throw new RequestException("Kho nguồn và kho đích không được trùng nhau.");
        }

        Warehouse fromWh = warehouseRepo.findById(req.getFromWarehouseId())
                .orElseThrow(() -> new RequestException("Kho nguồn không tồn tại"));
        Warehouse toWh = warehouseRepo.findById(req.getToWarehouseId())
                .orElseThrow(() -> new RequestException("Kho đích không tồn tại"));

        User creator = userRepo.findById(userId)
                .orElseThrow(() -> new RequestException("Người dùng không tồn tại"));

        StockTransfer transfer = StockTransfer.builder()
                .code("ST-" + System.currentTimeMillis())
                .fromWarehouse(fromWh)
                .toWarehouse(toWh)
                .status(TransferStatus.PENDING)
                .note(req.getNote())
                .createdBy(creator)
                .build();

        List<StockTransferItem> items = new ArrayList<>();

        // Duyệt danh sách hàng & Giữ chỗ
        for (var itemReq : req.getItems()) {
            ProductSKU sku = skuRepo.findById(itemReq.getSkuId())
                    .orElseThrow(() -> new RequestException("SKU " + itemReq.getSkuId() + " không tồn tại"));

            if (itemReq.getQuantity() <= 0) {
                throw new RequestException("Số lượng chuyển phải lớn hơn 0");
            }

            // Gọi StockService để giữ chỗ tại kho nguồn
            // Dùng mã phiếu chuyển (code) làm key để giữ chỗ
            productStockService.reserveStock(transfer.getCode(), sku.getId(), fromWh.getId(), itemReq.getQuantity());

            items.add(StockTransferItem.builder()
                    .stockTransfer(transfer)
                    .productSku(sku)
                    .quantity(itemReq.getQuantity())
                    .build());
        }

        transfer.setItems(items);
        return stockTransferRepository.save(transfer);
    }

    // TỰ ĐỘNG TẠO PHIẾU ( OrderService)

    @Override
    @Transactional
    public void createAutoTransfer(Long fromWhId, Long toWhId, Long skuId, int qty, String refOrderCode) {
        Warehouse fromWh = warehouseRepo.findById(fromWhId).orElseThrow();
        Warehouse toWh = warehouseRepo.findById(toWhId).orElseThrow();
        ProductSKU sku = skuRepo.findById(skuId).orElseThrow();
        // Khi OrderService gọi hàm này, nó ĐÃ thực hiện reserveStock rồi
        // Nên ở đây ta KHÔNG gọi reserveStock nữa để tránh giữ chỗ 2 lần
        User systemAdmin = userRepo.findById(1L).orElse(null);
        StockTransfer transfer = StockTransfer.builder()
                .code("AUTO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .fromWarehouse(fromWh)
                .toWarehouse(toWh)
                .status(TransferStatus.PENDING)
                .note("Điều chuyển tự động cho đơn: " + refOrderCode)
                .createdBy(systemAdmin)
                .build();

        StockTransferItem item = StockTransferItem.builder()
                .stockTransfer(transfer)
                .productSku(sku)
                .quantity(qty)
                .build();

        transfer.setItems(List.of(item));
        stockTransferRepository.save(transfer);
    }

    // XUẤT KHO (SHIP) - Trừ kho thật tại nguồn
    @Override
    @Transactional
    public void shipTransfer(Long transferId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new RequestException("Phiếu chuyển không tồn tại"));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RequestException("Chỉ phiếu ở trạng thái PENDING mới được xuất kho.");
        }

        Long fromWhId = transfer.getFromWarehouse().getId();

        // Commit Reservation: Chuyển từ Reserved -> Trừ hẳn QtyOnHand
        for (StockTransferItem item : transfer.getItems()) {
            // Sử dụng hàm commitTransferStock (đã thêm vào ProductStockService)
            // Hàm này sẽ: Giảm OnHand và Giảm Reserved
            productStockService.commitTransferStock(
                    item.getProductSku().getId(),
                    fromWhId,
                    item.getQuantity()
            );
        }

        transfer.setStatus(TransferStatus.SHIPPING);
        stockTransferRepository.save(transfer);
    }

    //  NHẬP KHO Cộng kho tại đích
    @Override
    @Transactional
    public void receiveTransfer(Long transferId) {
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new RequestException("Phiếu chuyển không tồn tại"));

        if (transfer.getStatus() != TransferStatus.SHIPPING) {
            throw new RequestException("Phiếu chưa được xuất kho, không thể nhập.");
        }

        Long toWhId = transfer.getToWarehouse().getId();

        // Cộng tồn kho vào kho đích
        for (StockTransferItem item : transfer.getItems()) {
            // tăng tồn kho thực tế
            productStockService.addStock(
                    item.getProductSku().getId(),
                    toWhId,
                    item.getQuantity()
            );
        }

        transfer.setStatus(TransferStatus.COMPLETED);
        stockTransferRepository.save(transfer);
    }

    @Override
    public List<StockTransferResponse> getAll() {
        return stockTransferRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    private StockTransferResponse mapToResponse(StockTransfer t) {
        // Map chi tiết items
        List<StockTransferResponse.TransferItemResponse> itemResponses = t.getItems().stream()
                .map(item -> StockTransferResponse.TransferItemResponse.builder()
                        .skuId(item.getProductSku().getId())
                        .skuCode(item.getProductSku().getSku())
                        .productName(item.getProductSku().getName())
                        .imageUrl(item.getProductSku().getProduct().getImageUrl())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return StockTransferResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .fromWarehouseName(t.getFromWarehouse().getName())
                .toWarehouseName(t.getToWarehouse().getName())
                .createdByName(t.getCreatedBy() != null ? t.getCreatedBy().getFullName() : "Hệ thống")
                .status(t.getStatus())
                .note(t.getNote())
                .createdAt(t.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}