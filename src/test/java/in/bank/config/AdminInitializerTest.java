package in.bank.config;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInitializer Tests")
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @Test
    @DisplayName("TC1: Should create admin when admin does not exist")
    void testCreateAdmin_WhenAdminDoesNotExist() {
        // Given
        when(userRepository.findByEmail("admin@bank.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        adminInitializer.createAdmin();

        // Then
        verify(userRepository, times(1)).findByEmail("admin@bank.com");
        verify(passwordEncoder, times(1)).encode("admin123");
        verify(userRepository, times(1)).save(any(AppUser.class));
    }

    @Test
    @DisplayName("TC2: Should skip creating admin when admin already exists")
    void testCreateAdmin_WhenAdminAlreadyExists() {
        // Given
        AppUser existingAdmin = AppUser.builder()
                .email("admin@bank.com")
                .role(UserRole.ROLE_ADMIN)
                .build();
        when(userRepository.findByEmail("admin@bank.com")).thenReturn(Optional.of(existingAdmin));

        // When
        adminInitializer.createAdmin();

        // Then
        verify(userRepository, times(1)).findByEmail("admin@bank.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("TC3: Should create admin with correct fields")
    void testCreateAdmin_WithCorrectFields() {
        // Given
        when(userRepository.findByEmail("admin@bank.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encodedPassword123");
        
        // When
        adminInitializer.createAdmin();

        // Then
        verify(userRepository).save(argThat(admin -> 
            admin.getFirstName().equals("Super") &&
            admin.getLastName().equals("Admin") &&
            admin.getEmail().equals("admin@bank.com") &&
            admin.getPhoneNumber().equals("9999999999") &&
            admin.getCountryCode().equals("+91") &&
            admin.getPassword().equals("encodedPassword123") &&
            admin.getRole().equals(UserRole.ROLE_ADMIN) &&
            admin.getStatus().equals(UserStatus.ACTIVE) &&
            admin.getEmailVerified() == true
        ));
    }

    @Test
    @DisplayName("TC4: Should handle repository exception gracefully")
    void testCreateAdmin_WhenRepositoryThrowsException() {
        // Given
        when(userRepository.findByEmail("admin@bank.com"))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            adminInitializer.createAdmin();
        });
        
        verify(userRepository).findByEmail("admin@bank.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(AppUser.class));
    }
}