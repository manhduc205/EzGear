package com.manhduc205.ezgear.elasticsearch.services;

import com.manhduc205.ezgear.elasticsearch.documents.ProductDocument;
import com.manhduc205.ezgear.elasticsearch.repositories.ProductSearchRepository;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductSKU;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductEsService {

    private final ProductSearchRepository productSearchRepository;

    // chuyên đồng bộ dữ liệu sản phẩm lên Elasticsearch
    @Async
    public void syncProductToEs(Product product) {
        if (product == null) return;

        Long displayPrice = 0L;
        if (product.getProductSkus() != null && !product.getProductSkus().isEmpty()) {
            displayPrice = product.getProductSkus().stream()
                    .filter(sku -> sku.getIsActive() != null && sku.getIsActive())
                    .map(ProductSKU::getPrice)
                    .min(Long::compare)
                    .orElse(0L);
        }

        ProductDocument doc = ProductDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .price(displayPrice)
                .imageUrl(product.getImageUrl())
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .isActive(product.getIsActive())
                .ratingAverage(product.getRatingAverage())
                .reviewCount(product.getReviewCount())
                .createdAt(product.getCreatedAt())
                .build();

        productSearchRepository.save(doc);
    }

    @Async
    public void deleteFromEs(Long id) {
        productSearchRepository.deleteById(id);
    }

    public Page<ProductDocument> searchProducts(String keyword, String sortBy, String order, int page, int size) {
        // Xử lý logic sắp xếp (Mapping từ String sang object Sort của Spring)
        Sort sort = handleSortLogic(sortBy, order);
        Pageable pageable = PageRequest.of(page, size, sort);

        if (keyword == null || keyword.trim().isEmpty()) {
            return productSearchRepository.findAll(pageable);
        }
        return productSearchRepository.findByNameContaining(keyword, pageable);
    }
    // logic sắp xếp
    private Sort handleSortLogic(String sortBy, String order) {
        String sortField = "id";
        if (sortBy != null) {
            switch (sortBy) {
                case "relevance": return Sort.unsorted();
                case "newest": sortField = "createdAt"; break;
                case "price": sortField = "price"; break;
                case "rating": sortField = "ratingAverage"; break;
                default: sortField = "id";
            }
        }
        Sort.Direction direction = (order != null && order.equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortField);
    }
}