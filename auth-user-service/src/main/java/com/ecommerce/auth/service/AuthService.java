package com.ecommerce.auth.service;

import com.ecommerce.auth.domain.RefreshToken;
import com.ecommerce.auth.domain.Role;
import com.ecommerce.auth.domain.User;
import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.security.JwtTokenProvider;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered: " + request.email());
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role() != null ? request.role() : Role.ROLE_USER);

        User savedUser = userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(savedUser);
        RefreshToken refreshToken = createRefreshToken(savedUser);

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole(),
                tokenProvider.getAccessTokenExpirationMs()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        String accessToken = tokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                tokenProvider.getAccessTokenExpirationMs()
        );
    }

    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        User user = token.getUser();

        // Compromise detection: If a revoked token is used, someone may have intercepted it!
        if (token.isRevoked()) {
            log.warn("SECURITY ALERT: Revoked refresh token used by user {}. Revoking all active tokens.", user.getEmail());
            refreshTokenRepository.revokeAllUserTokens(user);
            throw new BadRequestException("Revoked refresh token used. All active sessions have been terminated for security.");
        }

        if (token.isExpired()) {
            throw new BadRequestException("Expired refresh token. Please log in again.");
        }

        // Token rotation: Create new refresh token and invalidate old one
        String newRefreshTokenStr = tokenProvider.generateRefreshTokenString();
        Instant newExpiry = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());
        RefreshToken newRefreshToken = new RefreshToken(user, newRefreshTokenStr, newExpiry);

        token.setRevoked(true);
        token.setReplacedByToken(newRefreshTokenStr);

        refreshTokenRepository.save(token);
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = tokenProvider.generateAccessToken(user);

        return TokenRefreshResponse.of(
                newAccessToken,
                newRefreshTokenStr,
                tokenProvider.getAccessTokenExpirationMs()
        );
    }

    @Transactional
    public void revokeToken(RevokeTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadRequestException("Refresh token not found"));
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        log.info("Successfully revoked refresh token for user: {}", token.getUser().getEmail());
    }

    @Transactional(readOnly = true)
    public UserResponse validateToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!tokenProvider.validateToken(token)) {
            throw new BadRequestException("Invalid or expired token");
        }

        String email = tokenProvider.getEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for token"));

        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserResponse.fromEntity(user);
    }

    private RefreshToken createRefreshToken(User user) {
        String tokenStr = tokenProvider.generateRefreshTokenString();
        Instant expiry = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());
        RefreshToken refreshToken = new RefreshToken(user, tokenStr, expiry);
        return refreshTokenRepository.save(refreshToken);
    }
}
