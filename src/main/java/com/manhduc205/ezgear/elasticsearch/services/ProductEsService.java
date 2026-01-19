package com.manhduc205.ezgear.elasticsearch.services;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.manhduc205.ezgear.elasticsearch.documents.ProductDocument;
import com.manhduc205.ezgear.elasticsearch.repositories.ProductSearchRepository;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductSKU;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
//thuật toán es
// fuzzy search : tim kiếm mờ
//N-gram / Edge N-gram (Autocomplete)
//Multi-match Query : tìm kiếm trên nhiều trường
public class ProductEsService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

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
    // dùng native query để tìm kiếm sản phẩm
    public Page<ProductDocument> searchProducts(String keyword, String sortBy, String order, int page, int size) {
        // Xử lý logic sắp xếp (Mapping từ String sang object Sort của Spring)
        Sort sortObj = Sort.by(
                (order != null && order.equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC,
                (sortBy == null || sortBy.equals("relevance")) ? "_score" : sortBy // score là độ phù hợp
        );
        Pageable pageable = PageRequest.of(page, size, sortObj);
        //native
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> {
                    // nếu k nhập gì thì trả về tất cả
                    if (keyword == null || keyword.trim().isEmpty()) {
                        return q.matchAll(m -> m);
                    }
                    // Nếu có từ khóa -> Dùng MultiMatch thông minh
                    return q.multiMatch(m -> m
                            .fields("name^3", "brand^2", "category", "shortDesc") // Ưu tiên: Tên (x3 điểm) > Hãng (x2)
                            .query(keyword)
                            .fuzziness("AUTO")
                            .operator(Operator.Or)          // Tìm thấy 1 trong các từ là được
                    );
                })
                .withPageable(pageable)
                .build();
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);
        // Convert kq
        List<ProductDocument> products = searchHits.stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(products, pageable, searchHits.getTotalHits());
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