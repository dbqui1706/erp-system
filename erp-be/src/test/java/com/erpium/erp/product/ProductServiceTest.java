package com.erpium.erp.product;

import com.erpium.erp.organization.port.OrganizationValidatorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProductService, Dynamic Price Engine, and OrganizationValidatorPort decoupling.
 * Follows backend-test.md: isolated JUnit 5 + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductPriceTierRepository priceTierRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private OrganizationValidatorPort organizationValidatorPort;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, priceTierRepository, productMapper, organizationValidatorPort);
    }

    @Nested
    @DisplayName("Product Creation & Port Validation")
    class CreationTests {

        @Test
        @DisplayName("Should create product when Organization is valid and SKU is unique")
        void shouldCreateProduct_WhenOrganizationActiveAndSkuUnique() {
            CreateProductRequest request = new CreateProductRequest(
                    1L, "SKU-LAPTOP-01", "ThinkPad X1", new BigDecimal("1200.5000"), "USD", new BigDecimal("0.1000")
            );
            doNothing().when(organizationValidatorPort).validateActiveOrganization(1L);
            when(productRepository.existsBySku("SKU-LAPTOP-01")).thenReturn(false);

            Product savedEntity = new Product(
                    1L, "SKU-LAPTOP-01", "ThinkPad X1", new BigDecimal("1200.5000"), "USD", new BigDecimal("0.1000")
            );
            when(productRepository.save(any(Product.class))).thenReturn(savedEntity);

            ProductResponse expectedResponse = new ProductResponse(
                    10L, 1L, "SKU-LAPTOP-01", "ThinkPad X1", new BigDecimal("1200.5000"), "USD",
                    new BigDecimal("0.1000"), new BigDecimal("1320.55"), ProductStatus.DRAFT, 0L, Instant.now(), Instant.now()
            );
            when(productMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

            ProductResponse response = productService.createProduct(request);

            assertThat(response).isNotNull();
            assertThat(response.sku()).isEqualTo("SKU-LAPTOP-01");

            // Verify Port was called to ensure organization validity
            verify(organizationValidatorPort).validateActiveOrganization(1L);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getSku()).isEqualTo("SKU-LAPTOP-01");
        }

        @Test
        @DisplayName("Should reject product creation when Organization is NOT active (Port rejects)")
        void shouldRejectCreation_WhenOrganizationPortFails() {
            CreateProductRequest request = new CreateProductRequest(
                    99L, "SKU-LAPTOP-01", "ThinkPad X1", new BigDecimal("1200.5000"), "USD", new BigDecimal("0.1000")
            );
            doThrow(new IllegalStateException("Organization with ID 99 is not ACTIVE"))
                    .when(organizationValidatorPort).validateActiveOrganization(99L);

            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Organization with ID 99 is not ACTIVE");

            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Dynamic Tiered Pricing Engine")
    class DynamicPricingEngineTests {

        @Test
        @DisplayName("Should resolve tier price when quantity meets or exceeds minimum volume threshold")
        void shouldResolveTierPrice_WhenQuantityMeetsThreshold() {
            Product product = new Product(1L, "SKU-ITEM", "Enterprise Server", new BigDecimal("2000.0000"), "USD", new BigDecimal("0.1000"));
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));

            // Configured tiers for WHOLESALE:
            // Tier 1: minQty = 50 -> $1600.0000
            // Tier 2: minQty = 10 -> $1800.0000
            // Tier 3: minQty = 1  -> $1950.0000
            ProductPriceTier tier50 = new ProductPriceTier(100L, "WHOLESALE", 50, new BigDecimal("1600.0000"));
            ProductPriceTier tier10 = new ProductPriceTier(100L, "WHOLESALE", 10, new BigDecimal("1800.0000"));
            ProductPriceTier tier1 = new ProductPriceTier(100L, "WHOLESALE", 1, new BigDecimal("1950.0000"));

            when(priceTierRepository.findByProductIdAndPriceListCodeOrderByMinQuantityDesc(100L, "WHOLESALE"))
                    .thenReturn(List.of(tier50, tier10, tier1));

            // Buying 25 units -> matches tier10 ($1800.0000)
            EffectivePriceResponse response = productService.calculateEffectivePrice(100L, "WHOLESALE", 25);

            assertThat(response).isNotNull();
            assertThat(response.unitBasePrice()).isEqualByComparingTo("1800.0000");
            assertThat(response.appliedRule()).contains("minQty=10");

            // Unit Gross Price: $1800 * 1.10 = $1980.00
            assertThat(response.grossUnitPrice()).isEqualByComparingTo("1980.00");

            // Total Gross Amount: $1980.00 * 25 = $49500.00
            assertThat(response.totalGrossAmount()).isEqualByComparingTo("49500.00");
        }

        @Test
        @DisplayName("Should fallback to Catalog MSRP base price when no tier matches")
        void shouldFallbackToCatalogBasePrice_WhenNoTierMatches() {
            Product product = new Product(1L, "SKU-ITEM", "Enterprise Server", new BigDecimal("2000.0000"), "USD", new BigDecimal("0.1000"));
            when(productRepository.findById(100L)).thenReturn(Optional.of(product));

            // Only tier for VIP requires minQty = 10
            ProductPriceTier vipTier = new ProductPriceTier(100L, "VIP", 10, new BigDecimal("1500.0000"));
            when(priceTierRepository.findByProductIdAndPriceListCodeOrderByMinQuantityDesc(100L, "VIP"))
                    .thenReturn(List.of(vipTier));

            // Buying only 3 units (< 10) -> falls back to basePrice ($2000.0000)
            EffectivePriceResponse response = productService.calculateEffectivePrice(100L, "VIP", 3);

            assertThat(response.unitBasePrice()).isEqualByComparingTo("2000.0000");
            assertThat(response.appliedRule()).isEqualTo("FALLBACK_CATALOG_MSRP");
            assertThat(response.grossUnitPrice()).isEqualByComparingTo("2200.00");
            assertThat(response.totalGrossAmount()).isEqualByComparingTo("6600.00");
        }
    }
}
