package com.manhduc205.ezgear.repositories;

import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {
    boolean existsByName(String name);
    // Tìm sản phẩm active theo slug
    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    // Tìm các sản phẩm cùng Series Code (trừ chính nó)
    List<Product> findBySeriesCodeAndIdNot(String seriesCode, Long id);
}
