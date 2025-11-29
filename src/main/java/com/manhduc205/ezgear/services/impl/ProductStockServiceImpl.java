package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.ProductStockDTO;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.responses.StockResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.ProductStock;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.repositories.ProductStockRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
import com.manhduc205.ezgear.services.ProductStockService;
import com.manhduc205.ezgear.services.StockTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

    private final ProductStockRepository productStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductSkuRepository productSkuRepository;
    private final OrderRepository orderRepository;

    // vì stockTransferService cần productStockService để trừ/ cộng kho
    // producstStockService cũng cần stockTransferService để tạo phiếu chuyển kho tự động
    // nên ta dùng @Autowired @Lazy để tránh vòng phụ thuộc giữa 2 service @lazy giúp hoãn việc khởi tạo bean đến khi nó thực sự được sử dụng
    @Autowired
    @Lazy
    private  StockTransferService stockTransferService;

    private Long getWarehouseIdByBranch(Long branchId) {
        return warehouseRepository.findFirstByBranchIdAndIsActiveTrue(branchId)
                .map(Warehouse::getId)
                .orElseThrow(() -> new RequestException("Không tìm thấy kho hàng cho chi nhánh " + branchId));
    }
    //ProductStockDTO + delta (số lượng cộng/trừ).
    @Override
    public ProductStockDTO adjustStock(ProductStockDTO productStockDTO, int delta) {
        ProductStock productStock = productStockRepository.findByProductSkuIdAndWarehouseId(productStockDTO.getSkuId(),productStockDTO.getWarehouseId())
                .orElseGet(() ->{
                    ProductSKU sku = productSkuRepository.findById(productStockDTO.getSkuId())
                            .orElseThrow(() -> new RequestException("SKU not found"));
                    Warehouse warehouse = warehouseRepository.findById(productStockDTO.getWarehouseId())
                            .orElseThrow(() -> new RequestException("Warehouse not found"));
                    return ProductStock.builder()
                            .productSku(sku)
                            .warehouse(warehouse)
                            .qtyOnHand(0)
                            .qtyReserved(0)
                            .safetyStock(productStockDTO.getSafetyStock() != null ? productStockDTO.getSafetyStock() : 0)
                            .build();
                });
        productStock.setQtyOnHand(productStock.getQtyOnHand() + delta);
        ProductStock saved = productStockRepository.save(productStock);
        return ProductStockDTO.builder()
                .id(saved.getId())
                .skuId(saved.getProductSku().getId())
                .warehouseId(saved.getWarehouse().getId())
                .qtyOnHand(saved.getQtyOnHand())
                .qtyReserved(saved.getQtyReserved())
                .safetyStock(saved.getSafetyStock())
                .build();
    }

    @Override
    public int getAvailable(Long skuId, Long warehouseId) {
        return productStockRepository.findByProductSkuIdAndWarehouseId(skuId, warehouseId)
                .map(stock -> {
                    int available = stock.getQtyOnHand() - stock.getQtyReserved() - stock.getSafetyStock();
                    return Math.max(0, available); // Không bao giờ trả về số âm
                })
                .orElse(0);
    }


    @Override
    public List<StockResponse> getAllStock() {
        return productStockRepository.findAll()
                .stream()
                .map(stock -> StockResponse.builder()
                        .sku(stock.getProductSku().getSku())
                        .skuName(stock.getProductSku().getName())
                        .warehouseName(stock.getWarehouse().getName())
                        .qtyOnHand(stock.getQtyOnHand())
                        .qtyReserved(stock.getQtyReserved())
                        .safetyStock(stock.getSafetyStock())
                        .available(stock.getQtyOnHand() - stock.getQtyReserved() - stock.getSafetyStock())
                        .build()
                )
                .toList();
    }

    @Override
    @Transactional
    // giữ chỗ, tìm nguồn hàng từ các kho
    public void reserveStock(String orderCode, Long skuId, Long warehouseId, int qty) {
        if (warehouseId == null) throw new RequestException("Kho không hợp lệ");

        Long mainWarehouseId = warehouseId; // Đây là Kho Hub (Kho Đích)

        // Thử giữ chỗ tại Kho Hub (Kho Chính)
        int currentAvailable = getAvailable(skuId, mainWarehouseId);
        int quantityToReserveAtMain = Math.min(currentAvailable, qty);
        int quantityNeededFromOthers = qty - quantityToReserveAtMain;

        if (quantityToReserveAtMain > 0) {
            // giữ chỗ tại kho hub
            productStockRepository.reserveStock(skuId, mainWarehouseId, quantityToReserveAtMain);
        }

        // Nếu thiếu -> Vét hàng ở các kho khác CÙNG TỈNH (Kho Phụ)
        if (quantityNeededFromOthers > 0) {
            Integer provinceId = warehouseRepository.findById(mainWarehouseId).get().getBranch().getProvinceId();
            List<Warehouse> otherWarehouses = warehouseRepository.findActiveWarehousesByProvince(provinceId);

            for (Warehouse warehouse : otherWarehouses) {
                if (warehouse.getId().equals(mainWarehouseId)) continue; // Bỏ qua kho chính
                if (quantityNeededFromOthers == 0) break;

                int avail = getAvailable(skuId, warehouse.getId()); // Tồn kho khả dụng tại kho phụ
                if (avail > 0) {
                    int take = Math.min(avail, quantityNeededFromOthers);

                    // Giữ chỗ tại Kho Phụ
                    int updated = productStockRepository.reserveStock(skuId, warehouse.getId(), take);

                    if (updated > 0) {
                        // Tạo phiếu chuyển kho tự động
                        // Từ Kho Phụ  -> Về Kho Hub
                        stockTransferService.createAutoTransfer(
                                warehouse.getId(),       // From
                                mainWarehouseId,  // To
                                skuId,
                                take,
                                orderCode
                        );

                        quantityNeededFromOthers -= take; // Cập nhật số lượng còn thiếu
                    }
                }
            }
        }

        if (quantityNeededFromOthers > 0) {
            throw new RequestException("Lỗi hệ thống: Tồn kho không đồng bộ (Không đủ hàng để điều chuyển).");
        }
    }

    @Override
    public boolean hasReservation(String orderCode) {
        // Simple implementation: if order is in WAITING_PAYMENT and has items, we treat it as reserved.
        return orderRepository.findByCode(orderCode)
                .map(o -> "WAITING_PAYMENT".equalsIgnoreCase(o.getStatus()) && o.getItems() != null && !o.getItems().isEmpty())
                .orElse(false);
    }

    @Override
    @Transactional
    //Chốt đơn / Xuất kho thật
    public void commitReservation(String orderCode) {
        var order = orderRepository.findByCode(orderCode)
                .orElseThrow(() -> new RequestException("Order not found when committing reservation"));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        Long warehouseId = getWarehouseIdByBranch(order.getBranchId());
        for (var it : order.getItems()) {
            int updated = productStockRepository.commitReserved(it.getSkuId(), warehouseId, it.getQuantity());
            if (updated == 0) {
                throw new RequestException("Không thể commit giữ chỗ cho SKU " + it.getSkuId());
            }
        }
    }

    @Override
    @Transactional
    //Nhả hàng / Hủy giữ chỗ
    public void releaseReservation(String orderCode) {
        var order = orderRepository.findByCode(orderCode)
                .orElseThrow(() -> new RequestException("Order not found when releasing reservation"));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        Long warehouseId = getWarehouseIdByBranch(order.getBranchId());
        for (var it : order.getItems()) {
            productStockRepository.releaseReserved(it.getSkuId(), warehouseId, it.getQuantity());
        }
    }

    @Override
    @Transactional
    public void reduceStockDirect(Long skuId, Long warehouseId, int qty) {
        // SỬA: Nhận trực tiếp WarehouseId
        if (warehouseId == null) throw new RequestException("Kho không hợp lệ");

        int updated = productStockRepository.reduceDirect(skuId, warehouseId, qty);
        if (updated == 0) {
            throw new RequestException("Sản phẩm không đủ hàng để xuất kho.");
        }
    }

    @Override
    public int getAvailableInProvince(Long skuId, Integer provinceId) {
        // Nếu không có ProvinceId (Lỗi Frontend hoặc chưa chọn), coi như không có hàng tại đó
        if (provinceId == null) {
            return 0;
        }
        Integer totalAvailable = productStockRepository.sumStockByProvince(skuId, provinceId);
        return totalAvailable == null ? 0 : Math.max(0, totalAvailable);
    }

    @Override
    public int getTotalSystemStock(Long skuId) {
        Integer total = productStockRepository.sumTotalAvailable(skuId);
        return total == null ? 0 : Math.max(0, total);
    }

    @Override
    @Transactional
    public void commitTransferStock(Long skuId, Long warehouseId, int qty) {
        // Giảm QtyOnHand (xuất thật) VÀ Giảm QtyReserved (xả giữ chỗ)
        int updated = productStockRepository.commitReserved(skuId, warehouseId, qty);

        if (updated == 0) {
            // ta thử trừ trực tiếp vào QtyOnHand để đảm bảo hàng xuất đi được.
            // Điều này quan trọng để không chặn quy trình vận hành.
            int direct = productStockRepository.reduceDirect(skuId, warehouseId, qty);

            if (direct == 0) {
                throw new RequestException("Lỗi kho: Không đủ hàng để xuất chuyển kho (SKU: " + skuId + ")");
            }
        }
    }

    @Override
    @Transactional
    public void addStock(Long skuId, Long warehouseId, int qty) {
        if (qty <= 0) throw new RequestException("Số lượng nhập phải lớn hơn 0");

        // Thử cộng dồn vào record có sẵn
        int updated = productStockRepository.increaseStock(skuId, warehouseId, qty);

        // Nếu chưa có record nào -> tạo mới
        if (updated == 0) {
            ProductSKU sku = productSkuRepository.findById(skuId)
                    .orElseThrow(() -> new RequestException("SKU không tồn tại: " + skuId));

            Warehouse wh = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new RequestException("Kho không tồn tại: " + warehouseId));

            ProductStock newStock = ProductStock.builder()
                    .productSku(sku)
                    .warehouse(wh)
                    .qtyOnHand(qty) // Set số lượng ban đầu
                    .qtyReserved(0)
                    .safetyStock(0)
                    .build();

            productStockRepository.save(newStock);
        }
    }
}
