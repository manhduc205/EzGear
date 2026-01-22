package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductImage;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> , JpaSpecificationExecutor {
    boolean existsByName(String name);
    // Tìm sản phẩm active theo slug
    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    // Tìm các sản phẩm cùng Series Code (trừ chính nó)
    List<Product> findBySeriesCodeAndIdNot(String seriesCode, Long id);

    @Query("SELECT p FROM Product p " +
            "JOIN p.category c " +
            "LEFT JOIN p.brand b " + // Join bảng Brand
            "WHERE c.slug = :categorySlug " +
            "AND (:brandSlug IS NULL OR b.slug = :brandSlug) " +
            "AND p.isActive = true")
    Page<Product> findByCategoryAndBrand(
            @Param("categorySlug") String categorySlug,
            @Param("brandSlug") String brandSlug,
            Pageable pageable
    );

    // cộng sl đã bán ( Completed)
    @Modifying
    @Query("UPDATE Product p SET p.soldCount = p.soldCount + :qty WHERE p.id = :id")
    void incrementSoldCount(@Param("id") Long productId, @Param("qty") int quantity);
}
