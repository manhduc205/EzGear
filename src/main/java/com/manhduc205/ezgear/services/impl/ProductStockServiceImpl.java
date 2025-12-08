package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.ProductStockDTO;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.dtos.responses.StockResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.models.ProductStock;
import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.repositories.OrderRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.repositories.ProductStockRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
import com.manhduc205.ezgear.services.ProductStockService;
import com.manhduc205.ezgear.services.StockTransferService;
import com.manhduc205.ezgear.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

    private final ProductStockRepository productStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductSkuRepository productSkuRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
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
    public List<StockResponse> getAllStock(Long userId) {
        User user = userService.getUserById(userId);

        List<ProductStock> stocks;

        if (userService.isSysAdmin(user)) {
            stocks = productStockRepository.findAll();
        } else {
            Long branchId = user.getBranchId();
            if (branchId == null) {
                return List.of();
            }
            stocks = productStockRepository.findAllByBranchId(branchId);
        }

        return stocks.stream().map(this::mapToStockResponse).collect(Collectors.toList());
    }

    @Override
    public boolean hasReservation(String orderCode) {
        // Simple implementation: if order is in WAITING_PAYMENT and has items, we treat it as reserved.
        return orderRepository.findByCode(orderCode)
                .map(o -> "WAITING_PAYMENT".equalsIgnoreCase(o.getStatus()) && o.getItems() != null && !o.getItems().isEmpty())
                .orElse(false);
    }

    // chốt đơn / trừ kho thật
    // [SỬA LẠI] Logic trừ kho thật khi Giao Hàng (SHIPPING)
    @Override
    @Transactional
    public void commitReservation(String orderCode) {
        var order = orderRepository.findByCode(orderCode)
                .orElseThrow(() -> new RequestException("Order not found"));

        if (order.getItems() == null || order.getItems().isEmpty()) return;

        Long hubWarehouseId = getWarehouseIdByBranch(order.getBranchId());

        for (var it : order.getItems()) {
            int qtyToCommit = it.getQuantity(); // Ví dụ: 10

            // Ktra Hub đang giữ chỗ (Reserved) bao nhiêu?
            ProductStock hubStock = productStockRepository.findByProductSkuIdAndWarehouseId(it.getSkuId(), hubWarehouseId).orElse(null);
            int reservedAtHub = (hubStock != null) ? hubStock.getQtyReserved() : 0;

            // Tính toán phân bổ trừ
            // Ưu tiên trừ vào phần đã giữ chỗ
            int deductFromReserved = Math.min(qtyToCommit, reservedAtHub);
            // Phần còn lại trừ thẳng vào OnHand (Đây là phần hàng từ vệ tinh vừa nhập kho xong)
            int deductDirect = qtyToCommit - deductFromReserved;

            if (deductFromReserved > 0) {
                productStockRepository.commitReserved(it.getSkuId(), hubWarehouseId, deductFromReserved);
            }
            if (deductDirect > 0) {
                int directUpdated = productStockRepository.reduceDirect(it.getSkuId(), hubWarehouseId, deductDirect);
                if (directUpdated == 0) {
                    throw new RequestException("Lỗi kho nghiêm trọng: Hub không đủ hàng thực tế để giao đi (Thiếu OnHand).");
                }
            }
        }
    }

    @Override
    @Transactional
    public void releaseReservation(String orderCode) {
        var order = orderRepository.findByCode(orderCode).orElseThrow(() -> new RequestException("Order not found"));
        if (order.getItems() == null) return;
        Long warehouseId = getWarehouseIdByBranch(order.getBranchId());

        for (var it : order.getItems()) {
            ProductStock hubStock = productStockRepository.findByProductSkuIdAndWarehouseId(it.getSkuId(), warehouseId).orElse(null);
            if (hubStock != null && hubStock.getQtyReserved() > 0) {
                int amountToRelease = Math.min(it.getQuantity(), hubStock.getQtyReserved());
                productStockRepository.releaseReserved(it.getSkuId(), warehouseId, amountToRelease);
            }
        }
    }

    @Override
    @Transactional
    public void reduceStockDirect(Long skuId, Long warehouseId, int qty) {
        //  Nhận trực tiếp WarehouseId
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

    // nhả giữ chỗ thủ công (Dùng cho khi hủy phiếu)
    @Override
    @Transactional
    public void releaseStock(Long skuId, Long warehouseId, int qty) {
        // Chỉ giảm Reserved, KHÔNG giảm OnHand (vì hàng vẫn ở trong kho)
        int updated = productStockRepository.releaseReserved(skuId, warehouseId, qty);

        if (updated == 0) {
            throw new RequestException("Lỗi kho: Không thể nhả giữ chỗ (Số lượng giữ chỗ thực tế ít hơn yêu cầu).");
        }
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

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, Integer>> getStockMatrix(List<Long> warehouseIds, List<Long> skuIds) {
        List<ProductStock> stocks = productStockRepository.findAllBySkuIdInAndWarehouseIdIn(skuIds, warehouseIds);

        // Map<WarehouseID, Map<SkuID, AvailableQty>>
        Map<Long, Map<Long, Integer>> result = new HashMap<>();

        // Init map rỗng cho các kho để tránh NullPointer
        for (Long whId : warehouseIds) {
            result.put(whId, new HashMap<>());
        }

        for (ProductStock ps : stocks) {
            int available = ps.getQtyOnHand() - ps.getQtyReserved() - ps.getSafetyStock();
            if (available < 0) available = 0;

            Long whId = ps.getWarehouse().getId();
            Long sId = ps.getProductSku().getId();

            result.get(whId).put(sId, available);
        }

        return result;
    }

    @Override
    public List<StockResponse> getStockByBranchId(Long branchId) {
        return productStockRepository.findAllByBranchId(branchId).stream()
                .map(this::mapToStockResponse)
                .collect(Collectors.toList());
    }
    // hàm này dùng cho admin tự bấm chuyển hàng
    @Override
    @Transactional
    public void reserveStock(String orderCode, Long skuId, Long warehouseId, int qty) {
        if (warehouseId == null) throw new RequestException("Kho nguồn không hợp lệ");

        // Kiểm tra tồn kho khả dụng tại đúng kho đó
        int available = getAvailable(skuId, warehouseId);

        if (available < qty) {
            ProductSKU sku = productSkuRepository.findById(skuId).orElseThrow();
            Warehouse wh = warehouseRepository.findById(warehouseId).orElseThrow();
            throw new RequestException(
                    String.format("Kho '%s' không đủ hàng. Cần: %d, Có sẵn: %d",
                            wh.getName(), qty, available)
            );
        }

        // giữ chỗ (Chỉ trừ tại kho này)
        productStockRepository.reserveStock(skuId, warehouseId, qty);
    }
    // hàm này dùng cho đặt hàng tự động, ưu tiên kho Hub rồi mới đến kho vệ tinh trong tỉnh
    @Override
    @Transactional
    public Map<Long, Integer> reserveStockDistributed(String orderCode, Long skuId, int requestedQty, Long hubWarehouseId, Integer provinceId) {
        Map<Long, Integer> allocationResult = new HashMap<>();
        int remainingNeeded = requestedQty;

        // 1. Ưu tiên lấy tại Hub (Kho giao hàng) trước
        // Tìm record tồn kho tại Hub
        ProductStock hubStock = productStockRepository.findByProductSkuIdAndWarehouseId(skuId, hubWarehouseId).orElse(null);

        if (hubStock != null) {
            int availableAtHub = hubStock.getQtyOnHand() - hubStock.getQtyReserved() - hubStock.getSafetyStock();
            if (availableAtHub > 0) {
                int takeFromHub = Math.min(availableAtHub, remainingNeeded);

                // Thực hiện giữ chỗ DB
                productStockRepository.reserveStock(skuId, hubWarehouseId, takeFromHub);

                // Ghi vào map kết quả
                allocationResult.put(hubWarehouseId, takeFromHub);
                remainingNeeded -= takeFromHub;
            }
        }

        // Nếu Hub đã đủ -> Return ngay
        if (remainingNeeded == 0) return allocationResult;

        // Nếu Hub thiếu -> Quét các kho vệ tinh trong cùng Tỉnh
        List<ProductStock> provinceStocks = productStockRepository.findAvailableInProvince(skuId, provinceId);

        for (ProductStock pStock : provinceStocks) {
            // Bỏ qua Hub (vì đã lấy ở trên rồi)
            if (pStock.getWarehouse().getId().equals(hubWarehouseId)) continue;

            int available = pStock.getQtyOnHand() - pStock.getQtyReserved() - pStock.getSafetyStock();
            if (available > 0) {
                int take = Math.min(available, remainingNeeded);

                // Giữ chỗ tại kho vệ tinh
                productStockRepository.reserveStock(skuId, pStock.getWarehouse().getId(), take);

                // Ghi vào map
                allocationResult.put(pStock.getWarehouse().getId(), take);
                remainingNeeded -= take;
            }

            if (remainingNeeded == 0) break; // Đã đủ hàng
        }

        // 3. Check cuối cùng
        if (remainingNeeded > 0) {
            // Rollback nếu không đủ hàng
            throw new RequestException("Sản phẩm vừa hết hàng trong quá trình xử lý phân bổ.");
        }

        return allocationResult;
    }
    private StockResponse mapToStockResponse(ProductStock stock) {
        return StockResponse.builder()
                .sku(stock.getProductSku().getSku())
                .skuName(stock.getProductSku().getName())
                .warehouseName(stock.getWarehouse().getName())
                .qtyOnHand(stock.getQtyOnHand())
                .qtyReserved(stock.getQtyReserved())
                .safetyStock(stock.getSafetyStock())
                .available(Math.max(0, stock.getQtyOnHand() - stock.getQtyReserved() - stock.getSafetyStock()))
                .build();
    }
}
