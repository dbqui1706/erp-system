package com.erpium.erp.product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for Product entity and DTO transformations.
 * Generates compile-time, zero-reflection mapping for Java 21 records.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {

    /**
     * Maps Product entity to ProductResponse record, evaluating calculateGrossPrice() dynamically.
     */
    @Mapping(target = "grossPrice", expression = "java(entity.calculateGrossPrice())")
    ProductResponse toResponse(Product entity);

    /**
     * Maps a list of Product entities to ProductResponse records.
     */
    List<ProductResponse> toResponseList(List<Product> entities);
}
