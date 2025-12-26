package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.dtos.ProductImageDTO;
import com.manhduc205.ezgear.dtos.request.AdminProductSearchRequest;
import com.manhduc205.ezgear.dtos.responses.product.AdminProductResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductDetailResponse;
import com.manhduc205.ezgear.dtos.responses.product.ProductSiblingResponse;
import com.manhduc205.ezgear.models.Product;
import com.manhduc205.ezgear.models.ProductImage;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public interface ProductService {
    Product getProductById( Long id) throws Exception;
    Product createProduct(ProductDTO productDTO, List<MultipartFile> files) throws Exception;
    Product updateProduct(Long id ,ProductDTO productDTO, MultipartFile imageFile) throws IOException;
    void deleteProduct(Long id) throws Exception;
    List<ProductImage> uploadImages(Long productId, List<MultipartFile> files) throws Exception;
    boolean existsProduct( String productName) throws Exception;
    ProductImage createProductImage(Long id, ProductImageDTO productImageDTO);
    List<ProductImage> getImagesByProductId(Long productId);
    void deleteProductImage(Long imageId) throws Exception;
    ProductImage getProductImageById(Long id) throws Exception;
    List<ProductSiblingResponse> getRelatedProducts(String slug);
    ProductDetailResponse getProductDetail(String slug);
    Page<AdminProductResponse> searchProductsForAdmin(AdminProductSearchRequest request);
}
