package com.erpium.erp.order;

import com.erpium.erp.organization.port.OrganizationValidatorPort;
import com.erpium.erp.product.EffectivePriceResponse;
import com.erpium.erp.product.ProductResponse;
import com.erpium.erp.product.ProductService;
import com.erpium.erp.product.ProductStatus;
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
import java.util.Collections;
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
 * Unit tests for SalesOrderService and Order-Level Rounding Engine (DEBT-002 / INV-002).
 * Follows backend-test.md: isolated JUnit 5 + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrganizationValidatorPort organizationValidatorPort;

    @Mock
    private ProductService productService;

    private SalesOrderService salesOrderService;

    @BeforeEach
    void setUp() {
        salesOrderService = new SalesOrderService(salesOrderRepository, orderMapper, organizationValidatorPort, productService);
    }

    @Nested
    @DisplayName("Creation & Order-Level Rounding Tests (DEBT-002)")
    class CreationAndRoundingTests {

        @Test
        @DisplayName("Order-Level Rounding Engine: Eliminates $45.88 fractional discrepancy on bulk 10,000 units")
        void shouldCalculateOrderTotalsAccurately_WithoutLineLevelRoundingDrift() {
            // Setup bulk order of 10,000 units with base price $19.9950 and 8.25% VAT
            CreateOrderLineItemRequest lineItem = new CreateOrderLineItemRequest(100L, 10000, "WHOLESALE");
            CreateSalesOrderRequest request = new CreateSalesOrderRequest(
                    1L, "SO-BULK-001", "MegaCorp Retail", "USD", List.of(lineItem)
            );

            doNothing().when(organizationValidatorPort).validateActiveOrganization(1L);
            when(salesOrderRepository.existsByOrderNumber("SO-BULK-001")).thenReturn(false);

            ProductResponse activeProduct = new ProductResponse(
                    100L, 1L, "SKU-BULK-ITEM", "Industrial Widget", new BigDecimal("19.9950"), "USD",
                    new BigDecimal("0.0825"), new BigDecimal("21.64"), ProductStatus.ACTIVE, 0L, Instant.now(), Instant.now()
            );
            when(productService.getProductById(100L)).thenReturn(activeProduct);

            EffectivePriceResponse pricing = new EffectivePriceResponse(
                    100L, "SKU-BULK-ITEM", "WHOLESALE", 10000, new BigDecimal("19.9950"),
                    new BigDecimal("21.64"), new BigDecimal("216400.00"), "USD", "TIER_MATCH"
            );
            when(productService.calculateEffectivePrice(100L, "WHOLESALE", 10000)).thenReturn(pricing);

            when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

            SalesOrderResponse dummyResponse = new SalesOrderResponse(
                    1L, 1L, "SO-BULK-001", "MegaCorp Retail", "USD", OrderStatus.DRAFT,
                    new BigDecimal("199950.0000"), new BigDecimal("16495.8750"), new BigDecimal("216445.88"),
                    Collections.emptyList(), 0L, Instant.now(), Instant.now()
            );
            when(orderMapper.toResponse(any(SalesOrder.class))).thenReturn(dummyResponse);

            salesOrderService.createSalesOrder(request);

            // Capture the saved entity and verify exact Order-Level financial mathematics
            ArgumentCaptor<SalesOrder> orderCaptor = ArgumentCaptor.forClass(SalesOrder.class);
            verify(salesOrderRepository).save(orderCaptor.capture());
            SalesOrder savedOrder = orderCaptor.getValue();

            // Mathematical verification:
            // Unrounded subtotal = 10,000 * 19.9950 = $199,950.0000
            assertThat(savedOrder.getSubtotalAmount()).isEqualByComparingTo("199950.0000");

            // Unrounded tax = 199,950.0000 * 0.0825 = $16,495.8750
            assertThat(savedOrder.getTaxAmount()).isEqualByComparingTo("16495.8750");

            // Order-Level Rounded Total: (199,950.0000 + 16,495.8750) rounded HALF_UP to scale 2 = $216,445.88!
            // Notice: Line-level rounded would have produced $21.64 * 10,000 = $216,400.00 ($45.88 discrepancy!).
            assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("216445.88");
        }

        @Test
        @DisplayName("Should reject order creation when Organization is NOT active")
        void shouldRejectOrder_WhenOrganizationNotActive() {
            CreateOrderLineItemRequest lineItem = new CreateOrderLineItemRequest(100L, 5, "STANDARD");
            CreateSalesOrderRequest request = new CreateSalesOrderRequest(
                    99L, "SO-ERR-001", "Customer", "USD", List.of(lineItem)
            );
            doThrow(new IllegalStateException("Organization is not ACTIVE"))
                    .when(organizationValidatorPort).validateActiveOrganization(99L);

            assertThatThrownBy(() -> salesOrderService.createSalesOrder(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Organization is not ACTIVE");

            verify(salesOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject order creation when product is in DRAFT status")
        void shouldRejectOrder_WhenProductNotActive() {
            CreateOrderLineItemRequest lineItem = new CreateOrderLineItemRequest(200L, 2, "STANDARD");
            CreateSalesOrderRequest request = new CreateSalesOrderRequest(
                    1L, "SO-ERR-002", "Customer", "USD", List.of(lineItem)
            );
            doNothing().when(organizationValidatorPort).validateActiveOrganization(1L);
            when(salesOrderRepository.existsByOrderNumber("SO-ERR-002")).thenReturn(false);

            ProductResponse draftProduct = new ProductResponse(
                    200L, 1L, "SKU-DRAFT", "Draft Product", new BigDecimal("50.0000"), "USD",
                    new BigDecimal("0.1000"), new BigDecimal("55.00"), ProductStatus.DRAFT, 0L, Instant.now(), Instant.now()
            );
            when(productService.getProductById(200L)).thenReturn(draftProduct);

            assertThatThrownBy(() -> salesOrderService.createSalesOrder(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot order product that is not ACTIVE");

            verify(salesOrderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Lifecycle State Machine Transitions")
    class LifecycleTests {

        @Test
        @DisplayName("Happy path: Transition DRAFT -> CONFIRMED -> FULFILLED")
        void shouldTransitionOrderLifecycle() {
            SalesOrder order = new SalesOrder(1L, "SO-TRANS-01", "Client", "USD");
            order.addLine(10L, "SKU-01", "Item", 1, new BigDecimal("100.0000"), BigDecimal.ZERO);
            when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(order));

            when(orderMapper.toResponse(any(SalesOrder.class))).thenReturn(null);

            // DRAFT -> CONFIRMED
            salesOrderService.confirmOrder(1L);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

            // CONFIRMED -> FULFILLED
            salesOrderService.fulfillOrder(1L);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FULFILLED);
        }

        @Test
        @DisplayName("Terminal invariant: cannot cancel a FULFILLED order")
        void shouldRejectCancelling_WhenFulfilled() {
            SalesOrder order = new SalesOrder(1L, "SO-TERM-01", "Client", "USD");
            order.addLine(10L, "SKU-01", "Item", 1, new BigDecimal("100.0000"), BigDecimal.ZERO);
            order.confirm();
            order.fulfill(); // FULFILLED

            when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> salesOrderService.cancelOrder(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel an order that has already been FULFILLED");
        }
    }
}
