package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.ProductSkuDTO;
import com.manhduc205.ezgear.dtos.request.ProductSkuSearchRequest;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.repositories.ProductRepository;
import com.manhduc205.ezgear.repositories.ProductSkuRepository;
import com.manhduc205.ezgear.services.ProductSkuService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
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
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        // kiểm tra sku có bị trùng
        if(productSkuRepository.existsBySku(productSkuDTO.getSku())){
            throw new EntityExistsException("Product sku already exists");
        }

        ProductSKU productSKU = ProductSKU.builder()
                .product(product)
                .sku(productSkuDTO.getSku())
                .name(productSkuDTO.getName())
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
                .orElseThrow(() -> new EntityNotFoundException("ProductSKU not found"));

        if(productSkuDTO.getProductId() != null && productSkuDTO.getProductId().equals(productSKU.getProduct().getId())) {
            Product product = productRepository.findById(productSkuDTO.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            productSKU.setProduct(product);
        }

        productSKU.setName(productSkuDTO.getName());
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
                .orElseThrow(() -> new EntityNotFoundException("ProductSKU not found"));
        sku.setIsActive(false);  // soft delete tránh lỗi nếu SKU từng xh trong đơn hàng
        productSkuRepository.save(sku);
    }

    @Override
    public Page<ProductSKU> searchProductSkus(ProductSkuSearchRequest request) {
        Specification<ProductSKU> spec = Specification.allOf();

        // sku
        if(request.getSku() != null && !request.getSku().isEmpty()) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")),"%" + request.getSku().toLowerCase() + "%"));
        }
        if(request.getName() != null && !request.getName().isEmpty()) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),"%" + request.getName().toLowerCase() + "%"));
        }
        if(request.getProductName() != null && !request.getProductName().isEmpty()) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("product").get("name")),
                            "%" + request.getProductName().toLowerCase() + "%"));
        }

        if(request.getBrandName() != null && !request.getBrandName().isEmpty()) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("product").join("brand").get("name")),
                            "%" + request.getBrandName().toLowerCase() + "%"));
        }

        if(request.getCategoryName() != null && !request.getCategoryName().isEmpty()) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("product").join("category").get("name")),
                            "%" + request.getCategoryName().toLowerCase() + "%"));
        }
        if(request.getBarcode() != null && !request.getBarcode().isEmpty()) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("barcode")),
                            "%" + request.getBarcode().toLowerCase() + "%"));
        }
        if(request.getIsActive() != null) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("isActive"), request.getIsActive()));
        }
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            spec = spec.and((root, query, cb) -> {
                if (request.getMinPrice() != null
                        && request.getMaxPrice() != null
                        && request.getMaxPrice().compareTo(BigDecimal.ZERO) > 0) {
                    return cb.between(root.get("price"), request.getMinPrice(), request.getMaxPrice());
                } else if (request.getMinPrice() != null
                        && request.getMinPrice().compareTo(BigDecimal.ZERO) > 0) {
                    return cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice());
                } else if (request.getMaxPrice() != null
                        && request.getMaxPrice().compareTo(BigDecimal.ZERO) > 0) {
                    return cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice());
                } else {
                    return cb.conjunction(); // thay vì null
                }
            });
        }

        int page = (request.getPage() != null && request.getPage() >= 0) ? request.getPage() : 0;
        int size = (request.getSize() != null && request.getSize() > 0) ? request.getSize() : 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productSkuRepository.findAll(spec, pageable);

    }

    @Override
    public ProductSKU getById(Long id) {
        return productSkuRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ProductSKU not found"));
    }

}
