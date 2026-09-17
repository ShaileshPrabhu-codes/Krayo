package com.marketplace.repository;

import com.marketplace.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatus(String status);
    List<Product> findByThemeAndStatus(String theme, String status);
}
