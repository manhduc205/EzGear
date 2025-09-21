package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.repositories.ProductRepository;
import com.manhduc205.ezgear.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

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

}
