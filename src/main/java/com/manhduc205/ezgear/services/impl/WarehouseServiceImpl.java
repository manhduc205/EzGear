package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.WarehouseDTO;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.Branch;
import com.manhduc205.ezgear.models.CustomerAddress;
import com.manhduc205.ezgear.models.Location;
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
        if (address == null || address.getLocation() == null) {
            throw new RequestException("Địa chỉ giao hàng không hợp lệ hoặc thiếu thông tin địa giới.");
        }

        // 1️⃣ Lấy ra location hiện tại (phường/xã)
        Location location = address.getLocation();

        // 2️⃣ Truy ngược lên tỉnh/thành phố
        Location province = findParentProvince(location);
        if (province == null) {
            throw new RequestException("Không tìm thấy tỉnh/thành cho địa chỉ này.");
        }

        // 3️⃣ Tìm chi nhánh thuộc tỉnh đó
        Branch branch = branchRepository.findByLocationCode(province.getCode())
                .orElseThrow(() -> new RequestException("Không tìm thấy chi nhánh cho tỉnh: " + province.getName()));

        // 4️⃣ Lấy kho đang hoạt động thuộc chi nhánh
        Warehouse warehouse = warehouseRepository.findFirstByBranchIdAndIsActiveTrue(branch.getId())
                .orElseThrow(() -> new RequestException("Không có kho hoạt động thuộc chi nhánh: " + branch.getName()));

        return warehouse.getId();
    }

    /**
     * 🔹 Truy ngược cấp tỉnh (PROVINCE) từ một location bất kỳ
     */
    private Location findParentProvince(Location loc) {
        if (loc == null) return null;
        if (loc.getLevel() == Location.Level.PROVINCE) return loc;
        return findParentProvince(loc.getParent());
    }


}
