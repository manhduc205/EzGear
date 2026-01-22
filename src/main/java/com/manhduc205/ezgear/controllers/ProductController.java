package com.manhduc205.ezgear.controllers;

import com.github.javafaker.Faker;
import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.dtos.ProductImageDTO;
import com.manhduc205.ezgear.dtos.request.AdminProductSearchRequest;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.dtos.responses.product.AdminProductDetailResponse;
import com.manhduc205.ezgear.dtos.responses.product.AdminProductResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductDetailResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductSiblingResponse;
import com.manhduc205.ezgear.elasticsearch.documents.ProductDocument;
import com.manhduc205.ezgear.elasticsearch.services.ProductEsService;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductImage;
import com.manhduc205.ezgear.repositories.ProductRepository;
import com.manhduc205.ezgear.services.CloudinaryService;
import com.manhduc205.ezgear.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ProductEsService productEsService;
    private final ProductRepository productRepository;

    // tìm kiếm bên người dùng
    @GetMapping("/search/es")
    public ResponseEntity<?> searchProductsEs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "relevance") String sortBy, // relevance, newest, price, rating
            @RequestParam(defaultValue = "desc") String order,       // asc, desc
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            Page<ProductDocument> result = productEsService.searchProducts(keyword, sortBy, order, page, limit);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Search success")
                    .payload(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
    // đồng bộ dữ liệu cũ vào ES
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/admin/sync-es")
    public ResponseEntity<?> syncAllDataToElasticsearch() {
        try {
            List<Product> allProducts = productRepository.findAll();
            int count = 0;
            for (Product product : allProducts) {
                productEsService.syncProductToEs(product);
                count++;
            }
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Synced " + count + " products to Elasticsearch successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Sync failed: " + e.getMessage())
                    .build());
        }
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/admin/search")
    public ResponseEntity<?> searchProductsForAdmin(@RequestBody AdminProductSearchRequest request) {
        try {
            Page<AdminProductResponse> result = productService.searchProductsForAdmin(request);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Search products success")
                    .payload(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @GetMapping("/public/category/{slug}")
    public ResponseEntity<?> getProductsByCategory(
            @PathVariable String slug,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        try {
            Page<ProductSiblingResponse> result = productService.getProductsByCategorySlug(slug, brand, page, limit, sort);

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Get products by category success")
                    .payload(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @Valid @ModelAttribute ProductDTO productDTO,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        try {
            AdminProductDetailResponse newProduct = productService.createProduct(productDTO, files);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Created product successfully")
                    .payload(newProduct).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute ProductDTO productDTO,
            BindingResult bindingResult,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {
            AdminProductDetailResponse updatedProduct = productService.updateProduct(id, productDTO, file);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Update product successfully")
                    .payload(updatedProduct)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProductById(@PathVariable("id") Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Delete Product successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Delete product failed")
                            .error(e.getMessage())
                            .build());
        }
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @GetMapping("/{id}/images")
    public ResponseEntity<?> getProductImages(@PathVariable("id") Long productId) {
        try {
            List<ProductImage> images = productService.getImagesByProductId(productId);
            return ResponseEntity.ok(ApiResponse.builder().success(true).payload(images).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder().success(false).message(e.getMessage()).build());
        }
    }

    //  xóa 1 ảnh gallery
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<?> deleteProductImage(@PathVariable("imageId") Long imageId) {
        try {
            productService.deleteProductImage(imageId);
            return ResponseEntity.ok(ApiResponse.builder().success(true).message("Deleted image successfully").build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder().success(false).message(e.getMessage()).build());
        }
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping(value = "/uploads/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImages(@PathVariable("id") Long productId,
                                          @ModelAttribute("files") List<MultipartFile> files) {
        try {
            List<ProductImage> productImages = productService.uploadImages(productId, files);

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Product Images Uploaded")
                    .payload(productImages)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("Upload image failed")
                            .error(e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<?> viewImage(@PathVariable("id") Long imageId) {
        try {
            ProductImage productImage = productService.getProductImageById(imageId);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .payload(productImage)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body( // Trả về 404 chuẩn hơn
                    ApiResponse.builder()
                            .success(false)
                            .message("Image not found")
                            .error(e.getMessage())
                            .build()
            );
        }
    }


//    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
//    @PostMapping("/generate-faceker-products")
//    public ResponseEntity<?> generateFacekerProducts() throws Exception {
//        Faker faker = new Faker(new Locale("vi"));
//        for (int  i = 0; i < 10; i++ ) {
//            String productName = faker.commerce().productName();
//            if (productService.existsProduct(productName)) {
//                continue;
//            }
//            String slug = productName.toLowerCase()
//                    .replaceAll("[^a-z0-9\\s-]", "")
//                    .replaceAll("\\s+", "-");
//
//            String fakeImageUrl = "https://picsum.photos/800/600";
//
//
//            String uploadedImageUrl = cloudinaryService.uploadFileFromUrl(fakeImageUrl);
//
//            ProductDTO productDTO = ProductDTO.builder()
//                    .name(productName)
//                    .shortDesc(faker.lorem().sentence())
//                    .categoryId((long) faker.number().numberBetween(1, 3))
//                    .brandId((long) faker.number().numberBetween(1, 8)) // random brand
//                    .slug(slug)
//                    .imageUrl(uploadedImageUrl)
//                    .warrantyMonths(12)
//                    .isActive(true)
//                    .build();
//
//            try {
//                productService.createProduct(productDTO, null);
//            }catch (Exception e){
//                return ResponseEntity.badRequest().body(e.getMessage());
//            }
//        }
//        return ResponseEntity.ok("Fake product generated successfully");
//
//    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getProductDetail(@PathVariable String slug) {
        ProductDetailResponse response = productService.getProductDetail(slug);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Get product detail success")
                .payload(response)
                .build());
    }

    @GetMapping("/{slug}/related")
    public ResponseEntity<?> getRelatedProducts(@PathVariable String slug) {
        List<ProductSiblingResponse> response = productService.getRelatedProducts(slug);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Get related products success")
                .payload(response)
                .build());
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @GetMapping("/admin/{id}")
    public ResponseEntity<?> getProductById(@PathVariable("id") Long id) {
        try {
            // Service trả về AdminProductDetailResponse
            AdminProductDetailResponse response = productService.getProductById(id);

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Get product successfully")
                    .payload(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder().success(false).message(e.getMessage()).build());
        }
    }
}
