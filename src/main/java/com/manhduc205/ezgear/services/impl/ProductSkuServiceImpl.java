package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.ProductSkuDTO;
import com.manhduc205.ezgear.dtos.request.ProductSkuSearchRequest;
import com.manhduc205.ezgear.dtos.responses.product.ProductThumbnailResponse;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.repositories.ProductRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.services.ProductSkuService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl implements ProductSkuService {
    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;

    @Override
    public ProductSKU createProductSku(ProductSkuDTO productSkuDTO) {
        Product product = productRepository.findById(productSkuDTO.getProductId())
                .orElseThrow(() -> new EntityNotFoundException(
                        Translator.toLocale("error.product.not_found_by_id", productSkuDTO.getProductId())
                ));

        // kiểm tra sku có bị trùng
        if(productSkuRepository.existsBySku(productSkuDTO.getSku())){
            throw new EntityExistsException(Translator.toLocale("error.product_sku.duplicate_sku"));
        }

        ProductSKU productSKU = ProductSKU.builder()
                .product(product)
                .sku(productSkuDTO.getSku())
                .name(productSkuDTO.getName())
                .optionName(productSkuDTO.getOptionName())
                .skuImage(productSkuDTO.getSkuImage())
                .price(productSkuDTO.getPrice())
                .barcode(productSkuDTO.getBarcode())
                .weightGram(productSkuDTO.getWeightGram())
                .lengthCm(productSkuDTO.getLengthCm())
                .widthCm(productSkuDTO.getWidthCm())
                .heightCm(productSkuDTO.getHeightCm())
                .isActive(productSkuDTO.getIsActive())
                .build();
        return productSkuRepository.save(productSKU);
    }

    @Override
    public ProductSKU updateProductSku(Long id, ProductSkuDTO productSkuDTO) {
        ProductSKU productSKU = productSkuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        Translator.toLocale("error.product_sku.not_found_by_id", id)
                ));

        if(productSkuDTO.getProductId() != null && productSkuDTO.getProductId().equals(productSKU.getProduct().getId())) {
            Product product = productRepository.findById(productSkuDTO.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            Translator.toLocale("error.product.not_found_by_id", productSkuDTO.getProductId())
                    ));
            productSKU.setProduct(product);
        }

        productSKU.setName(productSkuDTO.getName());
        productSKU.setOptionName(productSkuDTO.getOptionName());
        productSKU.setSkuImage(productSkuDTO.getSkuImage());
        productSKU.setPrice(productSkuDTO.getPrice());
        productSKU.setBarcode(productSkuDTO.getBarcode());
        productSKU.setWeightGram(productSkuDTO.getWeightGram());
        productSKU.setLengthCm(productSkuDTO.getLengthCm());
        productSKU.setWidthCm(productSkuDTO.getWidthCm());
        productSKU.setHeightCm(productSkuDTO.getHeightCm());
        if (productSkuDTO.getIsActive() != null) {
            productSKU.setIsActive(productSkuDTO.getIsActive());
        }
        return productSkuRepository.save(productSKU);
    }
    @Override
    public void deleteProductSku(Long id) {
        ProductSKU sku = productSkuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        Translator.toLocale("error.product_sku.not_found_by_id", id)
                ));
        sku.setIsActive(false);  // soft delete tránh lỗi nếu SKU từng xh trong đơn hàng
        productSkuRepository.save(sku);
    }

    @Override
    public Page<ProductThumbnailResponse> searchProductSkus(ProductSkuSearchRequest request) {
        Specification<ProductSKU> spec = (root, query, cb) -> {
            // FIX N+1: Chỉ fetch Join Product khi đây là query lấy dữ liệu
            if (Long.class != query.getResultType()) {
                root.fetch("product", JoinType.LEFT);
            }

            boolean activeStatus = (request.getIsActive() != null) ? request.getIsActive() : true;
            Predicate skuActive = cb.equal(root.get("isActive"), activeStatus);
            Predicate parentActive = cb.equal(root.join("product").get("isActive"), true);
            return cb.and(skuActive, parentActive);
        };

        // 2. Các bộ lọc tìm kiếm (Giữ nguyên)
        if (request.getName() != null && !request.getName().isEmpty()) {
            String keyword = "%" + request.getName().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), keyword),
                    cb.like(cb.lower(root.get("optionName")), keyword),
                    cb.like(cb.lower(root.join("product").get("name")), keyword)
            ));
        }

        if (request.getSku() != null && !request.getSku().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("sku")), "%" + request.getSku().toLowerCase() + "%"));
        }

        if (request.getBrandName() != null && !request.getBrandName().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("product").join("brand").get("name")),
                            "%" + request.getBrandName().toLowerCase() + "%"));
        }

        if (request.getCategoryName() != null && !request.getCategoryName().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("product").join("category").get("name")),
                            "%" + request.getCategoryName().toLowerCase() + "%"));
        }

        if (request.getBarcode() != null && !request.getBarcode().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("barcode")), "%" + request.getBarcode().toLowerCase() + "%"));
        }

        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            spec = spec.and((root, query, cb) -> {
                if (request.getMinPrice() != null && request.getMaxPrice() != null) {
                    return cb.between(root.get("price"), request.getMinPrice(), request.getMaxPrice());
                } else if (request.getMinPrice() != null) {
                    return cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice());
                } else {
                    return cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice());
                }
            });
        }

        // 3. Phân trang & Query
        int page = (request.getPage() != null && request.getPage() >= 0) ? request.getPage() : 0;
        int size = (request.getSize() != null && request.getSize() > 0) ? request.getSize() : 12;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<ProductSKU> skuPage = productSkuRepository.findAll(spec, pageable);

        return skuPage.map(sku -> {
            Product parent = sku.getProduct(); // Lấy từ Cache L1

            // Logic ghép tên
            String displayName = parent.getName();
            if (sku.getOptionName() != null && !sku.getOptionName().isEmpty()) {
                displayName += " (" + sku.getOptionName() + ")";
            }

            // Logic fallback ảnh
            String displayImage = (sku.getSkuImage() != null && !sku.getSkuImage().isEmpty())
                    ? sku.getSkuImage()
                    : parent.getImageUrl();

            return ProductThumbnailResponse.builder()
                    .id(sku.getId())
                    .skuCode(sku.getSku())
                    .name(displayName)
                    .slug(parent.getSlug())
                    .price(sku.getPrice())
                    .imageUrl(displayImage)
                    .isStockAvailable(true)
                    .build();
        });
    }

    @Override
    public ProductSKU getById(Long id) {
        return productSkuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        Translator.toLocale("error.product_sku.not_found_by_id", id)
                ));
    }

}
