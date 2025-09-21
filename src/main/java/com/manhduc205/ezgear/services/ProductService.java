package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.models.Product;
import org.springframework.stereotype.Service;

@Service
public interface ProductService {
        Product getProductById( Long id) throws Exception;
        Product createProduct(ProductDTO productDTO) throws Exception;
        Product updateProduct(Long id ,ProductDTO productDTO) throws Exception;
        void deleteProduct(Long id) throws Exception;
        boolean existsProduct( String productName) throws Exception;

}
