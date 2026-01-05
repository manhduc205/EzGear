package com.manhduc205.ezgear.mapper;

import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.dtos.responses.product.AdminProductDetailResponse;
import com.manhduc205.ezgear.models.Product;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProductMapper {

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "productSkus", ignore = true)
    @Mapping(target = "productImages", ignore = true)
    Product toProduct(ProductDTO productDTO);
    ProductDTO toDTO(Product product);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "brand.id", target = "brandId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "brand.name", target = "brandName")
    AdminProductDetailResponse toDetailResponse(Product product);
}
