package com.duebook.app.controller;

import com.duebook.app.dto.DashboardMetricsDTO;
import com.duebook.app.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.duebook.app.model.User;
import com.duebook.app.repository.UserRepository;
import com.duebook.app.exception.ApplicationException;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    /**
     * Get comprehensive dashboard metrics for the authenticated user
     */
    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsDTO> getDashboardMetrics(Authentication authentication) {
        Long userId = extractUserId(authentication);
        log.info("Fetching dashboard metrics for user ID: {}", userId);

        DashboardMetricsDTO metrics = dashboardService.getDashboardMetrics(userId, null);
        log.info("Dashboard metrics retrieved successfully for user ID: {}", userId);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Get dashboard metrics filtered by shop ID
     */
    @GetMapping("/metrics/shop/{shopId}")
    public ResponseEntity<DashboardMetricsDTO> getDashboardMetricsByShop(
            Authentication authentication,
            @PathVariable Long shopId) {
        Long userId = extractUserId(authentication);
        log.info("Fetching dashboard metrics for user ID: {} and shop ID: {}", userId, shopId);

        DashboardMetricsDTO metrics = dashboardService.getDashboardMetrics(userId, shopId);
        log.info("Dashboard metrics retrieved successfully for user ID: {} and shop ID: {}", userId, shopId);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Extract user ID from the authentication token
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApplicationException("Authentication required", "UNAUTHORIZED");
        }

        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        User user = userRepository.findByPhone(email)
                .orElseThrow(() -> new ApplicationException("User not found", "USER_NOT_FOUND"));

        return user.getId();
    }
}

