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
import java.util.Optional;

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

        // Lấy tất cả kho Active trong tỉnh
        List<Warehouse> warehouses = warehouseRepository.findActiveWarehousesByProvince(targetProvinceId);

        if (warehouses.isEmpty()) {
            throw new RequestException("Rất tiếc, chưa có kho hàng tại khu vực giao hàng này.");
        }

        // Sắp xếp kho theo khoảng cách (Ưu tiên cùng Quận/Huyện)
        warehouses.sort((w1, w2) -> {
            Integer d1 = w1.getBranch().getDistrictId();
            Integer d2 = w2.getBranch().getDistrictId();
            Integer targetD = address.getDistrictId();

            boolean match1 = d1 != null && d1.equals(targetD);
            boolean match2 = d2 != null && d2.equals(targetD);

            if (match1 && !match2) return -1; // w1 gần hơn -> lên trước
            if (!match1 && match2) return 1;
            return 0;
        });

        // ƯU TIÊN 1: Tìm kho nào CÓ ĐỦ TẤT CẢ HÀNG
        for (Warehouse wh : warehouses) {
            boolean isFullStock = true;
            for (CartItemRequest item : items) {
                int available = productStockService.getAvailable(item.getSkuId(), wh.getId());
                if (available < item.getQuantity()) {
                    isFullStock = false;
                    break;
                }
            }
            if (isFullStock) {
                return wh; // Chọn ngay kho này
            }
        }

        // ƯU TIÊN 2 : Chọn kho GẦN NHẤT (đầu danh sách)
        return warehouses.get(0);
    }


}
