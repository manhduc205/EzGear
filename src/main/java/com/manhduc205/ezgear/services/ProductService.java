package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.dtos.ProductImageDTO;
import com.manhduc205.ezgear.dtos.responses.product.ProductDetailResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductSiblingResponse;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductImage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProductService {
    Product getProductById( Long id) throws Exception;
    Product createProduct(ProductDTO productDTO) throws Exception;
    Product updateProduct(Long id ,ProductDTO productDTO) throws Exception;
    void deleteProduct(Long id) throws Exception;
    boolean existsProduct( String productName) throws Exception;
    ProductImage createProductImage(Long id, ProductImageDTO productImageDTO) throws Exception;
    ProductImage getProductImageById(Long id) throws Exception;
    List<ProductSiblingResponse> getRelatedProducts(String slug);
    ProductDetailResponse getProductDetail(String slug);
}
