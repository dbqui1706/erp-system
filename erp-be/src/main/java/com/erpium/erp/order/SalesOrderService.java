package com.erpium.erp.order;

import com.erpium.erp.organization.port.OrganizationValidatorPort;
import com.erpium.erp.product.EffectivePriceResponse;
import com.erpium.erp.product.ProductResponse;
import com.erpium.erp.product.ProductService;
import com.erpium.erp.product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Sales Order commercial flows, order-level financial calculation,
 * and lifecycle transitions.
 *
 * Architecture Compliance:
 * - backend.md: Modular Monolith boundaries. Communicates with Organization via Port,
 *   and with Product via public ProductService & DTOs (no raw entity dependencies).
 * - INV-002: Implements Order-Level Rounding Engine across multi-item lines.
 */
@Service
@Transactional(readOnly = true)
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final OrderMapper orderMapper;
    private final OrganizationValidatorPort organizationValidatorPort;
    private final ProductService productService;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             OrderMapper orderMapper,
                             OrganizationValidatorPort organizationValidatorPort,
                             ProductService productService) {
        this.salesOrderRepository = salesOrderRepository;
        this.orderMapper = orderMapper;
        this.organizationValidatorPort = organizationValidatorPort;
        this.productService = productService;
    }

    /**
     * Creates a new Sales Order in DRAFT status.
     * Evaluates active tenant status via Port and resolves dynamic tiered pricing for each line item.
     */
    @Transactional
    public SalesOrderResponse createSalesOrder(CreateSalesOrderRequest request) {
        // Inter-module verification: ensure target Organization exists and is ACTIVE
        organizationValidatorPort.validateActiveOrganization(request.organizationId());

        String normalizedOrderNumber = request.orderNumber().trim().toUpperCase();
        if (salesOrderRepository.existsByOrderNumber(normalizedOrderNumber)) {
            throw new IllegalStateException("Sales Order with number already exists: " + normalizedOrderNumber);
        }

        SalesOrder order = new SalesOrder(
                request.organizationId(),
                normalizedOrderNumber,
                request.customerName().trim(),
                request.currency().trim().toUpperCase()
        );

        // Process line items via public ProductService API
        for (CreateOrderLineItemRequest lineRequest : request.lines()) {
            ProductResponse product = productService.getProductById(lineRequest.productId());

            if (product.status() != ProductStatus.ACTIVE) {
                throw new IllegalStateException("Cannot order product that is not ACTIVE. SKU: " + product.sku() + ", Status: " + product.status());
            }

            // Resolve dynamic unit price based on volume threshold and price list
            EffectivePriceResponse pricing = productService.calculateEffectivePrice(
                    lineRequest.productId(),
                    lineRequest.priceListCode(),
                    lineRequest.quantity()
            );

            // Add line and recalculate totals with Order-Level Rounding (INV-002)
            order.addLine(
                    product.id(),
                    product.sku(),
                    product.name(),
                    lineRequest.quantity(),
                    pricing.unitBasePrice(),
                    product.taxRate()
            );
        }

        SalesOrder saved = salesOrderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    /**
     * Retrieves a single sales order by ID.
     */
    public SalesOrderResponse getOrderById(Long id) {
        SalesOrder order = findEntityById(id);
        return orderMapper.toResponse(order);
    }

    /**
     * Retrieves paginated sales orders belonging to an organization.
     */
    public Page<SalesOrderResponse> getOrdersByOrganization(Long organizationId, Pageable pageable) {
        return salesOrderRepository.findByOrganizationId(organizationId, pageable)
                .map(orderMapper::toResponse);
    }

    /**
     * Confirms a draft order for fulfillment.
     */
    @Transactional
    public SalesOrderResponse confirmOrder(Long id) {
        SalesOrder order = findEntityById(id);
        order.confirm();
        return orderMapper.toResponse(order);
    }

    /**
     * Marks a confirmed order as fulfilled.
     */
    @Transactional
    public SalesOrderResponse fulfillOrder(Long id) {
        SalesOrder order = findEntityById(id);
        order.fulfill();
        return orderMapper.toResponse(order);
    }

    /**
     * Cancels an order.
     */
    @Transactional
    public SalesOrderResponse cancelOrder(Long id) {
        SalesOrder order = findEntityById(id);
        order.cancel();
        return orderMapper.toResponse(order);
    }

    private SalesOrder findEntityById(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sales order not found with id: " + id));
    }
}
