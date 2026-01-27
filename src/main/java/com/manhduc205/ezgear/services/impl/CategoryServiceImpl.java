package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.CategoryDTO;
import com.manhduc205.ezgear.models.Brand;
import com.manhduc205.ezgear.models.Category;
import com.manhduc205.ezgear.repositories.BrandRepository;
import com.manhduc205.ezgear.repositories.CategoryRepository;
import com.manhduc205.ezgear.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Override
    @Transactional
    public Category createCategory(CategoryDTO categoryDTO) {
        Category newCategory = Category.builder()
                .name(categoryDTO.getName())
                .slug(categoryDTO.getSlug())
                .parentId(categoryDTO.getParentId())
                .isActive(true)
                .build();

        if (categoryDTO.getBrandIds() != null && !categoryDTO.getBrandIds().isEmpty()) {
            List<Brand> brands = brandRepository.findAllById(categoryDTO.getBrandIds());
            newCategory.setBrands(new HashSet<>(brands));
        }

        return categoryRepository.save(newCategory);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException(Translator.toLocale("error.category.not_found")));
    }

    @Override
    @Transactional
    public Category updateCategory(Long categoryId, CategoryDTO categoryDTO) {
        Category oldCategory = getCategoryById(categoryId);

        // Update thông tin cơ bản
        oldCategory.setName(categoryDTO.getName());
        if(categoryDTO.getSlug() != null) oldCategory.setSlug(categoryDTO.getSlug());
        if(categoryDTO.getParentId() != null) oldCategory.setParentId(categoryDTO.getParentId());

        if (categoryDTO.getBrandIds() != null) {
            List<Brand> brands = brandRepository.findAllById(categoryDTO.getBrandIds());
            oldCategory.setBrands(new HashSet<>(brands));
        }

        return categoryRepository.save(oldCategory);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new RuntimeException(Translator.toLocale("error.category.not_found"));
        }
        categoryRepository.deleteById(categoryId);
    }
}