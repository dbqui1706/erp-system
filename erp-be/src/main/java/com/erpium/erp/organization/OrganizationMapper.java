package com.erpium.erp.organization;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for Organization entity and DTO transformations.
 * Generates compile-time, zero-reflection mapping code for Java 21 records.
 *
 * Architecture Compliance:
 * - backend.md: DTO mapping encapsulated at Service boundary; never leak entities.
 * - MapStruct: Uses Spring component model for seamless constructor injection.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrganizationMapper {

    /**
     * Maps an Organization JPA entity to an immutable OrganizationResponse record.
     *
     * @param entity the managed or detached Organization entity
     * @return the mapped OrganizationResponse record DTO
     */
    OrganizationResponse toResponse(Organization entity);

    /**
     * Maps a collection of Organization entities to a list of OrganizationResponse records.
     *
     * @param entities the list of Organization entities
     * @return the list of mapped OrganizationResponse records
     */
    List<OrganizationResponse> toResponseList(List<Organization> entities);
}
