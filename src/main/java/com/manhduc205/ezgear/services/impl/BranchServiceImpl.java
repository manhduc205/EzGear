package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.BranchDTO;
import com.manhduc205.ezgear.models.Branch;
import com.manhduc205.ezgear.repositories.BranchRepository;
import com.manhduc205.ezgear.services.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepo;

    @Override
    public Branch createBranch(BranchDTO dto) {
        Branch branch = Branch.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .province(dto.getProvince())
                .addressLine(dto.getAddressLine())
                .phone(dto.getPhone())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        return branchRepo.save(branch);
    }

    @Override
    public List<Branch> getAll() {
        return branchRepo.findAll();
    }

    @Override
    public Optional<Branch> getById(Long id) {
        return branchRepo.findById(id);
    }

    @Override
    public Branch updateBranch(Long id, BranchDTO dto) {
        Branch branch = branchRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        branch.setName(dto.getName());
        branch.setProvince(dto.getProvince());
        branch.setAddressLine(dto.getAddressLine());
        branch.setPhone(dto.getPhone());
        branch.setIsActive(dto.getIsActive());

        return branchRepo.save(branch);
    }

    @Override
    public void delete(Long id) {
        branchRepo.deleteById(id);
    }
}

