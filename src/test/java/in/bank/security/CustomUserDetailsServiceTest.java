package in.bank.security;

import in.bank.entity.AppUser;
import in.bank.entity.UserRole;
import in.bank.entity.UserStatus;
import in.bank.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("TC1: Load user by username - Success")
    void testLoadUserByUsername_Success() {
        // Given
        String email = "test@example.com";
        AppUser mockUser = AppUser.builder()
                .id(1L)
                .email(email)
                .password("encodedPassword")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
            .isEqualTo("ROLE_CUSTOMER");
        
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("TC2: Load user by username - User Not Found")
    void testLoadUserByUsername_UserNotFound() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("User not found");
        
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("TC3: Load user by username - Admin role")
    void testLoadUserByUsername_AdminRole() {
        // Given
        String email = "admin@bank.com";
        AppUser mockUser = AppUser.builder()
                .id(2L)
                .email(email)
                .password("adminPassword")
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
            .isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("TC4: Load user by username - Inactive user")
    void testLoadUserByUsername_InactiveUser() {
        // Given
        String email = "inactive@example.com";
        AppUser mockUser = AppUser.builder()
                .id(3L)
                .email(email)
                .password("password")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.BLOCKED)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(email);
        // User can still authenticate, status should be checked elsewhere
        assertThat(userDetails.isEnabled()).isTrue(); // CustomUserDetails always returns true
    }

    @Test
    @DisplayName("TC5: Load user by username - Verify CustomUserDetails type")
    void testLoadUserByUsername_ReturnsCustomUserDetails() {
        // Given
        String email = "user@example.com";
        AppUser mockUser = AppUser.builder()
                .id(5L)
                .email(email)
                .password("password")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Then
        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        assertThat(customUserDetails.getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("TC6: Load user by username - Empty email throws exception")
    void testLoadUserByUsername_EmptyEmail() {
        // Given
        String email = "";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("User not found");
    }

    @Test
    @DisplayName("TC7: Load user by username - Null email throws exception")
    void testLoadUserByUsername_NullEmail() {
        // Given
        when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(null))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("User not found");
    }
}