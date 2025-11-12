package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.BranchDTO;
import com.manhduc205.ezgear.models.Branch;
import com.manhduc205.ezgear.models.Location;
import com.manhduc205.ezgear.repositories.BranchRepository;
import com.manhduc205.ezgear.repositories.LocationRepository;
import com.manhduc205.ezgear.services.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepo;
    private final LocationRepository locationRepo;

    @Override
    public Branch createBranch(BranchDTO dto) {
        Location location = locationRepo.findByCode(dto.getLocationCode())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã Location"));
        Branch branch = Branch.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .location(location)
                .addressLine(dto.getAddressLine())
                .phone(dto.getPhone())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        return branchRepo.save(branch);
    }

    @Override
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
                .locationCode(branch.getLocation() != null ? branch.getLocation().getCode() : null)
                .addressLine(branch.getAddressLine())
                .phone(branch.getPhone())
                .isActive(branch.getIsActive())
                .build();
    }
    @Override
    public Optional<Branch> getById(Long id) {
        return branchRepo.findById(id);
    }

    @Override
    public Branch updateBranch(Long id, BranchDTO dto) {
        Branch branch = branchRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        if (dto.getName() != null)
            branch.setName(dto.getName());
        if (dto.getLocationCode() != null) {
            Location location = locationRepo.findByCode(dto.getLocationCode())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Location với mã: " + dto.getLocationCode()));
            branch.setLocation(location);
        }
        if (dto.getAddressLine() != null)
            branch.setAddressLine(dto.getAddressLine());
        if (dto.getPhone() != null)
            branch.setPhone(dto.getPhone());
        if (dto.getIsActive() != null)
            branch.setIsActive(dto.getIsActive());

        return branchRepo.save(branch);
    }

    @Override
    public void delete(Long id) {
        branchRepo.deleteById(id);
    }
}

