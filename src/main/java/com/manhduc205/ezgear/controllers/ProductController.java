package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.dtos.ProductImageDTO;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductImage;
import com.manhduc205.ezgear.repositories.ProductRepository;
import com.manhduc205.ezgear.services.CloudinaryService;
import com.manhduc205.ezgear.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SYS_ADMIN')")
    @PostMapping("")
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductDTO productDTO, BindingResult bindingResult) throws Exception{
        try{
            if(bindingResult.hasErrors()){
                List<String> errorMessage = bindingResult.getFieldErrors()
                        .stream()
                        .map(FieldError::getDefaultMessage)
                        .toList();
                return ResponseEntity.badRequest().body(
                        ApiResponse.builder()
                                .message("Validation failed ")
                                .errors(errorMessage)
                                .build()
                );
            }
            Product newProduct = productService.createProduct(productDTO);
            return ResponseEntity.ok(
                    ApiResponse.builder().success(true)
                            .message("Created product successfully")
                            .payload(newProduct)
                            .build()
            );
        }catch (Exception e){
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .error(e.getMessage())
                    .error("Create product failed")
                    .build());
        }
    }
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SYS_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable("id") Long id,
                                           @RequestBody ProductDTO productDTO){
        try{
            Product updateProduct = productService.updateProduct(id, productDTO);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .payload(updateProduct)
                    .build());
        }catch (Exception e){
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .message("Update product failed")
                    .error(e.getMessage())
                    .build());
        }
    }
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SYS_ADMIN')")
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
                            .message("Delete product failed")
                            .error(e.getMessage())
                            .build());
        }
    }

    private String storeFile(MultipartFile file) throws IOException {
        if (!isImageFile(file) || file.getOriginalFilename() == null) {
            throw new IOException("Invalid image file");
        }

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        // thêm UUID vào trước tên file để đảm bảo tên file là duy nhất
        String uniqueFilename = UUID.randomUUID() + "_" + fileName;
        // đường dẫn đến thư mục mà bạn muốn lưu file
        Path uploadDir = Paths.get("uploads");
        // kiểm tra và tạo thư mục nêú nó không tồn tại
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        // đường dẫn đầy đủ đến file
        Path destination = Paths.get(uploadDir.toString(), uniqueFilename);
        // sao chép file vào thư mục
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFilename;
    }

    // hàm kiểm tra xem có đúng định dạng file ảnh hay không
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SYS_ADMIN')")
    @PostMapping(
            value = "/uploads/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadImages(@PathVariable("id") Long productId,
                                          @ModelAttribute("files") List<MultipartFile> files){
        try{
            Product existsProduct = productService.getProductById(productId);
            files = (files == null) ? new ArrayList<>() : files;
            if(files.size() > ProductImage.MAXIMUM_IMAGES_PER_PRODUCT){
                return ResponseEntity.badRequest().body(ApiResponse.builder()
                        .error("File Required").build());
            }
            List<ProductImage> productImages = new ArrayList<>();
            for(MultipartFile file : files){
                if(file.getSize() == 0){
                    continue;
                }

                // Kiểm tra kích thước, định dạng file
                if(file.getSize() > 10 * 1024 * 1024){
                    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                            ApiResponse.builder()
                                    .error("File Too Large").build()
                    );
                }

                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                            .body(ApiResponse.builder().error("File images type failed").build());
                }

//                 lưu file và cập nhật thumnail trong DTO
//                String fileName = storeFile(file);

                String imageUrl = cloudinaryService.uploadFile(file);

                // lưu vào đối tượng product trong DB ->
                ProductImage productImage = productService.createProductImage(
                        existsProduct.getId(),
                        ProductImageDTO.builder().imageUrl(imageUrl).build()
                );
                productImages.add(productImage);
            }
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Product Image Uploaded")
                    .payload(productImages));
        }catch (Exception e){
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .message("Upload image failed")
                            .error(e.getMessage())
                            .build()
            );
        }

    }

}
