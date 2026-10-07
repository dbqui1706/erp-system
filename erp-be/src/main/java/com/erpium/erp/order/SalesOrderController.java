package com.erpium.erp.order;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST Controller exposing Sales Order endpoints.
 *
 * Architecture Compliance:
 * - backend-api.md: Thin controller adapter; Bean validation via @Valid; server-side page size capping.
 * - common.md: Standardized /api/v1 routing; 201 Created with Location header.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class SalesOrderController {

    private static final int MAX_PAGE_SIZE = 100;
    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.getOrderById(id));
    }

    @GetMapping
    public ResponseEntity<Page<SalesOrderResponse>> getOrders(
            @RequestParam Long organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int cappedSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(salesOrderService.getOrdersByOrganization(organizationId, pageable));
    }

    @PostMapping
    public ResponseEntity<SalesOrderResponse> createSalesOrder(@Valid @RequestBody CreateSalesOrderRequest request) {
        SalesOrderResponse created = salesOrderService.createSalesOrder(request);
        URI location = URI.create("/api/v1/orders/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<SalesOrderResponse> confirmOrder(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.confirmOrder(id));
    }

    @PostMapping("/{id}/fulfill")
    public ResponseEntity<SalesOrderResponse> fulfillOrder(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.fulfillOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SalesOrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.cancelOrder(id));
    }
}
