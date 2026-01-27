package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.BrandDTO;
import com.manhduc205.ezgear.models.Brand;
import com.manhduc205.ezgear.repositories.BrandRepository;
import com.manhduc205.ezgear.services.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandRepository brandRepository;

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    public Brand createBrand(BrandDTO brandDTO) {
        Brand newBrand = Brand.builder()
                .name(brandDTO.getName())
                .slug(brandDTO.getSlug())
                .build();
        return brandRepository.save(newBrand);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandDTO> getAllBrands() {
        return brandRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Brand getBrandById(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new RuntimeException(Translator.toLocale("error.brand.not_found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandDTO> getBrandsByCategory(Long categoryId) {
        List<Brand> brands = brandRepository.findBrandsByCategoryId(categoryId);
        return brands.stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    public Brand updateBrand(Long brandId, BrandDTO brandDTO) {
        Brand oldBrand = getBrandById(brandId);
        oldBrand.setName(brandDTO.getName());
        oldBrand.setSlug(brandDTO.getSlug());
        return brandRepository.save(oldBrand);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'SYS_ADMIN')")
    public void deleteBrandById(Long brandId) {
        if (!brandRepository.existsById(brandId)) {
            throw new RuntimeException(Translator.toLocale("error.brand.not_found"));
        }
        brandRepository.deleteById(brandId);
    }

    private BrandDTO mapToDTO(Brand brand) {
        return BrandDTO.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .build();
    }
}