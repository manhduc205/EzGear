package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.dtos.ProductImageDTO;
import com.manhduc205.ezgear.dtos.request.AdminProductSearchRequest; // 🟢 Import DTO Request
import com.manhduc205.ezgear.dtos.responses.product.*; // 🟢 Import DTO Response
import com.manhduc205.ezgear.elasticsearch.services.ProductEsService;
import com.manhduc205.ezgear.exceptions.DataNotFoundException;
import com.manhduc205.ezgear.mapper.ProductMapper;
import com.manhduc205.ezgear.models.*;
import com.manhduc205.ezgear.repositories.*;
import com.manhduc205.ezgear.services.CategoryService;
import com.manhduc205.ezgear.services.CloudinaryService;
import com.manhduc205.ezgear.services.ProductService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate; // 🟢 Cần cho Specification
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page; // 🟢 Cần cho phân trang
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSkuRepository productSkuRepository;
    private final CloudinaryService cloudinaryService;
    private final ProductEsService productEsService;

    @Override
    public Page<AdminProductResponse> searchProductsForAdmin(AdminProductSearchRequest request) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), keyword);
                Predicate seriesLike = cb.like(cb.lower(root.get("seriesCode")), keyword);
                predicates.add(cb.or(nameLike, seriesLike));
            }

            if (request.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), request.getCategoryId()));
            }

            if (request.getBrandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), request.getBrandId()));
            }

            if (request.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), request.getIsActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Phân trang
        int page = (request.getPage() != null) ? request.getPage() : 0;
        int size = (request.getSize() != null) ? request.getSize() : 10;
        Sort sort = Sort.by("id").descending(); // Mặc định mới nhất lên đầu

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // Map sang DTO
        return productPage.map(product -> AdminProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .seriesCode(product.getSeriesCode())
                .imageUrl(product.getImageUrl())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "N/A")
                .brandName(product.getBrand() != null ? product.getBrand().getName() : "N/A")
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build());
    }


    @Override
    public AdminProductDetailResponse getProductById(Long id) throws DataNotFoundException {
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.product.not_found")));
        return productMapper.toDetailResponse(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // Rollback nếu upload lỗi
    public AdminProductDetailResponse createProduct(ProductDTO productDTO, List<MultipartFile> files) throws Exception {
        Category existsCategory = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.category.not_found")));
        Brand existBrand = brandRepository.findById(productDTO.getBrandId())
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.brand.not_found")));

        Product product = productMapper.toProduct(productDTO);
        product.setCategory(existsCategory);
        product.setBrand(existBrand);
        product.setSeriesCode(productDTO.getSeriesCode());
        if(productDTO.getSlug() != null) product.setSlug(productDTO.getSlug());
        if(productDTO.getIsActive() == null) product.setIsActive(true);

        // XỬ LÝ ẢNH CHÍNH (Lấy file đầu tiên)
        if (files != null && !files.isEmpty()) {
            MultipartFile mainFile = files.get(0);
            if(mainFile.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException(Translator.toLocale("error.product.main_image_too_large"));

            String mainImageUrl = cloudinaryService.uploadFile(mainFile);
            product.setImageUrl(mainImageUrl);
        } else if (productDTO.getImageUrl() != null) {
            product.setImageUrl(productDTO.getImageUrl());
        }

        Product savedProduct = productRepository.save(product);

        // XỬ LÝ ẢNH PHỤ (Lấy từ file thứ 2 trở đi)
        if (files != null && files.size() > 1) {
            List<ProductImage> productImages = new ArrayList<>();

            for (int i = 1; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if(file.getSize() == 0) continue;
                if(file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException(Translator.toLocale("error.product.gallery_image_too_large"));

                // Upload Cloudinary
                String galleryUrl = cloudinaryService.uploadFile(file);

                // Tạo Entity ProductImage
                ProductImage productImage = ProductImage.builder()
                        .product(savedProduct)
                        .imageUrl(galleryUrl)
                        .build();

                productImages.add(productImage);
            }

            // Validate số lượng ảnh (nếu cần)
            if(productImages.size() > ProductImage.MAXIMUM_IMAGES_PER_PRODUCT) {
                throw new IllegalArgumentException(Translator.toLocale("error.product.too_many_images"));
            }

            // Lưu tất cả ảnh phụ
            if(!productImages.isEmpty()) {
                productImageRepository.saveAll(productImages);
            }
        }
        productEsService.syncProductToEs(savedProduct); // đồng bộ sang ES
        return productMapper.toDetailResponse(savedProduct);
    }

    @Override
    @Transactional
    public AdminProductDetailResponse updateProduct(Long id, ProductDTO productDTO, MultipartFile imageFile) throws IOException {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.product.not_found_by_id", id)));

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.category.not_found")));

        Brand brand = brandRepository.findById(productDTO.getBrandId())
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.brand.not_found")));

        // Update thông tin cơ bản
        existingProduct.setName(productDTO.getName());
        existingProduct.setSlug(productDTO.getSlug());
        existingProduct.setShortDesc(productDTO.getShortDesc());
        existingProduct.setWarrantyMonths(productDTO.getWarrantyMonths());
        existingProduct.setIsActive(productDTO.getIsActive());
        existingProduct.setCategory(category);
        existingProduct.setBrand(brand);
        existingProduct.setSeriesCode(productDTO.getSeriesCode());

        // Logic cập nhật ảnh: Ưu tiên File Upload > URL Text > Giữ nguyên
        if (imageFile != null && !imageFile.isEmpty()) {
            if(imageFile.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("File size too large");
            }
            String newImageUrl = cloudinaryService.uploadFile(imageFile);
            existingProduct.setImageUrl(newImageUrl);
        } else if (productDTO.getImageUrl() != null && !productDTO.getImageUrl().isEmpty()) {
            existingProduct.setImageUrl(productDTO.getImageUrl());
        }

        Product updatedProduct = productRepository.save(existingProduct);
        productEsService.syncProductToEs(updatedProduct); // đồng bộ sang ES
        return productMapper.toDetailResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) throws Exception {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.product.not_found_by_id", id)));

        existingProduct.setIsActive(false);
        productRepository.save(existingProduct);
        productEsService.deleteFromEs(id);
        productSkuRepository.softDeleteByProductId(id);
    }

    @Override
    public boolean existsProduct(String productName) throws Exception {
        return productRepository.existsByName(productName);
    }

    @Override
    public ProductImage createProductImage(Long productId , ProductImageDTO productImageDTO)  {
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
    public List<ProductImage> getImagesByProductId(Long productId) {
        // Hàm này lấy toàn bộ ảnh phụ (Gallery)
        return productImageRepository.findByProductId(productId);
    }

    @Override
    @Transactional
    public void deleteProductImage(Long imageId) throws Exception {
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.product_image.not_found_by_id", imageId)));

        cloudinaryService.deleteFile(productImage.getImageUrl());
        productImageRepository.delete(productImage);
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
                        .isStockAvailable(true)
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
                .imageUrl(product.getImageUrl())
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProductImage> uploadImages(Long productId, List<MultipartFile> files) throws Exception {
        Product existsProduct = productRepository.findById(productId)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.product.not_found_by_id", productId)));
        files = (files == null) ? new ArrayList<>() : files;

        // 1. Check số lượng ảnh tối đa
        int currentImages = existsProduct.getProductImages().size();
        if (currentImages + files.size() > ProductImage.MAXIMUM_IMAGES_PER_PRODUCT) {
            throw new IllegalArgumentException(Translator.toLocale("error.product.upload_limit_exceeded", ProductImage.MAXIMUM_IMAGES_PER_PRODUCT));
        }

        List<ProductImage> savedImages = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.getSize() == 0) continue;

            // 2. Validate Kích thước (>10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException(Translator.toLocale("error.product.file_size_too_large_10mb"));
            }

            // 3. Validate Loại file (Phải là ảnh)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException(Translator.toLocale("error.product.file_must_be_image"));
            }

            // 4. Upload Cloudinary & Lưu Database
            String imageUrl = cloudinaryService.uploadFile(file);

            ProductImage productImage = ProductImage.builder()
                    .product(existsProduct)
                    .imageUrl(imageUrl)
                    .build();

            savedImages.add(productImageRepository.save(productImage));
        }

        return savedImages;
    }
    @Override
    @Transactional(readOnly = true)
    public Page<ProductSiblingResponse> getProductsByCategorySlug(String categorySlug, String brandSlug, int page, int limit, String sortStr) {

        Sort sort = Sort.by("createdAt").descending(); // Mặc định: Mới nhất

        if (sortStr != null && !sortStr.isEmpty()) {
            switch (sortStr.toLowerCase()) {
                case "top-rated":
                    sort = Sort.by("ratingAverage").descending()
                            .and(Sort.by("reviewCount").descending());
                    break;

                case "most-reviewed":
                    sort = Sort.by("reviewCount").descending();
                    break;

                case "best-selling":
                    sort = Sort.by("soldCount").descending();
                    break;

                // 🆕 Case 4: Mới nhất
                case "latest":
                case "newest":
                    sort = Sort.by("createdAt").descending();
                    break;

                default:
                    // Mặc định giữ nguyên là mới nhất
                    break;
            }
        }

        Pageable pageable = PageRequest.of(page, limit, sort);

        Page<Product> productPage = productRepository.findByCategoryAndBrand(categorySlug, brandSlug, pageable);

        return productPage.map(p -> {
            // Logic lấy giá hiển thị (Giá rẻ nhất trong các biến thể Active)
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
                    .ratingAverage(p.getRatingAverage() != null ? p.getRatingAverage() : 0.0)
                    .reviewCount(p.getReviewCount() != null ? p.getReviewCount() : 0)
                    .soldCount(p.getSoldCount())
                    .build();
        });
    }
}
