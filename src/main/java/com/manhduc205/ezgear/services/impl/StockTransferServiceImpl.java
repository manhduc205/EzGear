package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.request.StockTransferRequest;
import com.manhduc205.ezgear.dtos.responses.StockTransferResponse;
import com.manhduc205.ezgear.enums.ROLE;
import com.manhduc205.ezgear.enums.TransferStatus;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.services.ProductStockService;
import com.manhduc205.ezgear.services.StockTransferService;
import com.manhduc205.ezgear.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTransferServiceImpl implements StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final WarehouseRepository warehouseRepo;
    private final ProductSkuRepository skuRepo;
    private final UserService userService;
    private final ProductStockService productStockService;

    // ycau chuyển kho thủ công

    @Override
    @Transactional
    public StockTransfer createTransfer(StockTransferRequest req, Long userId) {
        if (req.getFromWarehouseId().equals(req.getToWarehouseId())) {
            throw new RequestException(Translator.toLocale("error.stock_transfer.same_warehouses"));
        }

        Warehouse fromWh = warehouseRepo.findById(req.getFromWarehouseId())
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.warehouse.not_found")));
        Warehouse toWh = warehouseRepo.findById(req.getToWarehouseId())
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.warehouse.not_found")));

        User creator = userService.getUserById(userId);
        // trừ sysadmin thì admin chi nhánh chỉ đc tạo phiếu chuyển từ kho thuộc chi nhánh mình
        if (!userService.isSysAdmin(creator)) {
            Long userBranchId = creator.getBranchId();

            Long sourceBranchId = fromWh.getBranch() != null ? fromWh.getBranch().getId() : null;

            if (userBranchId == null || !userBranchId.equals(sourceBranchId)) {
                throw new RequestException(Translator.toLocale("error.stock_transfer.no_permission_from"));
            }
        }
        StockTransfer transfer = StockTransfer.builder()
                .code("ST-" + System.currentTimeMillis())
                .fromWarehouse(fromWh)
                .toWarehouse(toWh)
                .status(TransferStatus.PENDING)
                .note(req.getNote())
                .referenceCode(null)
                .createdBy(creator)
                .build();

        List<StockTransferItem> items = new ArrayList<>();

        // Duyệt danh sách hàng & Giữ chỗ
        for (var itemReq : req.getItems()) {
            ProductSKU sku = skuRepo.findById(itemReq.getSkuId())
                    .orElseThrow(() -> new RequestException(Translator.toLocale("error.sku.not_found_by_id", itemReq.getSkuId())));

            if (itemReq.getQuantity() <= 0) {
                throw new RequestException(Translator.toLocale("error.stock_transfer.invalid_quantity"));
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
        User systemAdmin = userService.getUserById(1L);
        StockTransfer transfer = StockTransfer.builder()
                .code("AUTO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .fromWarehouse(fromWh)
                .toWarehouse(toWh)
                .status(TransferStatus.PENDING)
                .note("Điều chuyển tự động cho đơn: " + refOrderCode)
                .referenceCode(refOrderCode)
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
    public void shipTransfer(Long transferId, Long userId) {
        User user = userService.getUserById(userId);
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.stock_transfer.not_found")));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new RequestException(Translator.toLocale("error.stock_transfer.only_pending_shippable"));
        }
        if (!userService.isSysAdmin(user)) {
            Long userBranchId = user.getBranchId();
            Long fromBranchId = transfer.getFromWarehouse().getBranch().getId();

            if (userBranchId == null || !userBranchId.equals(fromBranchId)) {
                throw new RequestException(Translator.toLocale("error.stock_transfer.no_permission_ship"));
            }
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
    public void receiveTransfer(Long transferId, Long userId) {
        User user = userService.getUserById(userId);
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.stock_transfer.not_found")));

        if (transfer.getStatus() != TransferStatus.SHIPPING) {
            throw new RequestException(Translator.toLocale("error.stock_transfer.not_shipped_yet"));
        }
        if (!userService.isSysAdmin(user)) {
            Long userBranchId = user.getBranchId();
            Long toBranchId = transfer.getToWarehouse().getBranch().getId();

            if (userBranchId == null || !userBranchId.equals(toBranchId)) {
                throw new RequestException(Translator.toLocale("error.stock_transfer.no_permission_receive"));
            }
        }
        Long toWhId = transfer.getToWarehouse().getId();

        // Cộng tồn kho vào kho đích
        for (StockTransferItem item : transfer.getItems()) {
            Long skuId = item.getProductSku().getId();
            int qty = item.getQuantity();

            // Cộng tồn kho thật -> Hàng đã về kho
            productStockService.addStock(skuId, toWhId, qty);

            // check xem là auto system hay là admin nhập kho
            if (transfer.getReferenceCode() != null) {
                // nếu là auto Hàng về để trả đơn thì Reserve luôn cho đơn
                try {
                    // Mã đơn hàng lấy từ referenceCode
                    productStockService.reserveStock(transfer.getReferenceCode(), skuId, toWhId, qty);
                    log.info("Đã auto-reserve {} item cho đơn {} tại kho {}", qty, transfer.getReferenceCode(), toWhId);
                } catch (Exception e) {
                    // Log warning: Không giữ chỗ được (có thể đơn đã bị hủy trước khi hàng về)
                    // Nhưng vẫn cho phép nhập kho thành công để tránh lệch tồn kho vật lý
                    log.warn("Không thể auto-reserve cho đơn {}: {}", transfer.getReferenceCode(), e.getMessage());
                }
            }
        }

        transfer.setStatus(TransferStatus.COMPLETED);
        stockTransferRepository.save(transfer);
    }
    @Override
    @Transactional
    public void cancelTransfer(Long transferId, Long userId) {
        User user = userService.getUserById(userId);
        StockTransfer transfer = stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new RequestException(Translator.toLocale("error.stock_transfer.not_found")));

        // Check quyền (Giữ nguyên logic check quyền của bạn)
        if (userService.isSysAdmin(user)) {
            Long userBranchId = user.getBranchId();
            Long fromBranchId = transfer.getFromWarehouse().getBranch().getId();
            if (userBranchId == null || !userBranchId.equals(fromBranchId)) {
                throw new RequestException(Translator.toLocale("error.stock_transfer.no_permission_cancel"));
            }
        }

        Long fromWhId = transfer.getFromWarehouse().getId();

        // LOGIC XỬ LÝ DỮ LIỆU
        for (StockTransferItem item : transfer.getItems()) {
            Long skuId = item.getProductSku().getId();
            int qty = item.getQuantity();

            if (transfer.getStatus() == TransferStatus.PENDING) {
                // Chưa xuất -> Hàng vẫn ở kho -> Chỉ cần bỏ giữ chỗ
                productStockService.releaseStock(skuId, fromWhId, qty);

            } else if (transfer.getStatus() == TransferStatus.SHIPPING) {
                // Đã xuất (đã trừ OnHand) -> Phải cộng lại hàng vào kho
                // (Gọi hàm addStock trong ProductStockService)
                productStockService.addStock(skuId, fromWhId, qty);

            } else {
                throw new RequestException(Translator.toLocale("error.stock_transfer.not_cancellable"));
            }
        }

        transfer.setStatus(TransferStatus.CANCELLED);
        stockTransferRepository.save(transfer);
    }
    @Override
    public List<StockTransferResponse> getAll(Long userId) {
        User user = userService.getUserById(userId);

        List<StockTransfer> transfers;

        // Check xem user có phải SYS_ADMIN không
        if (userService.isSysAdmin(user)) {
            transfers = stockTransferRepository.findAll();
        } else {
            Long branchId = user.getBranchId();
            if (branchId == null) return List.of();
            transfers = stockTransferRepository.findAllByBranchId(branchId);
        }

        return transfers.stream()
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