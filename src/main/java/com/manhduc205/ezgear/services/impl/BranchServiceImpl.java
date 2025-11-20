package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.BranchDTO;
import com.manhduc205.ezgear.models.Branch;
import com.manhduc205.ezgear.repositories.BranchRepository;
import com.manhduc205.ezgear.services.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepo;

    @Override
    public Branch createBranch(@Valid BranchDTO dto) {
        Branch branch = new Branch();
        branch.setCode(dto.getCode());
        branch.setName(dto.getName());
        branch.setProvinceId(dto.getProvinceId());
        branch.setDistrictId(dto.getDistrictId());
        branch.setWardCode(dto.getWardCode());
        branch.setAddressLine(dto.getAddressLine());
        branch.setPhone(dto.getPhone());
        branch.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE);
        return branchRepo.save(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchDTO> getAll() {
        return branchRepo.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    private BranchDTO toDTO(Branch branch) {
        return BranchDTO.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .provinceId(branch.getProvinceId())
                .districtId(branch.getDistrictId())
                .wardCode(branch.getWardCode())
                .addressLine(branch.getAddressLine())
                .phone(branch.getPhone())
                .isActive(branch.getIsActive())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Branch> getById(Long id) {
        return branchRepo.findById(id);
    }

    @Override
    public Branch updateBranch(Long id, @Valid BranchDTO dto) {
        Branch branch = branchRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        if (dto.getCode() != null) branch.setCode(dto.getCode());
        if (dto.getName() != null) branch.setName(dto.getName());
        if (dto.getProvinceId() != null) branch.setProvinceId(dto.getProvinceId());
        if (dto.getDistrictId() != null) branch.setDistrictId(dto.getDistrictId());
        if (dto.getWardCode() != null) branch.setWardCode(dto.getWardCode());
        if (dto.getAddressLine() != null) branch.setAddressLine(dto.getAddressLine());
        if (dto.getPhone() != null) branch.setPhone(dto.getPhone());
        if (dto.getIsActive() != null) branch.setIsActive(dto.getIsActive());

        return branchRepo.save(branch);
    }

    @Override
    public void delete(Long id) {
        branchRepo.deleteById(id);
    }
}
