package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dto.BrandDTO;
import com.manhduc205.ezgear.models.Brand;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BrandService {
    Brand createBrand(BrandDTO brandDTO);
    List<Brand> getAllBrands();
    Brand updateBrand(Long brandId,BrandDTO brandDTO);
    Brand getBrandById(Long brandId);
    void deleteBrandById(Long brandId);
}
