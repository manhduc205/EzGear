package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.dtos.ProductImageDTO;
import com.manhduc205.ezgear.dtos.responses.product.ProductDetailResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductSiblingResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductSkuDetailResponse;
import com.manhduc205.ezgear.exceptions.DataNotFoundException;
import com.manhduc205.ezgear.mapper.ProductMapper;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.services.CategoryService;
import com.manhduc205.ezgear.services.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final ProductImageRepository productImageRepository;
    private final ProductSkuRepository productSkuRepository;
    @Override
    public Product getProductById(Long id) throws DataNotFoundException {
        return productRepository
                .findById(id)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.product.not_found")));
    }

    @Override
    @Transactional
    public Product createProduct(ProductDTO productDTO) throws DataNotFoundException {
        Category existsCategory = categoryRepository
                .findById(productDTO.getCategoryId())
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.category.not_found")));

        Brand existBrand = brandRepository
                .findById(productDTO.getBrandId())
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.brand.not_found")));
        Product product = productMapper.toProduct(productDTO);
        product.setCategory(existsCategory);
        product.setBrand(existBrand);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct( Long id, ProductDTO productDTO) throws DataNotFoundException {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.product.not_found_by_id", id)));

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.category.not_found")));

        Brand brand = brandRepository.findById(productDTO.getBrandId())
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.brand.not_found")));

        // Update fields
        existingProduct.setName(productDTO.getName());
        existingProduct.setSlug(productDTO.getSlug());
        existingProduct.setShortDesc(productDTO.getShortDesc());
        existingProduct.setImageUrl(productDTO.getImageUrl());
        existingProduct.setWarrantyMonths(productDTO.getWarrantyMonths());
        existingProduct.setIsActive(productDTO.getIsActive());
        existingProduct.setCategory(category);
        existingProduct.setBrand(brand);

        return productRepository.save(existingProduct);

    }

    @Override

    public void deleteProduct(Long id) throws Exception {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.product.not_found_by_id", id)));
        productRepository.delete(existingProduct);
    }

    @Override
    public boolean existsProduct(String productName) throws Exception {
        return productRepository.existsByName(productName);
    }

    @Override
    public ProductImage createProductImage(Long productId , ProductImageDTO productImageDTO) throws Exception {
        Product existsProduct = productRepository.findById(productId)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.product.not_found_by_id", productId)));
        ProductImage productImage = ProductImage
                .builder()
                .product(existsProduct)
                .imageUrl(productImageDTO.getImageUrl())
                .build();

        int size = productImageRepository.findByProductId(productId).size();
        if(size >= ProductImage.MAXIMUM_IMAGES_PER_PRODUCT){
            throw new DataNotFoundException(Translator.toLocale(
                    "error.product.image_limit_reached",
                    ProductImage.MAXIMUM_IMAGES_PER_PRODUCT
            ));
        }
        return productImageRepository.save(productImage);
    }

    @Override
    public ProductImage getProductImageById(Long imageId) throws Exception {
        return productImageRepository.findById(imageId)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.product_image.not_found_by_id", imageId)));

    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(String slug) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new EntityNotFoundException(Translator.toLocale("error.product.not_found_or_inactive")));

        List<ProductSKU> skus = productSkuRepository.findByProductIdAndIsActiveTrueOrderByPriceAsc(product.getId());
        List<ProductSkuDetailResponse> skuResponses = skus.stream()
                .map(sku -> ProductSkuDetailResponse.builder()
                        .id(sku.getId())
                        .sku(sku.getSku())
                        .skuName(sku.getName())
                        .optionName(sku.getOptionName())
                        .skuImage(sku.getSkuImage())
                        .price(sku.getPrice())
                        .isStockAvailable(true) // Tạm thời để true, sau này tích hợp stock sau
                        .build())
                .toList();

        // Lấy ảnh
        List<String> galleryImages = productImageRepository.findByProductId(product.getId())
                .stream().map(ProductImage::getImageUrl).toList();

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .seriesCode(product.getSeriesCode())
                .shortDesc(product.getShortDesc())
                .ratingAverage(product.getRatingAverage())
                .reviewCount(product.getReviewCount())
                .skus(skuResponses)
                .galleryImages(galleryImages)
                .build();
    }

    // lấy sản phẩm liên quan cùng series code
    @Override
    @Transactional(readOnly = true)
    public List<ProductSiblingResponse> getRelatedProducts(String slug) {
        Product currentProduct = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new EntityNotFoundException(Translator.toLocale("error.product.not_found")));

        if (currentProduct.getSeriesCode() == null || currentProduct.getSeriesCode().isEmpty()) {
            return new ArrayList<>();
        }

        List<Product> siblings = productRepository.findBySeriesCodeAndIdNot(
                currentProduct.getSeriesCode(),
                currentProduct.getId()
        );

        return siblings.stream().map(p -> {
            // Lấy giá của SKU rẻ nhất
            Long displayPrice = p.getProductSkus().stream()
                    .filter(ProductSKU::getIsActive)
                    .map(ProductSKU::getPrice)
                    .min(Long::compare)
                    .orElse(0L);

            return ProductSiblingResponse.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .slug(p.getSlug())
                    .imageUrl(p.getImageUrl())
                    .price(displayPrice)
                    .isCurrent(false)
                    .build();
        }).toList();
    }
}
