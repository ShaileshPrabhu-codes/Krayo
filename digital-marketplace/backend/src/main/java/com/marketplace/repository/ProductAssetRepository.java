package com.marketplace.repository;

import com.marketplace.entity.ProductAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductAssetRepository extends JpaRepository<ProductAsset, Long> {
    List<ProductAsset> findByProductId(Long productId);
}
