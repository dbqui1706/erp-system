package com.erpium.erp.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Organization entity.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsByCode(String code);

    Optional<Organization> findByCode(String code);
}
