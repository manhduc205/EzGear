package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.ProductSkuDTO;
import com.manhduc205.ezgear.dtos.request.AdminProductSkuSearchRequest;
import com.manhduc205.ezgear.dtos.request.ProductSkuSearchRequest;
import com.manhduc205.ezgear.dtos.responses.product.AdminProductSkuResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductThumbnailResponse;
import com.manhduc205.ezgear.models.ProductSKU;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;



@Service
public interface ProductSkuService {
    ProductSkuDTO createProductSku(ProductSkuDTO productSKUDTO);
    ProductSkuDTO updateProductSku(Long id,ProductSkuDTO productSKUDTO);
    void deleteProductSku(Long id);
    Page<ProductThumbnailResponse> searchProductSkus(ProductSkuSearchRequest request);
    Page<AdminProductSkuResponse> searchProductSkusForAdmin(AdminProductSkuSearchRequest request);
    ProductSKU getById(Long id);
    ProductSkuDTO getSkuDetail(Long id);
}
