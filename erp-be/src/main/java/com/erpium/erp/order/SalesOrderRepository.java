package com.erpium.erp.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for SalesOrder aggregate roots.
 */
@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    boolean existsByOrderNumber(String orderNumber);

    Optional<SalesOrder> findByOrderNumber(String orderNumber);

    Page<SalesOrder> findByOrganizationId(Long organizationId, Pageable pageable);
}
