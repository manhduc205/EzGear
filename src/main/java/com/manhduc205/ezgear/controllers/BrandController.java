package com.manhduc205.ezgear.controllers;

import com.manhduc205.ezgear.dto.BrandDTO;
import com.manhduc205.ezgear.dto.responses.ApiResponse;
import com.manhduc205.ezgear.models.Brand;
import com.manhduc205.ezgear.models.Category;
import com.manhduc205.ezgear.services.BrandService;
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
@RequestMapping("/api/brands")
public class BrandController {
    private final BrandService brandService;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SYS_ADMIN')")
    @PostMapping("")
    public ResponseEntity<?> createBrand(@Valid @RequestBody BrandDTO brandDTO, BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
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
            Brand newBrand = brandService.createBrand(brandDTO);
            return ResponseEntity.ok(ApiResponse.builder().success(true)
                    .payload(newBrand)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.builder()
                    .error(e.getMessage())
                    .message("Create Brand failed")
                    .build());
        }
    }

    @GetMapping("")
    public ResponseEntity<?> getAllBrands() {
        List<Brand> brands = brandService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.builder().success(true)
                .payload(brands)
                .build());
    }
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SYS_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBrand(@PathVariable("id") Long id, @RequestBody BrandDTO brandDTO) {
        Brand brand = brandService.updateBrand(id, brandDTO);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .payload(brand)
                .build());
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SYS_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable("id") Long id) {
        try {
            brandService.deleteBrandById(id);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .build());
        }catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.builder()
                    .error(e.getMessage())
                    .message("Delete brand failed")
                    .build());
        }
    }

}
