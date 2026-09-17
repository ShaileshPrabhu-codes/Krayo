package com.marketplace.repository;

import com.marketplace.entity.Customization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomizationRepository extends JpaRepository<Customization, Long> {
    Optional<Customization> findByOrderId(Long orderId);
}
