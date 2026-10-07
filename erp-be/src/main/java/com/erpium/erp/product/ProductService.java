package com.erpium.erp.product;

import com.erpium.erp.organization.port.OrganizationValidatorPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Service managing Product catalog operations, pricing validation, dynamic tier resolution,
 * and inter-module boundary enforcement via OrganizationValidatorPort.
 *
 * Architecture Compliance:
 * - backend.md: Inter-module loose coupling; constructor injection; transactional boundaries.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPriceTierRepository priceTierRepository;
    private final ProductMapper productMapper;
    private final OrganizationValidatorPort organizationValidatorPort;

    /**
     * Constructor injection for all dependencies including the cross-module validation port.
     */
    public ProductService(ProductRepository productRepository,
                          ProductPriceTierRepository priceTierRepository,
                          ProductMapper productMapper,
                          OrganizationValidatorPort organizationValidatorPort) {
        this.productRepository = productRepository;
        this.priceTierRepository = priceTierRepository;
        this.productMapper = productMapper;
        this.organizationValidatorPort = organizationValidatorPort;
    }

    /**
     * Creates a new Product in DRAFT status.
     * Enforces inter-module boundary validation: rejects product creation if Organization
     * does not exist or is not in ACTIVE operational status.
     *
     * @param request the validated creation payload
     * @return the created product response DTO
     * @throws IllegalStateException if SKU is duplicate or Organization is not active
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        // Inter-module verification via Port (eliminates dangling orphaned records)
        organizationValidatorPort.validateActiveOrganization(request.organizationId());

        String normalizedSku = request.sku().trim().toUpperCase();
        if (productRepository.existsBySku(normalizedSku)) {
            throw new IllegalStateException("Product with SKU already exists: " + normalizedSku);
        }

        Product product = new Product(
                request.organizationId(),
                normalizedSku,
                request.name().trim(),
                request.basePrice(),
                request.currency().trim().toUpperCase(),
                request.taxRate()
        );

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    /**
     * Retrieves a single product by ID.
     */
    public ProductResponse getProductById(Long id) {
        Product product = findEntityById(id);
        return productMapper.toResponse(product);
    }

    /**
     * Retrieves paginated products belonging to a specific organization.
     */
    public Page<ProductResponse> getProductsByOrganization(Long organizationId, Pageable pageable) {
        return productRepository.findByOrganizationId(organizationId, pageable)
                .map(productMapper::toResponse);
    }

    /**
     * Activates a product for commercial operations.
     */
    @Transactional
    public ProductResponse activateProduct(Long id) {
        Product product = findEntityById(id);
        product.activate();
        return productMapper.toResponse(product);
    }

    /**
     * Discontinues a product permanently.
     */
    @Transactional
    public ProductResponse discontinueProduct(Long id) {
        Product product = findEntityById(id);
        product.discontinue();
        return productMapper.toResponse(product);
    }

    // =========================================================================
    // DYNAMIC TIERED PRICING ENGINE (Solves Static Pricing Illusion)
    // =========================================================================

    /**
     * Configures a price tier for a product.
     */
    @Transactional
    public PriceTierResponse addPriceTier(Long productId, AddPriceTierRequest request) {
        findEntityById(productId); // Ensure product exists

        ProductPriceTier tier = new ProductPriceTier(
                productId,
                request.priceListCode(),
                request.minQuantity(),
                request.tierPrice()
        );

        ProductPriceTier saved = priceTierRepository.save(tier);
        return PriceTierResponse.fromEntity(saved);
    }

    /**
     * Retrieves all configured price tiers for a product.
     */
    public List<PriceTierResponse> getPriceTiers(Long productId) {
        findEntityById(productId);
        return priceTierRepository.findByProductIdOrderByPriceListCodeAscMinQuantityAsc(productId)
                .stream()
                .map(PriceTierResponse::fromEntity)
                .toList();
    }

    /**
     * Dynamic Price Engine: Resolves the effective unit price and calculates line gross total.
     * Evaluates volume thresholds against the requested price list; falls back to Catalog MSRP if no tier matches.
     *
     * @param productId     the product ID
     * @param priceListCode the target price list (e.g. WHOLESALE, RETAIL)
     * @param quantity      the ordered quantity
     * @return the calculated effective pricing breakdown
     */
    public EffectivePriceResponse calculateEffectivePrice(Long productId, String priceListCode, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1 (got " + quantity + ")");
        }

        Product product = findEntityById(productId);
        String targetPriceList = (priceListCode == null || priceListCode.isBlank()) ? "STANDARD" : priceListCode.trim().toUpperCase();

        // Find candidate tiers sorted by minQuantity DESC
        List<ProductPriceTier> tiers = priceTierRepository.findByProductIdAndPriceListCodeOrderByMinQuantityDesc(productId, targetPriceList);

        Optional<ProductPriceTier> matchedTier = tiers.stream()
                .filter(tier -> quantity >= tier.getMinQuantity())
                .findFirst();

        BigDecimal effectiveUnitPrice;
        String appliedRule;

        if (matchedTier.isPresent()) {
            effectiveUnitPrice = matchedTier.get().getTierPrice();
            appliedRule = "TIER_MATCH (" + targetPriceList + " minQty=" + matchedTier.get().getMinQuantity() + ")";
        } else {
            // Fallback to Catalog Base Price (MSRP per Option 1-C)
            effectiveUnitPrice = product.getBasePrice();
            appliedRule = "FALLBACK_CATALOG_MSRP";
        }

        // Compute gross unit price including VAT with commercial HALF_UP rounding
        BigDecimal grossMultiplier = BigDecimal.ONE.add(product.getTaxRate());
        BigDecimal grossUnitPrice = effectiveUnitPrice.multiply(grossMultiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalGrossAmount = grossUnitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        return new EffectivePriceResponse(
                product.getId(),
                product.getSku(),
                targetPriceList,
                quantity,
                effectiveUnitPrice,
                grossUnitPrice,
                totalGrossAmount,
                product.getCurrency(),
                appliedRule
        );
    }

    private Product findEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }
}
