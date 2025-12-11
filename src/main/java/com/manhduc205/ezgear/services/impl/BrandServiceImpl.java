package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.BrandDTO;
import com.manhduc205.ezgear.models.Brand;
import com.manhduc205.ezgear.repositories.BrandRepository;
import com.manhduc205.ezgear.services.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandRepository brandRepository;

    @Override
    @Transactional
    public Brand createBrand(BrandDTO brandDTO) {
        Brand newBrand = Brand.builder()
                .name(brandDTO.getName())
                .build();
        return brandRepository.save(newBrand);
    }

    @Override
    public List<BrandDTO> getAllBrands() {
        return brandRepository.findAll()
                .stream()
                .map(brand -> BrandDTO.builder()
                        .id(brand.getId())
                        .name(brand.getName())
                        .slug(brand.getSlug())
                        .build())
                .toList();
    }

    @Override
    public Brand getBrandById(Long categoryId) {
        return brandRepository.findById(categoryId).
                orElseThrow(() -> new RuntimeException(Translator.toLocale("error.brand.not_found")));
    }



    @Override
    public Brand updateBrand(Long brandId, BrandDTO brandDTO) {
        Brand oldBrand =  getBrandById(brandId);
        oldBrand.setName(brandDTO.getName());
        return brandRepository.save(oldBrand);
    }

    @Override
    public void deleteBrandById(Long brandId) {
        if (!brandRepository.existsById(brandId)) {
            throw new RuntimeException(Translator.toLocale("error.brand.not_found"));
        }
        brandRepository.deleteById(brandId);
    }
}
