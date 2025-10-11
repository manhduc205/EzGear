package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch,Long> {
    // tìm chi nhánh theo tỉnh thành
    Optional<Branch> findByCode(String code);
}
