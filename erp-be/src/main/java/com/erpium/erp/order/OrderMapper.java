package com.erpium.erp.order;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for SalesOrder and SalesOrderLine DTO transformations.
 * Generates compile-time mapping into Java 21 records.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    SalesOrderResponse toResponse(SalesOrder order);

    SalesOrderLineResponse toLineResponse(SalesOrderLine line);

    List<SalesOrderResponse> toResponseList(List<SalesOrder> orders);
}
