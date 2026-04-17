package in.bank.config;

import in.bank.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditorConfig Tests")
class AuditorConfigTest {

    private AuditorConfig auditorConfig;

    @BeforeEach
    void setUp() {
        auditorConfig = new AuditorConfig();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC1: Returns empty when authentication is null")
    void testAuditorProvider_WhenAuthIsNull() {
        SecurityContextHolder.clearContext();
        
        Optional<Long> auditor = auditorConfig.auditorProvider().getCurrentAuditor();
        
        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("TC2: Returns empty when authentication is not authenticated")
    void testAuditorProvider_WhenNotAuthenticated() {
        var auth = new UsernamePasswordAuthenticationToken("user", "password");
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        Optional<Long> auditor = auditorConfig.auditorProvider().getCurrentAuditor();
        
        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("TC3: Returns empty when principal is anonymousUser")
    void testAuditorProvider_WhenPrincipalIsAnonymousUser() {
        var auth = new UsernamePasswordAuthenticationToken("anonymousUser", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        Optional<Long> auditor = auditorConfig.auditorProvider().getCurrentAuditor();
        
        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("TC4: Returns user ID when principal is CustomUserDetails")
    void testAuditorProvider_WhenPrincipalIsCustomUserDetails() {
        CustomUserDetails customUserDetails = new CustomUserDetails(
            123L,
            "test@example.com",
            "encodedPassword",
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        
        var auth = new UsernamePasswordAuthenticationToken(
            customUserDetails, 
            null, 
            customUserDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        Optional<Long> auditor = auditorConfig.auditorProvider().getCurrentAuditor();
        
        assertThat(auditor).isPresent();
        assertThat(auditor.get()).isEqualTo(123L);
    }

    @Test
    @DisplayName("TC5: Returns empty when principal is regular UserDetails")
    void testAuditorProvider_WhenPrincipalIsRegularUserDetails() {
        UserDetails regularUser = User.builder()
                .username("regularUser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        
        var auth = new UsernamePasswordAuthenticationToken(
            regularUser, 
            null, 
            regularUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        Optional<Long> auditor = auditorConfig.auditorProvider().getCurrentAuditor();
        
        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("TC6: Returns empty when principal is string (not anonymousUser)")
    void testAuditorProvider_WhenPrincipalIsString() {
        // Use 3-parameter constructor with empty authorities
        var auth = new UsernamePasswordAuthenticationToken("someUser", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        Optional<Long> auditor = auditorConfig.auditorProvider().getCurrentAuditor();
        
        assertThat(auditor).isEmpty();
    }

    @Test
    @DisplayName("TC7: Returns user ID for different user IDs")
    void testAuditorProvider_WithDifferentUserIds() {
        Long[] userIds = {1L, 100L, 999L, 1000L};
        
        for (Long userId : userIds) {
            CustomUserDetails customUserDetails = new CustomUserDetails(
                userId,
                "user" + userId + "@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            
            var auth = new UsernamePasswordAuthenticationToken(
                customUserDetails, 
                null, 
                customUserDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            Optional<Long> auditor = auditorConfig.auditorProvider().getCurrentAuditor();
            
            assertThat(auditor).isPresent();
            assertThat(auditor.get()).isEqualTo(userId);
            
            SecurityContextHolder.clearContext();
        }
    }
}