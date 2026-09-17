package com.marketplace.controller;

import com.marketplace.entity.Download;
import com.marketplace.repository.DownloadRepository;
import com.marketplace.security.JwtAuthFilter.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/downloads")
public class DownloadController {

    private final DownloadRepository downloadRepository;

    public DownloadController(DownloadRepository downloadRepository) {
        this.downloadRepository = downloadRepository;
    }

    // "My Purchases" - every card the user has ever paid for, always re-downloadable in HD.
    @GetMapping
    public ResponseEntity<List<Download>> myDownloads(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(downloadRepository.findByUserIdOrderByCreatedAtDesc(user.id()));
    }
}
