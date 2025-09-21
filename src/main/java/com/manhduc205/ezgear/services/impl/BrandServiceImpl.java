package com.manhduc205.ezgear.services.impl;

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
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Override
    public Brand getBrandById(Long categoryId) {
        return brandRepository.findById(categoryId).
                orElseThrow(() -> new RuntimeException());
    }



    @Override
    public Brand updateBrand(Long brandId, BrandDTO brandDTO) {
        Brand oldBrand =  getBrandById(brandId);
        oldBrand.setName(brandDTO.getName());
        return brandRepository.save(oldBrand);
    }

    @Override
    public void deleteBrandById(Long brandId) {
        brandRepository.deleteById(brandId);
    }
}
