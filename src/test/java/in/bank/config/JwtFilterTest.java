package in.bank.config;

import in.bank.security.CustomUserDetailsService;
import in.bank.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter Tests")
class JwtFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter printWriter;

    @InjectMocks
    private JwtFilter jwtFilter;

    private StringWriter responseWriter;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        responseWriter = new StringWriter();
        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC1: No Authorization header - continues filter chain")
    void testDoFilterInternal_NoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("TC2: Authorization header without Bearer - continues filter chain")
    void testDoFilterInternal_HeaderWithoutBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic token");
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("TC3: Valid token - sets authentication and continues chain")
    void testDoFilterInternal_ValidToken() throws Exception {
        String token = "valid.jwt.token";
        String username = "test@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.validateToken(token, userDetails)).thenReturn(true);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(jwtService).extractUsername(token);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtService).validateToken(token, userDetails);
        verify(filterChain).doFilter(request, response);
        
        // Verify authentication is set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        assert auth.getPrincipal().equals(userDetails);
    }

    @Test
    @DisplayName("TC4: Valid token but user already authenticated - skips re-authentication")
    void testDoFilterInternal_AlreadyAuthenticated() throws Exception {
        String token = "valid.jwt.token";
        String username = "test@example.com";
        
        // Set existing authentication
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(username);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtService, never()).validateToken(anyString(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("TC5: Username is null from token - continues without authentication")
    void testDoFilterInternal_NullUsername() throws Exception {
        String token = "valid.jwt.token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(null);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("TC6: Invalid token - JwtException returns 401")
    void testDoFilterInternal_InvalidToken() throws Exception {
        String token = "invalid.jwt.token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenThrow(new JwtException("Invalid token"));
        when(response.getWriter()).thenReturn(printWriter);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(printWriter).write(anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("TC7: Expired token - ExpiredJwtException returns 401")
    void testDoFilterInternal_ExpiredToken() throws Exception {
        String token = "expired.jwt.token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));
        when(response.getWriter()).thenReturn(printWriter);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(printWriter).write(anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("TC8: General exception - returns 500")
    void testDoFilterInternal_GeneralException() throws Exception {
        String token = "some.token";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("Database error"));
        when(response.getWriter()).thenReturn(printWriter);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(response).setContentType("application/json");
        verify(printWriter).write(anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("TC9: Token validation fails - no authentication set")
    void testDoFilterInternal_TokenValidationFails() throws Exception {
        String token = "valid.jwt.token";
        String username = "test@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.validateToken(token, userDetails)).thenReturn(false);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtService).validateToken(token, userDetails);
        verify(filterChain).doFilter(request, response);
        
        // Authentication should not be set
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    @DisplayName("TC10: User not found - exception handled")
    void testDoFilterInternal_UserNotFound() throws Exception {
        String token = "valid.jwt.token";
        String username = "nonexistent@example.com";
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenThrow(new RuntimeException("User not found"));
        when(response.getWriter()).thenReturn(printWriter);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        verify(filterChain, never()).doFilter(request, response);
    }
}