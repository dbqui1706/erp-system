package com.erpium.erp.organization.port;

/**
 * Public Port defining the contract for validating Organization state across module boundaries.
 *
 * Architecture Compliance:
 * - Clean Architecture / Hexagonal Ports: External modules depend exclusively on this interface,
 *   never on Organization JPA entities or internal persistence repositories.
 */
public interface OrganizationValidatorPort {

    /**
     * Validates that the organization exists and is in ACTIVE operational status.
     *
     * @param organizationId the organization identifier
     * @throws IllegalArgumentException if the organization does not exist
     * @throws IllegalStateException    if the organization exists but is not ACTIVE (e.g. DRAFT, SUSPENDED, CLOSED)
     */
    void validateActiveOrganization(Long organizationId);
}
