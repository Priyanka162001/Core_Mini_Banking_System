package in.bank.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET_KEY = "my-super-secret-key-my-super-secret-key";

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        SecurityContextHolder.clearContext();
    }

    private UserDetails createUserDetails(String username, String role) {
        return User.builder()
                .username(username)
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
    }

    private String createExpiredToken(String username, String role, Long userId) {
        // Set expiration to 2 seconds ago to ensure it's definitely expired
        Date pastDate = new Date(System.currentTimeMillis() - 5000);
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("userId", userId)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7000))
                .setExpiration(pastDate)
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("TC1: Generate access token successfully")
        void testGenerateToken_Success() {
            UserDetails userDetails = createUserDetails("test@example.com", "ROLE_CUSTOMER");
            Long userId = 123L;

            String token = jwtService.generateToken(userDetails, userId);

            assertThat(token).isNotNull();
            assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
            assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_CUSTOMER");
            assertThat(jwtService.extractUserId(token)).isEqualTo(123L);
        }

        @Test
        @DisplayName("TC2: Generate refresh token successfully")
        void testGenerateRefreshToken_Success() {
            UserDetails userDetails = createUserDetails("test@example.com", "ROLE_CUSTOMER");

            String refreshToken = jwtService.generateRefreshToken(userDetails);

            assertThat(refreshToken).isNotNull();
            assertThat(jwtService.extractUsername(refreshToken)).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("TC3: Generate token for admin user")
        void testGenerateToken_AdminUser() {
            UserDetails userDetails = createUserDetails("admin@bank.com", "ROLE_ADMIN");
            String token = jwtService.generateToken(userDetails, 1L);

            assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("Extraction Tests")
    class ExtractionTests {

        @Test
        @DisplayName("TC4: Extract username from token")
        void testExtractUsername() {
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_CUSTOMER");
            String token = jwtService.generateToken(userDetails, 1L);

            assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("TC5: Extract role from token")
        void testExtractRole() {
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_ADMIN");
            String token = jwtService.generateToken(userDetails, 1L);

            assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("TC6: Extract role throws exception when role claim is missing")
        void testExtractRole_WhenRoleIsNull() {
            String tokenWithoutRole = Jwts.builder()
                    .setSubject("user@example.com")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 10000))
                    .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                    .compact();
            
            assertThatThrownBy(() -> jwtService.extractRole(tokenWithoutRole))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Role not found in token");
        }

        @Test
        @DisplayName("TC7: Extract roles as list from token")
        void testExtractRoles() {
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_CUSTOMER");
            String token = jwtService.generateToken(userDetails, 1L);

            List<String> roles = jwtService.extractRoles(token);
            assertThat(roles).hasSize(1);
            assertThat(roles.get(0)).isEqualTo("ROLE_CUSTOMER");
        }

        @Test
        @DisplayName("TC8: Extract user ID from token")
        void testExtractUserId() {
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_CUSTOMER");
            Long expectedUserId = 456L;
            String token = jwtService.generateToken(userDetails, expectedUserId);

            assertThat(jwtService.extractUserId(token)).isEqualTo(expectedUserId);
        }

        @Test
        @DisplayName("TC9: Extract user ID throws exception when userId claim is missing")
        void testExtractUserId_WhenUserIdIsNull() {
            String tokenWithoutUserId = Jwts.builder()
                    .setSubject("user@example.com")
                    .claim("role", "CUSTOMER")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 10000))
                    .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                    .compact();
            
            assertThatThrownBy(() -> jwtService.extractUserId(tokenWithoutUserId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User ID not found in token");
        }

        @Test
        @DisplayName("TC10: Extract expiration date from token")
        void testExtractExpiration() {
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_CUSTOMER");
            String token = jwtService.generateToken(userDetails, 1L);

            assertThat(jwtService.extractExpiration(token)).isNotNull();
            assertThat(jwtService.extractExpiration(token)).isAfter(new Date());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("TC11: Validate token - valid token")
        void testValidateToken_Valid() {
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_CUSTOMER");
            String token = jwtService.generateToken(userDetails, 1L);

            assertThat(jwtService.validateToken(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("TC12: Validate token - wrong username returns false")
        void testValidateToken_WrongUsername() {
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_CUSTOMER");
            UserDetails wrongUser = createUserDetails("wrong@example.com", "ROLE_CUSTOMER");
            String token = jwtService.generateToken(userDetails, 1L);

            assertThat(jwtService.validateToken(token, wrongUser)).isFalse();
        }

        @Test
        @DisplayName("TC13: Validate token - expired token throws exception")
        void testValidateToken_ExpiredToken_ThrowsException() {
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_CUSTOMER");
            String expiredToken = createExpiredToken("user@example.com", "CUSTOMER", 123L);
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            assertThatThrownBy(() -> jwtService.validateToken(expiredToken, userDetails))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    @DisplayName("Logged-in User ID Tests")
    class LoggedInUserIdTests {

        @Test
        @DisplayName("TC14: Get logged-in user ID from security context")
        void testGetLoggedInUserId_Success() {
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_CUSTOMER");
            String token = jwtService.generateToken(userDetails, 789L);
            
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getCredentials()).thenReturn(token);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            assertThat(jwtService.getLoggedInUserId()).isEqualTo(789L);
        }

        @Test
        @DisplayName("TC15: Get logged-in user ID - no authentication")
        void testGetLoggedInUserId_NoAuthentication() {
            SecurityContextHolder.clearContext();
            assertThatThrownBy(() -> jwtService.getLoggedInUserId())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No authenticated user found");
        }

        @Test
        @DisplayName("TC16: Get logged-in user ID - not authenticated")
        void testGetLoggedInUserId_NotAuthenticated() {
            when(authentication.isAuthenticated()).thenReturn(false);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            assertThatThrownBy(() -> jwtService.getLoggedInUserId())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No authenticated user found");
        }

        @Test
        @DisplayName("TC17: Get logged-in user ID - credentials not a string token")
        void testGetLoggedInUserId_CredentialsNotToken() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getCredentials()).thenReturn(123L);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            assertThatThrownBy(() -> jwtService.getLoggedInUserId())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("JWT token not found");
        }
    }

    @Nested
    @DisplayName("Private Method Tests")
    class PrivateMethodTests {

        @Test
        @DisplayName("TC18: Test isTokenExpired private method with valid token")
        void testIsTokenExpired_ValidToken() throws Exception {
            Method isTokenExpiredMethod = JwtService.class.getDeclaredMethod("isTokenExpired", String.class);
            isTokenExpiredMethod.setAccessible(true);
            
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_CUSTOMER");
            String token = jwtService.generateToken(userDetails, 1L);
            
            boolean isExpired = (boolean) isTokenExpiredMethod.invoke(jwtService, token);
            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("TC19: Test isTokenExpired private method with expired token throws exception")
        void testIsTokenExpired_ExpiredToken() throws Exception {
            Method isTokenExpiredMethod = JwtService.class.getDeclaredMethod("isTokenExpired", String.class);
            isTokenExpiredMethod.setAccessible(true);
            
            String expiredToken = createExpiredToken("user@example.com", "CUSTOMER", 123L);
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            assertThatThrownBy(() -> isTokenExpiredMethod.invoke(jwtService, expiredToken))
                    .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                    .hasCauseInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("TC20: Test extractAllClaims private method")
        void testExtractAllClaims() throws Exception {
            Method extractAllClaimsMethod = JwtService.class.getDeclaredMethod("extractAllClaims", String.class);
            extractAllClaimsMethod.setAccessible(true);
            
            UserDetails userDetails = createUserDetails("user@example.com", "ROLE_CUSTOMER");
            String token = jwtService.generateToken(userDetails, 1L);
            
            Claims claims = (Claims) extractAllClaimsMethod.invoke(jwtService, token);
            
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("user@example.com");
        }
        
        @Test
        @DisplayName("TC21: Test extractAllClaims private method with expired token throws exception")
        void testExtractAllClaims_ExpiredToken() throws Exception {
            Method extractAllClaimsMethod = JwtService.class.getDeclaredMethod("extractAllClaims", String.class);
            extractAllClaimsMethod.setAccessible(true);
            
            String expiredToken = createExpiredToken("user@example.com", "CUSTOMER", 123L);
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            assertThatThrownBy(() -> extractAllClaimsMethod.invoke(jwtService, expiredToken))
                    .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                    .hasCauseInstanceOf(ExpiredJwtException.class);
        }
    }
}