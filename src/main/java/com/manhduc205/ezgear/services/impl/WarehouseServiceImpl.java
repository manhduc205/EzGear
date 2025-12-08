package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.WarehouseDTO;
import com.manhduc205.ezgear.dtos.request.CartItemRequest;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.Branch;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.repositories.BranchRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
import com.manhduc205.ezgear.services.ProductStockService;
import com.manhduc205.ezgear.services.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final BranchRepository branchRepository;
    private final ProductStockService productStockService;
    @Override
    public Warehouse createWarehouse(WarehouseDTO dto) {
        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        Warehouse warehouse = Warehouse.builder()
                .branch(branch)
                .code(dto.getCode())
                .name(dto.getName())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        return warehouseRepository.save(warehouse);
    }

    @Override
    public List<WarehouseDTO> getAll() {
        return warehouseRepository.findAll().stream()
                .map(w -> WarehouseDTO.builder()
                        .id(w.getId())
                        .branchId(w.getBranch() != null ? w.getBranch().getId() : null)
                        .code(w.getCode())
                        .name(w.getName())
                        .isActive(w.getIsActive())
                        .build())
                .toList();
    }

    @Override
    public Optional<Warehouse> getById(Long id) {
        return warehouseRepository.findById(id);
    }

    @Override
    public Warehouse updateWarehouse(Long id, WarehouseDTO dto) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        if (dto.getBranchId() != null) {
            Branch branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            warehouse.setBranch(branch);
        }

        if (dto.getName() != null) {
            warehouse.setName(dto.getName());
        }
        if (dto.getCode() != null) {
            warehouse.setCode(dto.getCode());
        }
        if (dto.getIsActive() != null) {
            warehouse.setIsActive(dto.getIsActive());
        }

        return warehouseRepository.save(warehouse);
    }

    @Override
    public void delete(Long id) {
        warehouseRepository.deleteById(id);
    }

    /**
     * Chọn kho giao hàng dựa trên địa chỉ khách:
     *  - Ưu tiên chi nhánh cùng quận (district)
     *  - Nếu không có, fallback chi nhánh cùng tỉnh (province)
     *  - Sau đó lấy kho đang hoạt động đầu tiên của chi nhánh đó
     */
    @Override
    public Warehouse resolveWarehouseForAddress(CustomerAddress address) {
        if (address == null) {
            throw new RequestException("Địa chỉ giao hàng không hợp lệ (null).");
        }

        Integer provinceId = address.getProvinceId();
        Integer districtId = address.getDistrictId();

        if (provinceId == null || districtId == null) {
            throw new RequestException("Địa chỉ giao hàng thiếu thông tin tỉnh / quận.");
        }

        // chi nhánh cùng quận
        Optional<Branch> branchOpt = branchRepository
                .findFirstByDistrictIdAndIsActiveTrue(districtId);

        Branch branch = branchOpt.orElseGet(() ->
                // Fallback chi nhánh cùng tỉnh
                branchRepository.findFirstByProvinceIdAndIsActiveTrue(provinceId)
                        .orElseThrow(() -> new RequestException(
                                "Không tìm thấy chi nhánh phù hợp cho tỉnh " + provinceId
                        ))
        );

        //Lấy kho đang hoạt động thuộc chi nhánh
        Warehouse warehouse = warehouseRepository
                .findFirstByBranchIdAndIsActiveTrue(branch.getId())
                .orElseThrow(() -> new RequestException(
                        "Không có kho hoạt động thuộc chi nhánh: " + branch.getName()
                ));

        return warehouse;
    }
    @Override
    public Long getWarehouseIdByAddress(CustomerAddress address) {
        Warehouse warehouse = resolveWarehouseForAddress(address);
        return warehouse != null ? warehouse.getId() : null;
    }
    @Override
    public Long getWarehouseIdByBranch(Long branchId) {
        return warehouseRepository.findFirstByBranchIdAndIsActiveTrue(branchId)
                .map(Warehouse::getId)
                .orElseThrow(() -> new RequestException("Kho không tồn tại"));
    }
    // hàm tìm kho gần nhất có hàng (nếu thiếu thì chuyển kho cùng tỉnh sang)

    @Override
    public Warehouse findOptimalWarehouse(CustomerAddress address, List<CartItemRequest> items) {
        Integer targetProvinceId = address.getProvinceId();

        // Lấy và Sort kho theo khoảng cách
        List<Warehouse> warehouses = warehouseRepository.findActiveWarehousesByProvince(targetProvinceId);
        if (warehouses.isEmpty()) throw new RequestException("Rất tiếc, chưa có kho hàng tại khu vực giao hàng này.");

        warehouses.sort((w1, w2) -> {
            Integer d1 = w1.getBranch().getDistrictId();
            Integer d2 = w2.getBranch().getDistrictId();
            Integer targetD = address.getDistrictId();
            boolean match1 = d1 != null && d1.equals(targetD);
            boolean match2 = d2 != null && d2.equals(targetD);
            if (match1 && !match2) return -1;
            if (!match1 && match2) return 1;
            return 0;
        });

        // Bulk Query Ma trận tồn kho (Giữ nguyên logic Bulk Read để tối ưu hiệu năng)
        List<Long> warehouseIds = warehouses.stream().map(Warehouse::getId).toList();
        List<Long> skuIds = items.stream().map(CartItemRequest::getSkuId).toList();

        // Map<WarehouseID, Map<SkuID, AvailableQty>>
        Map<Long, Map<Long, Integer>> stockMatrix = productStockService.getStockMatrix(warehouseIds, skuIds);

        int totalRequired = items.stream().mapToInt(CartItemRequest::getQuantity).sum();
        double threshold = totalRequired * 0.6; // Ngưỡng 60%

        // Tạo Map<Warehouse, Integer> lưu trữ: Kho A -> Đáp ứng được 5 cái, Kho B -> 10 cái...
        Map<Warehouse, Integer> warehouseCapacityMap = warehouses.stream()
                .collect(Collectors.toMap(
                        wh -> wh,
                        wh -> {
                            // Lấy tồn kho của kho này từ Matrix
                            Map<Long, Integer> whStock = stockMatrix.get(wh.getId());
                            // Tính tổng số lượng kho này có thể cung cấp
                            return items.stream()
                                    .mapToInt(item -> Math.min(whStock.getOrDefault(item.getSkuId(), 0), item.getQuantity()))
                                    .sum();
                        }
                ));

        return warehouses.stream()
                .max((w1, w2) -> {
                    int qty1 = warehouseCapacityMap.get(w1);
                    int qty2 = warehouseCapacityMap.get(w2);

                    // Ưu tiên 1: Ai đáp ứng đủ 100% thì thắng tuyệt đối
                    boolean full1 = qty1 == totalRequired;
                    boolean full2 = qty2 == totalRequired;
                    if (full1 != full2) return Boolean.compare(full1, full2);

                    // Ưu tiên 2: Logic Đảo Hub (> 60%)
                    // Nếu w1 > 60% mà w2 < 60% -> w1 thắng (dù w1 có thể xa hơn)
                    boolean pass1 = qty1 > threshold;
                    boolean pass2 = qty2 > threshold;
                    if (pass1 != pass2) return Boolean.compare(pass1, pass2);

                    // Logic Vét Cạn (Tránh kho 0 hàng) & So sánh số lượng
                    // Nếu cả 2 đều > 60% -> Chọn ông nhiều hàng hơn.
                    //  cả 2 đều < 60% -> Chọn ông nhiều hàng hơn (để đỡ phải chuyển kho nhiều).
                    // bằng điểm nhau -> Stream.max sẽ giữ lại ông xuất hiện trước (tức là gần hơn).
                    return Integer.compare(qty1, qty2);
                })
                .orElse(warehouses.get(0));
    }
}
