package com.erpium.erp.product;

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
import java.util.List;

/**
 * REST Controller exposing Product catalog and Dynamic Pricing endpoints.
 *
 * Architecture Compliance:
 * - backend-api.md: Thin controller adapter; Bean validation via @Valid; server-side page size capping.
 * - common.md: Standardized /api/v1 routing; 201 Created with Location header.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final int MAX_PAGE_SIZE = 100;
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Retrieves a single product by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Retrieves paginated products for an organization with server-enforced max page size.
     */
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam Long organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        int cappedSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(productService.getProductsByOrganization(organizationId, pageable));
    }

    /**
     * Creates a new Product in DRAFT status.
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        URI location = URI.create("/api/v1/products/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Activates a product for commercial operations.
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.activateProduct(id));
    }

    /**
     * Discontinues a product permanently.
     */
    @PostMapping("/{id}/discontinue")
    public ResponseEntity<ProductResponse> discontinueProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.discontinueProduct(id));
    }

    // =========================================================================
    // DYNAMIC PRICE ENGINE ENDPOINTS
    // =========================================================================

    /**
     * Configures a price tier for a product.
     */
    @PostMapping("/{id}/price-tiers")
    public ResponseEntity<PriceTierResponse> addPriceTier(
            @PathVariable Long id,
            @Valid @RequestBody AddPriceTierRequest request
    ) {
        PriceTierResponse response = productService.addPriceTier(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all price tiers configured for a product.
     */
    @GetMapping("/{id}/price-tiers")
    public ResponseEntity<List<PriceTierResponse>> getPriceTiers(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getPriceTiers(id));
    }

    /**
     * Resolves effective unit price and total gross amount dynamically based on quantity and price list.
     */
    @GetMapping("/{id}/effective-price")
    public ResponseEntity<EffectivePriceResponse> getEffectivePrice(
            @PathVariable Long id,
            @RequestParam(defaultValue = "STANDARD") String priceListCode,
            @RequestParam(defaultValue = "1") int quantity
    ) {
        return ResponseEntity.ok(productService.calculateEffectivePrice(id, priceListCode, quantity));
    }
}
