package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Branch;
import com.manhduc205.ezgear.models.Brand;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    @Query(value = """
        SELECT b.* FROM brands b
        INNER JOIN category_brands cb ON b.id = cb.brand_id
        WHERE cb.category_id = :categoryId
    """, nativeQuery = true)
    List<Brand> findBrandsByCategoryId(@Param("categoryId") Long categoryId);
}
