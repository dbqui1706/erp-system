package com.erpium.erp.organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrganizationService, State Machine transitions, and Audit History logging.
 * Follows backend-test.md: isolated JUnit 5 + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationStatusHistoryRepository statusHistoryRepository;

    @Mock
    private OrganizationMapper organizationMapper;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(organizationRepository, statusHistoryRepository, organizationMapper);
    }

    @Nested
    @DisplayName("Creation & Audit Logging Tests")
    class CreationAndRetrievalTests {

        @Test
        @DisplayName("Should create organization in DRAFT status and log initial audit entry")
        void shouldCreateOrganization_InDraftStatus() {
            CreateOrganizationRequest request = new CreateOrganizationRequest("Acme Corp", "ACME", "TAX-12345");
            when(organizationRepository.existsByCode("ACME")).thenReturn(false);

            Organization savedEntity = new Organization(1L, "Acme Corp", "ACME", "TAX-12345");
            when(organizationRepository.save(any(Organization.class))).thenReturn(savedEntity);

            OrganizationResponse expectedResponse = new OrganizationResponse(
                    1L, "Acme Corp", "ACME", "TAX-12345", OrganizationStatus.DRAFT, 0L, Instant.now(), Instant.now()
            );
            when(organizationMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

            OrganizationResponse actualResponse = organizationService.createOrganization(request);

            assertThat(actualResponse).isNotNull();
            assertThat(actualResponse.status()).isEqualTo(OrganizationStatus.DRAFT);

            // Verify status history recorded
            ArgumentCaptor<OrganizationStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrganizationStatusHistory.class);
            verify(statusHistoryRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(OrganizationStatus.DRAFT);
            assertThat(historyCaptor.getValue().getReason()).contains("Initial organization creation");
        }

        @Test
        @DisplayName("Should reject creation when organization code is duplicated")
        void shouldRejectCreation_WhenCodeDuplicated() {
            CreateOrganizationRequest request = new CreateOrganizationRequest("Acme Corp", "ACME", "TAX-12345");
            when(organizationRepository.existsByCode("ACME")).thenReturn(true);

            assertThatThrownBy(() -> organizationService.createOrganization(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Organization with code already exists: ACME");

            verify(organizationRepository, never()).save(any());
            verify(statusHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("State Machine & Audit Trail Tests")
    class StateMachineTests {

        @Test
        @DisplayName("Service layer: should activate organization and record audit log")
        void shouldActivateOrganization_AndRecordAuditLog() {
            Organization entity = new Organization(1L, "HQ", "HQ01", "TAX-001");
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(entity));

            OrganizationResponse response = new OrganizationResponse(
                    1L, "HQ", "HQ01", "TAX-001", OrganizationStatus.ACTIVE, 1L, Instant.now(), Instant.now()
            );
            when(organizationMapper.toResponse(entity)).thenReturn(response);

            OrganizationResponse result = organizationService.activateOrganization(1L, "Board Approval");

            assertThat(result.status()).isEqualTo(OrganizationStatus.ACTIVE);
            assertThat(entity.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);

            ArgumentCaptor<OrganizationStatusHistory> captor = ArgumentCaptor.forClass(OrganizationStatusHistory.class);
            verify(statusHistoryRepository).save(captor.capture());
            assertThat(captor.getValue().getFromStatus()).isEqualTo(OrganizationStatus.DRAFT);
            assertThat(captor.getValue().getToStatus()).isEqualTo(OrganizationStatus.ACTIVE);
            assertThat(captor.getValue().getReason()).isEqualTo("Board Approval");
        }

        @Test
        @DisplayName("Illegal transition: cannot suspend a DRAFT organization directly")
        void shouldRejectSuspending_WhenInDraftStatus() {
            Organization entity = new Organization("HQ", "HQ01", "TAX-001");
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> organizationService.suspendOrganization(1L, "Non-payment"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only ACTIVE organizations can be suspended");

            verify(statusHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("OrganizationValidatorPort Validation Tests")
    class PortValidationTests {

        @Test
        @DisplayName("validateActiveOrganization() should succeed when organization is ACTIVE")
        void shouldPassValidation_WhenOrganizationIsActive() {
            Organization entity = new Organization("Active Corp", "ACT01", "TAX-ACT");
            entity.activate(); // Now ACTIVE
            when(organizationRepository.findById(10L)).thenReturn(Optional.of(entity));

            // Should not throw any exception
            organizationService.validateActiveOrganization(10L);
        }

        @Test
        @DisplayName("validateActiveOrganization() should throw IllegalStateException when organization is DRAFT")
        void shouldThrowException_WhenOrganizationIsNotActive() {
            Organization entity = new Organization("Draft Corp", "DFT01", "TAX-DFT"); // DRAFT
            when(organizationRepository.findById(20L)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> organizationService.validateActiveOrganization(20L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is not ACTIVE");
        }

        @Test
        @DisplayName("validateActiveOrganization() should throw IllegalArgumentException when organization does not exist")
        void shouldThrowException_WhenOrganizationNotFound() {
            when(organizationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> organizationService.validateActiveOrganization(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Organization does not exist with ID: 999");
        }
    }
}
