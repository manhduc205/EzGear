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

import java.util.*;

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
    public List<Warehouse> getAll() {
        return warehouseRepository.findAll();
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

        warehouse.setName(dto.getName());
        warehouse.setCode(dto.getCode());
        warehouse.setIsActive(dto.getIsActive());

        return warehouseRepository.save(warehouse);
    }

    @Override
    public void delete(Long id) {
        warehouseRepository.deleteById(id);
    }

    public Long getWarehouseIdByAddress(CustomerAddress address) {
        // 1️⃣ Lấy province code (ví dụ "HN", "HCM")
        String provinceCode = address.getLocationCode();
        if (provinceCode == null || provinceCode.isEmpty()) {
            throw new RequestException("Địa chỉ giao hàng chưa có mã tỉnh/thành (locationCode).");
        }

        // 2️⃣ Tìm chi nhánh theo mã tỉnh
        Branch branch = branchRepository.findByCode(provinceCode)
                .orElseThrow(() -> new RequestException("Không tìm thấy chi nhánh cho tỉnh: " + provinceCode));

        // 3️⃣ Tìm kho hoạt động của chi nhánh đó
        Warehouse warehouse = warehouseRepository.findFirstByBranchIdAndIsActiveTrue(branch.getId())
                .orElseThrow(() -> new RequestException("Không tìm thấy kho hoạt động cho chi nhánh: " + branch.getName()));

        // 4️⃣ Trả về id
        return warehouse.getId();
    }


}
