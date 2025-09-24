package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.dtos.ProductImageDTO;
import com.manhduc205.ezgear.exceptions.DataNotFoundException;
import com.manhduc205.ezgear.mapper.ProductMapper;
import com.manhduc205.ezgear.models.Brand;
import com.manhduc205.ezgear.models.Category;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductImage;
import com.manhduc205.ezgear.repositories.BrandRepository;
import com.manhduc205.ezgear.repositories.CategoryRepository;
import com.manhduc205.ezgear.repositories.ProductImageRepository;
import com.manhduc205.ezgear.repositories.ProductRepository;
import com.manhduc205.ezgear.services.CategoryService;
import com.manhduc205.ezgear.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final ProductImageRepository productImageRepository;

    @Override
    public Product getProductById(Long id) throws DataNotFoundException {
        return productRepository
                .findById(id)
                .orElseThrow(() -> new DataNotFoundException("Product not found"));
    }

    @Override
    @Transactional
    public Product createProduct(ProductDTO productDTO) throws DataNotFoundException {
        Category existsCategory = categoryRepository
                .findById(productDTO.getId())
                .orElseThrow(() -> new DataNotFoundException("Category not found"));

        Brand existBrand = brandRepository
                .findById(productDTO.getId())
                .orElseThrow(() -> new DataNotFoundException("Brand not found"));
        Product product = productMapper.toProduct(productDTO);
        product.setCategory(existsCategory);
        product.setBrand(existBrand);
        return productRepository.save(product);

    }

    @Override
    @Transactional
    public Product updateProduct( Long id, ProductDTO productDTO) throws DataNotFoundException {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Product not found with id = " + id));

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new DataNotFoundException("Category not found"));

        Brand brand = brandRepository.findById(productDTO.getBrandId())
                .orElseThrow(() -> new DataNotFoundException("Brand not found"));

        // Update fields
        existingProduct.setName(productDTO.getName());
        existingProduct.setSlug(productDTO.getSlug());
        existingProduct.setShortDesc(productDTO.getShortDesc());
        existingProduct.setImageUrl(productDTO.getImageUrl());
        existingProduct.setWarrantyMonths(productDTO.getWarrantyMonths());
        existingProduct.setIsActive(productDTO.getIsActive());
        existingProduct.setCategory(category);
        existingProduct.setBrand(brand);

        return productRepository.save(existingProduct);

    }

    @Override

    public void deleteProduct(Long id) throws Exception {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Product not found with id = " + id));
        productRepository.delete(existingProduct);
    }

    @Override
    public boolean existsProduct(String productName) throws Exception {
        return productRepository.existsByName(productName);
    }

    @Override
    public ProductImage createProductImage(Long productId , ProductImageDTO productImageDTO) throws Exception {
        Product existsProduct = productRepository.findById(productId)
                .orElseThrow(() -> new DataNotFoundException("Product not found with id = " + productId));
        ProductImage productImage = ProductImage
                .builder()
                .product(existsProduct)
                .imageUrl(productImageDTO.getImageUrl())
                .build();

        int size = productImageRepository.findByProductId(productId).size();
        if(size >= ProductImage.MAXIMUM_IMAGES_PER_PRODUCT){
            throw new DataNotFoundException("Number of images lest " +  ProductImage.MAXIMUM_IMAGES_PER_PRODUCT);
        }
        return productImageRepository.save(productImage);
    }

    @Override
    public ProductImage getProductImageById(Long imageId) throws Exception {
        return productImageRepository.findById(imageId)
                .orElseThrow(() -> new DataNotFoundException("Product image not found with id = " + imageId));

    }


}
