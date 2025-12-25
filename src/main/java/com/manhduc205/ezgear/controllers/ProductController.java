package com.manhduc205.ezgear.controllers;

import com.github.javafaker.Faker;
import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.dtos.ProductImageDTO;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductDetailResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductSiblingResponse;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductImage;
import com.manhduc205.ezgear.services.CloudinaryService;
import com.manhduc205.ezgear.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final CloudinaryService cloudinaryService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @Valid @RequestPart ProductDTO productDTO,
            BindingResult bindingResult,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        try {
            if (bindingResult.hasErrors()) {
                List<String> errorMessage = bindingResult.getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .toList();
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .message("Validation failed")
                                .errors(errorMessage)
                                .build()
                );
            }

            if (files != null && !files.isEmpty()) {
                String mainImageUrl = cloudinaryService.uploadFile(files.get(0));
                productDTO.setImageUrl(mainImageUrl);
            }

            Product newProduct = productService.createProduct(productDTO);


            if (files != null && files.size() > 1) {
                for (int i = 1; i < files.size(); i++) {
                    MultipartFile file = files.get(i);
                    if(file.getSize() == 0) continue;

                    String imageUrl = cloudinaryService.uploadFile(file);
                    productService.createProductImage(
                            newProduct.getId(),
                            ProductImageDTO.builder().imageUrl(imageUrl).build()
                    );
                }
            }

            return ResponseEntity.ok(
                    ApiResponse.builder().success(true)
                            .message("Created product successfully")
                            .payload(newProduct)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductDTO productDTO,
            BindingResult bindingResult
    ) {
        try {

            if (bindingResult.hasErrors()) {
                List<String> errorMessage = bindingResult.getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .toList();
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .message("Validation failed")
                                .errors(errorMessage)
                                .build()
                );
            }

            Product updateProduct = productService.updateProduct(id, productDTO);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Update product successfully")
                    .payload(updateProduct)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .message("Update product failed")
                    .error(e.getMessage())
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
    @PostMapping(value = "/uploads/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImages(@PathVariable("id") Long productId,
                                          @ModelAttribute("files") List<MultipartFile> files) {
        try {
            Product existsProduct = productService.getProductById(productId);
            files = (files == null) ? new ArrayList<>() : files;

            int currentImages = existsProduct.getProductImages().size();
            if (currentImages + files.size() > ProductImage.MAXIMUM_IMAGES_PER_PRODUCT) {
                return ResponseEntity.badRequest().body(ApiResponse.builder()
                        .error("You can only upload max " + ProductImage.MAXIMUM_IMAGES_PER_PRODUCT + " images").build());
            }

            List<ProductImage> productImages = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file.getSize() == 0) continue;
                if(file.getSize() > 20 * 1024 * 1024){
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                            ApiResponse.builder()
                                    .error("File is too large! Maximum size is 20MB").build()
                    );
                }
                // Validate file ảnh
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                            .body(ApiResponse.builder().error("File must be an image").build());
                }

                String imageUrl = cloudinaryService.uploadFile(file);
                ProductImage productImage = productService.createProductImage(
                        existsProduct.getId(),
                        ProductImageDTO.builder().imageUrl(imageUrl).build()
                );
                productImages.add(productImage);
            }
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


    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/generate-faceker-products")
    public ResponseEntity<?> generateFacekerProducts() throws Exception {
        Faker faker = new Faker(new Locale("vi"));
        for (int  i = 0; i < 10; i++ ) {
            String productName = faker.commerce().productName();
            if (productService.existsProduct(productName)) {
                continue;
            }
            String slug = productName.toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-");

            String fakeImageUrl = "https://picsum.photos/800/600";


            String uploadedImageUrl = cloudinaryService.uploadFileFromUrl(fakeImageUrl);

            ProductDTO productDTO = ProductDTO.builder()
                    .name(productName)
                    .shortDesc(faker.lorem().sentence())
                    .categoryId((long) faker.number().numberBetween(1, 3))
                    .brandId((long) faker.number().numberBetween(1, 8)) // random brand
                    .slug(slug)
                    .imageUrl(uploadedImageUrl)
                    .warrantyMonths(12)
                    .isActive(true)
                    .build();

            try {
                productService.createProduct(productDTO);
            }catch (Exception e){
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
        return ResponseEntity.ok("Fake product generated successfully");

    }

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
}