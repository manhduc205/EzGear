package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.BranchDTO;
import com.manhduc205.ezgear.models.Branch;

import java.util.List;
import java.util.Optional;

public interface BranchService {
    Branch createBranch(BranchDTO dto);
    List<BranchDTO> getAll();
    Optional<Branch> getById(Long id);
    Branch updateBranch(Long id, BranchDTO dto);
    void delete(Long id);
}

