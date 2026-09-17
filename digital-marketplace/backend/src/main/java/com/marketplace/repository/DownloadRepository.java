package com.marketplace.repository;

import com.marketplace.entity.Download;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DownloadRepository extends JpaRepository<Download, Long> {
    List<Download> findByUserIdOrderByCreatedAtDesc(Long userId);
}
