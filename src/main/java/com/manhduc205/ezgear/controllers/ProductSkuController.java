package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dtos.ProductSkuDTO;
import com.manhduc205.ezgear.dtos.request.AdminProductSkuSearchRequest;
import com.manhduc205.ezgear.dtos.request.ProductSkuSearchRequest;
import com.manhduc205.ezgear.dtos.responses.ApiResponse;
import com.manhduc205.ezgear.dtos.responses.product.AdminProductSkuResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductThumbnailResponse;
import com.manhduc205.ezgear.models.ProductSKU;
import com.manhduc205.ezgear.services.ProductSkuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product-skus")
@RequiredArgsConstructor
public class ProductSkuController {
    private final ProductSkuService productSkuService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("")
    public ResponseEntity<?> createProductSku (@Valid @RequestBody ProductSkuDTO productSkuDTO, BindingResult result) {
        ProductSKU productSKU = productSkuService.createProductSku(productSkuDTO);
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Product sku created successfully")
                        .payload(productSKU)
                        .build()
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/{id}")
    public ResponseEntity<?> updateProductSku (@Valid @RequestBody ProductSkuDTO productSkuDTO, @PathVariable Long id){
        ProductSKU productSKU = productSkuService.updateProductSku(id,productSkuDTO);
        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Product sku updated successfully")
                        .payload(productSKU)
                        .build()
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProductSku(@PathVariable Long id) {
        productSkuService.deleteProductSku(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Delete SKU successfully")
                .build());
    }


    @PostMapping("/search")
    public ResponseEntity<?> searchProductSku (@Valid @RequestBody ProductSkuSearchRequest request) {
        Page<ProductThumbnailResponse> result = productSkuService.searchProductSkus(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .payload(result)
                .message("Product sku searched successfully")
                .build());
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    @PostMapping("/admin/search")
    public ResponseEntity<?> searchProductSkuForAdmin(@RequestBody AdminProductSkuSearchRequest request) {
        Page<AdminProductSkuResponse> result = productSkuService.searchProductSkusForAdmin(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Admin search success")
                .payload(result)
                .build());
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getSkuDetail(@PathVariable Long id) {
        ProductSkuDTO response = productSkuService.getSkuDetail(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .payload(response)
                .build());
    }

}
