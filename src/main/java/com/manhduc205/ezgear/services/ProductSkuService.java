package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.ProductSkuDTO;
import com.manhduc205.ezgear.dtos.request.ProductSkuSearchRequest;
import com.manhduc205.ezgear.models.ProductSKU;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;



@Service
public interface ProductSkuService {
    ProductSKU createProductSku(ProductSkuDTO productSKUDTO);
    ProductSKU updateProductSku(Long id,ProductSkuDTO productSKUDTO);
    void deleteProductSku(Long id);
    Page<ProductSKU> searchProductSkus(ProductSkuSearchRequest request);
}
