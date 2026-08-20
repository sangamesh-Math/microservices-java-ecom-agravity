package com.ecommerce.auth.security;

import com.ecommerce.auth.domain.Role;
import com.ecommerce.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(tokenProvider, "jwtAccessExpirationMs", 900000L);
        ReflectionTestUtils.setField(tokenProvider, "jwtRefreshExpirationMs", 604800000L);
        tokenProvider.init();
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully() {
        User user = new User(1L, "john.doe@example.com", "secret", "John Doe", Role.ROLE_USER);

        String token = tokenProvider.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("john.doe@example.com", tokenProvider.getEmailFromToken(token));
        assertEquals(1L, tokenProvider.getUserIdFromToken(token));
        assertEquals("ROLE_USER", tokenProvider.getRoleFromToken(token));
    }

    @Test
    void shouldGenerateUniqueRefreshTokenStrings() {
        String token1 = tokenProvider.generateRefreshTokenString();
        String token2 = tokenProvider.generateRefreshTokenString();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.token.structure"));
    }
}
