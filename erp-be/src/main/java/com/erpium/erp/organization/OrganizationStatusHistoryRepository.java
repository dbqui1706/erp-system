package com.erpium.erp.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for OrganizationStatusHistory audit entries.
 */
@Repository
public interface OrganizationStatusHistoryRepository extends JpaRepository<OrganizationStatusHistory, Long> {

    List<OrganizationStatusHistory> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
}
