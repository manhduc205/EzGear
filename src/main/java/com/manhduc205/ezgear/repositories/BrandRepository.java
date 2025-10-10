package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Branch;
import com.manhduc205.ezgear.models.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    // tìm chi nhánh theo tỉnh thành
    Optional<Branch> findByProvince(String province);
}
