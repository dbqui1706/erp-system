package com.erpium.erp.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ProductPriceTier entities.
 */
@Repository
public interface ProductPriceTierRepository extends JpaRepository<ProductPriceTier, Long> {

    List<ProductPriceTier> findByProductIdAndPriceListCodeOrderByMinQuantityDesc(Long productId, String priceListCode);

    List<ProductPriceTier> findByProductIdOrderByPriceListCodeAscMinQuantityAsc(Long productId);
}
