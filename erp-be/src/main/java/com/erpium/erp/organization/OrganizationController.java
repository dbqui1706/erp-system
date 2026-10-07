package com.erpium.erp.organization;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST Controller exposing Organization resource endpoints and lifecycle history.
 */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations() {
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getOrganizationById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<OrganizationStatusHistoryResponse>> getStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getStatusHistory(id));
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request
    ) {
        OrganizationResponse created = organizationService.createOrganization(request);
        URI location = URI.create("/api/v1/organizations/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<OrganizationResponse> activateOrganization(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Activated via API") String reason
    ) {
        return ResponseEntity.ok(organizationService.activateOrganization(id, reason));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<OrganizationResponse> suspendOrganization(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Suspended via API") String reason
    ) {
        return ResponseEntity.ok(organizationService.suspendOrganization(id, reason));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<OrganizationResponse> closeOrganization(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Closed via API") String reason
    ) {
        return ResponseEntity.ok(organizationService.closeOrganization(id, reason));
    }
}
