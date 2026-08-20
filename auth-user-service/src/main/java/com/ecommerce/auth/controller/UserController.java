package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.UserResponse;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId
    ) {
        if (headerUserId != null) {
            return ResponseEntity.ok(ApiResponse.success(authService.getUserProfile(headerUserId)));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String email) {
            return ResponseEntity.ok(ApiResponse.success(authService.getUserByEmail(email)));
        }

        return ResponseEntity.status(401).body(ApiResponse.error("Unauthenticated request"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(authService.getUserProfile(id)));
    }
}
