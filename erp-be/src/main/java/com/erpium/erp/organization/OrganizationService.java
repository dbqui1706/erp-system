package com.erpium.erp.organization;

import com.erpium.erp.organization.port.OrganizationValidatorPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing Organization business operations, lifecycle state machine,
 * and immutable audit history logging.
 *
 * Implements OrganizationValidatorPort for inter-module decoupling.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationService implements OrganizationValidatorPort {

    private final OrganizationRepository organizationRepository;
    private final OrganizationStatusHistoryRepository statusHistoryRepository;
    private final OrganizationMapper organizationMapper;

    public OrganizationService(OrganizationRepository organizationRepository,
                               OrganizationStatusHistoryRepository statusHistoryRepository,
                               OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.organizationMapper = organizationMapper;
    }

    public List<OrganizationResponse> getAllOrganizations() {
        return organizationMapper.toResponseList(organizationRepository.findAll());
    }

    public OrganizationResponse getOrganizationById(Long id) {
        Organization entity = findEntityById(id);
        return organizationMapper.toResponse(entity);
    }

    public List<OrganizationStatusHistoryResponse> getStatusHistory(Long organizationId) {
        findEntityById(organizationId); // Ensure organization exists
        return statusHistoryRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(OrganizationStatusHistoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        if (organizationRepository.existsByCode(normalizedCode)) {
            throw new IllegalStateException("Organization with code already exists: " + normalizedCode);
        }

        Organization entity = new Organization(
                request.name().trim(),
                normalizedCode,
                request.taxCode() != null ? request.taxCode().trim() : null
        );

        Organization saved = organizationRepository.save(entity);

        // Record initial state audit log
        statusHistoryRepository.save(new OrganizationStatusHistory(
                saved.getId(), OrganizationStatus.DRAFT, OrganizationStatus.DRAFT, "Initial organization creation"
        ));

        return organizationMapper.toResponse(saved);
    }

    @Transactional
    public OrganizationResponse activateOrganization(Long id, String reason) {
        Organization entity = findEntityById(id);
        OrganizationStatus fromStatus = entity.getStatus();
        entity.activate();

        statusHistoryRepository.save(new OrganizationStatusHistory(
                entity.getId(), fromStatus, entity.getStatus(), reason != null ? reason : "Activated by administrative action"
        ));

        return organizationMapper.toResponse(entity);
    }

    @Transactional
    public OrganizationResponse suspendOrganization(Long id, String reason) {
        Organization entity = findEntityById(id);
        OrganizationStatus fromStatus = entity.getStatus();
        entity.suspend();

        statusHistoryRepository.save(new OrganizationStatusHistory(
                entity.getId(), fromStatus, entity.getStatus(), reason != null ? reason : "Suspended by administrative action"
        ));

        return organizationMapper.toResponse(entity);
    }

    @Transactional
    public OrganizationResponse closeOrganization(Long id, String reason) {
        Organization entity = findEntityById(id);
        OrganizationStatus fromStatus = entity.getStatus();
        entity.close();

        statusHistoryRepository.save(new OrganizationStatusHistory(
                entity.getId(), fromStatus, entity.getStatus(), reason != null ? reason : "Permanently closed"
        ));

        return organizationMapper.toResponse(entity);
    }

    // =========================================================================
    // IMPLEMENTATION OF OrganizationValidatorPort (Inter-Module Boundary)
    // =========================================================================

    @Override
    public void validateActiveOrganization(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization does not exist with ID: " + organizationId));

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Organization with ID " + organizationId + " is not ACTIVE (current status: " + organization.getStatus() + ")"
            );
        }
    }

    private Organization findEntityById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with id: " + id));
    }
}
