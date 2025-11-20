package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.WarehouseDTO;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.Branch;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Warehouse;
import com.manhduc205.ezgear.repositories.BranchRepository;
import com.manhduc205.ezgear.repositories.WarehouseRepository;
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

        // 1) Tìm chi nhánh cùng quận
        Optional<Branch> branchOpt = branchRepository
                .findFirstByDistrictIdAndIsActiveTrue(districtId);

        Branch branch = branchOpt.orElseGet(() ->
                // 2) Fallback: tìm chi nhánh cùng tỉnh
                branchRepository.findFirstByProvinceIdAndIsActiveTrue(provinceId)
                        .orElseThrow(() -> new RequestException(
                                "Không tìm thấy chi nhánh phù hợp cho tỉnh " + provinceId
                        ))
        );

        // 3) Lấy kho đang hoạt động thuộc chi nhánh
        Warehouse warehouse = warehouseRepository
                .findFirstByBranchIdAndIsActiveTrue(branch.getId())
                .orElseThrow(() -> new RequestException(
                        "Không có kho hoạt động thuộc chi nhánh: " + branch.getName()
                ));

        return warehouse;
    }
}
