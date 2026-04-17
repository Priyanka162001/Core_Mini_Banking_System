package in.bank.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomUserDetails Tests")
class CustomUserDetailsTest {

    @Test
    @DisplayName("TC1: Constructor sets all fields correctly")
    void testConstructor() {
        // Given
        Long expectedId = 123L;
        String expectedEmail = "test@example.com";
        String expectedPassword = "encodedPassword";
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

        // When
        CustomUserDetails userDetails = new CustomUserDetails(
            expectedId,
            expectedEmail,
            expectedPassword,
            authorities
        );

        // Then
        assertThat(userDetails.getId()).isEqualTo(expectedId);
        assertThat(userDetails.getUsername()).isEqualTo(expectedEmail);
        assertThat(userDetails.getPassword()).isEqualTo(expectedPassword);
        assertThat(userDetails.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("TC2: isAccountNonExpired returns true")
    void testIsAccountNonExpired() {
        // Given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", List.of());

        // When & Then
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("TC3: isAccountNonLocked returns true")
    void testIsAccountNonLocked() {
        // Given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", List.of());

        // When & Then
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("TC4: isCredentialsNonExpired returns true")
    void testIsCredentialsNonExpired() {
        // Given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", List.of());

        // When & Then
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("TC5: isEnabled returns true")
    void testIsEnabled() {
        // Given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", List.of());

        // When & Then
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("TC6: Getter methods return correct values")
    void testGetters() {
        // Given
        Long id = 456L;
        String email = "user@example.com";
        String password = "secret";
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_USER")
        );

        // When
        CustomUserDetails userDetails = new CustomUserDetails(id, email, password, authorities);

        // Then
        assertThat(userDetails.getId()).isEqualTo(id);
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo(password);
        assertThat(userDetails.getAuthorities()).hasSize(2);
        assertThat(userDetails.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("TC7: Different user roles work correctly")
    void testDifferentRoles() {
        // Given & When
        CustomUserDetails adminUser = new CustomUserDetails(1L, "admin@bank.com", "password", 
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        CustomUserDetails customerUser = new CustomUserDetails(2L, "customer@bank.com", "password", 
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        // Then - Extract authority names as Strings
        assertThat(adminUser.getAuthorities().iterator().next().getAuthority())
            .isEqualTo("ROLE_ADMIN");
        assertThat(customerUser.getAuthorities().iterator().next().getAuthority())
            .isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("TC8: Multiple authorities work correctly")
    void testMultipleAuthorities() {
        // Given
        List<SimpleGrantedAuthority> multipleAuthorities = List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_MANAGER"),
            new SimpleGrantedAuthority("ROLE_USER")
        );

        // When
        CustomUserDetails userDetails = new CustomUserDetails(1L, "user@example.com", "password", multipleAuthorities);

        // Then
        assertThat(userDetails.getAuthorities()).hasSize(3);
        assertThat(userDetails.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_USER");
    }

    @Test
    @DisplayName("TC9: Empty authorities list works")
    void testEmptyAuthorities() {
        // Given
        List<SimpleGrantedAuthority> emptyAuthorities = List.of();

        // When
        CustomUserDetails userDetails = new CustomUserDetails(1L, "user@example.com", "password", emptyAuthorities);

        // Then
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("TC10: Different user IDs work")
    void testDifferentUserIds() {
        // Given
        Long[] userIds = {1L, 100L, 999L, 1000L};

        for (Long userId : userIds) {
            // When
            CustomUserDetails userDetails = new CustomUserDetails(userId, "user@example.com", "password", List.of());

            // Then
            assertThat(userDetails.getId()).isEqualTo(userId);
        }
    }

    @Test
    @DisplayName("TC11: Null authorities list works")
    void testNullAuthorities() {
        // Given
        CustomUserDetails userDetails = new CustomUserDetails(1L, "user@example.com", "password", null);

        // Then
        assertThat(userDetails.getAuthorities()).isNull();
    }

    @Test
    @DisplayName("TC12: Authority with different role names")
    void testDifferentRoleNames() {
        // Given
        String[] roleNames = {"ROLE_SUPER_ADMIN", "ROLE_SUPPORT", "ROLE_VIEWER", "ROLE_AUDITOR"};

        for (String roleName : roleNames) {
            // When
            CustomUserDetails userDetails = new CustomUserDetails(1L, "user@example.com", "password", 
                List.of(new SimpleGrantedAuthority(roleName)));

            // Then
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo(roleName);
        }
    }
}