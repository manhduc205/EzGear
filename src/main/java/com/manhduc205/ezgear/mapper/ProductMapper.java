package com.manhduc205.ezgear.mapper;

import com.manhduc205.ezgear.dtos.ProductDTO;
import com.manhduc205.ezgear.models.Product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ProductMapper {

    Product toProduct(ProductDTO productDTO);
    ProductDTO toDTO(Product product);
}

