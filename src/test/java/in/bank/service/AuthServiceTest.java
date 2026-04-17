package in.bank.service;

import in.bank.dto.AuthRequest;
import in.bank.dto.AuthResponse;
import in.bank.dto.RegisterRequestDTO;
import in.bank.entity.AppUser;
import in.bank.entity.UserRole;
import in.bank.entity.UserStatus;
import in.bank.exception.BadRequestException;
import in.bank.exception.DuplicateResourceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.UserRepository;
import in.bank.security.CustomUserDetails;
import in.bank.security.CustomUserDetailsService;
import in.bank.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDTO registerRequest;
    private AppUser activeUser;
    private AppUser pendingUser;
    private AppUser adminUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequestDTO();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setCountryCode("+91");
        registerRequest.setPhoneNumber("9876543210");
        registerRequest.setPassword("Pass@1234");
        registerRequest.setConfirmPassword("Pass@1234");

        activeUser = AppUser.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("+919876543210")
                .password("encodedPassword")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        pendingUser = AppUser.builder()
                .id(2L)
                .email("pending@example.com")
                .otp("123456")
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .emailVerified(false)
                .status(UserStatus.PENDING)
                .role(UserRole.ROLE_CUSTOMER)
                .build();

        adminUser = AppUser.builder()
                .id(3L)
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Nested
    @DisplayName("Register Customer Tests")
    class RegisterCustomerTests {

        @Test
        @DisplayName("TC1: Register customer success")
        void registerCustomer_Success() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            doNothing().when(emailService).sendOtp(anyString(), anyString());

            assertDoesNotThrow(() -> authService.registerCustomer(registerRequest));

            verify(userRepository).save(any(AppUser.class));
            verify(emailService).sendOtp(eq("john@example.com"), anyString());
        }

        @Test
        @DisplayName("TC2: Duplicate email throws exception")
        void registerCustomer_DuplicateEmail_ThrowsException() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThrows(DuplicateResourceException.class,
                    () -> authService.registerCustomer(registerRequest));
        }

        @Test
        @DisplayName("TC3: Duplicate phone number throws exception")
        void registerCustomer_DuplicatePhone_ThrowsException() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhoneNumber("+919876543210")).thenReturn(true);

            assertThrows(DuplicateResourceException.class,
                    () -> authService.registerCustomer(registerRequest));
        }

        @Test
        @DisplayName("TC4: Password mismatch throws exception")
        void registerCustomer_PasswordMismatch_ThrowsException() {
            registerRequest.setConfirmPassword("WrongPass@1");

            assertThrows(BadRequestException.class,
                    () -> authService.registerCustomer(registerRequest));
        }
    }

    @Nested
    @DisplayName("Verify OTP Tests")
    class VerifyOtpTests {

        @Test
        @DisplayName("TC5: Verify OTP success")
        void verifyOtp_Success() {
            when(userRepository.findByEmail("pending@example.com"))
                    .thenReturn(Optional.of(pendingUser));

            assertDoesNotThrow(() -> authService.verifyOtp("pending@example.com", "123456"));

            assertTrue(pendingUser.getEmailVerified());
            assertEquals(UserStatus.ACTIVE, pendingUser.getStatus());
            assertNull(pendingUser.getOtp());
            verify(userRepository).save(pendingUser);
        }

        @Test
        @DisplayName("TC6: Wrong OTP throws exception")
        void verifyOtp_WrongOtp_ThrowsException() {
            when(userRepository.findByEmail("pending@example.com"))
                    .thenReturn(Optional.of(pendingUser));

            assertThrows(BadRequestException.class,
                    () -> authService.verifyOtp("pending@example.com", "999999"));
        }

        @Test
        @DisplayName("TC7: Expired OTP throws exception")
        void verifyOtp_ExpiredOtp_ThrowsException() {
            pendingUser.setOtpExpiry(LocalDateTime.now().minusMinutes(10));
            when(userRepository.findByEmail("pending@example.com"))
                    .thenReturn(Optional.of(pendingUser));

            assertThrows(BadRequestException.class,
                    () -> authService.verifyOtp("pending@example.com", "123456"));
        }

        @Test
        @DisplayName("TC8: OTP expiry is null throws exception")
        void verifyOtp_NullExpiry_ThrowsException() {
            pendingUser.setOtpExpiry(null);
            when(userRepository.findByEmail("pending@example.com"))
                    .thenReturn(Optional.of(pendingUser));

            assertThrows(BadRequestException.class,
                    () -> authService.verifyOtp("pending@example.com", "123456"));
        }

        @Test
        @DisplayName("TC9: User not found throws exception")
        void verifyOtp_UserNotFound_ThrowsException() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> authService.verifyOtp("notfound@example.com", "123456"));
        }
    }

    @Nested
    @DisplayName("Resend OTP Tests")
    class ResendOtpTests {

        @Test
        @DisplayName("TC10: Resend OTP success")
        void resendOtp_Success() {
            when(userRepository.findByEmail("pending@example.com"))
                    .thenReturn(Optional.of(pendingUser));
            doNothing().when(emailService).sendOtp(anyString(), anyString());

            assertDoesNotThrow(() -> authService.resendOtp("pending@example.com"));

            verify(userRepository).save(pendingUser);
            verify(emailService).sendOtp(eq("pending@example.com"), anyString());
        }

        @Test
        @DisplayName("TC11: Already verified user cannot resend OTP")
        void resendOtp_AlreadyVerified_ThrowsException() {
            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(activeUser));

            assertThrows(BadRequestException.class,
                    () -> authService.resendOtp("john@example.com"));
        }

        @Test
        @DisplayName("TC12: User not found on resend OTP")
        void resendOtp_UserNotFound_ThrowsException() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> authService.resendOtp("notfound@example.com"));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("TC13: Customer login success")
        void login_Customer_Success() {
            AuthRequest request = new AuthRequest("john@example.com", "Pass@1234");

            CustomUserDetails userDetails = new CustomUserDetails(
                    1L, "john@example.com", "encodedPassword",
                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            );

            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(activeUser));
            when(authenticationManager.authenticate(any()))
                    .thenReturn(new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()));
            when(jwtService.generateToken(any(), anyLong())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("accessToken", response.getAccessToken());
            assertEquals("refreshToken", response.getRefreshToken());
            assertEquals("john@example.com", response.getEmail());
            assertEquals("CUSTOMER", response.getRole());
        }

        @Test
        @DisplayName("TC14: Admin login success")
        void login_Admin_Success() {
            AuthRequest request = new AuthRequest("admin@example.com", "Pass@1234");

            CustomUserDetails adminDetails = new CustomUserDetails(
                    3L, "admin@example.com", "encodedPassword",
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );

            when(userRepository.findByEmail("admin@example.com"))
                    .thenReturn(Optional.of(adminUser));
            when(authenticationManager.authenticate(any()))
                    .thenReturn(new UsernamePasswordAuthenticationToken(
                            adminDetails, null, adminDetails.getAuthorities()));
            when(jwtService.generateToken(any(), anyLong())).thenReturn("adminAccessToken");
            when(jwtService.generateRefreshToken(any())).thenReturn("adminRefreshToken");

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals("adminAccessToken", response.getAccessToken());
            assertEquals("ADMIN", response.getRole());
        }

        @Test
        @DisplayName("TC15: Email not verified throws exception")
        void login_EmailNotVerified_ThrowsException() {
            activeUser.setEmailVerified(false);
            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(activeUser));

            AuthRequest request = new AuthRequest("john@example.com", "Pass@1234");

            assertThrows(BadRequestException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("TC16: Account not active throws exception")
        void login_AccountNotActive_ThrowsException() {
            activeUser.setStatus(UserStatus.PENDING);
            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(activeUser));

            AuthRequest request = new AuthRequest("john@example.com", "Pass@1234");

            assertThrows(BadRequestException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("TC17: User not found on login")
        void login_UserNotFound_ThrowsException() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> authService.login(new AuthRequest("no@one.com", "Pass@1234")));
        }
        
        @Test
        @DisplayName("TC18: Customer with null status - login throws BadRequestException")
        void login_CustomerWithNullStatus_ThrowsException() {
            AppUser customerWithNullStatus = AppUser.builder()
                    .id(5L)
                    .email("customer2@example.com")
                    .role(UserRole.ROLE_CUSTOMER)
                    .status(null)
                    .emailVerified(true)
                    .build();

            when(userRepository.findByEmail("customer2@example.com"))
                    .thenReturn(Optional.of(customerWithNullStatus));

            AuthRequest request = new AuthRequest("customer2@example.com", "Pass@1234");

            // Should throw exception because status is not ACTIVE
            assertThrows(BadRequestException.class, () -> authService.login(request));
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("TC19: Refresh token success")
        void refreshToken_Success() {
            CustomUserDetails userDetails = new CustomUserDetails(
                    1L, "john@example.com", "encodedPassword",
                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            );

            when(jwtService.extractUsername("validRefreshToken"))
                    .thenReturn("john@example.com");
            when(userDetailsService.loadUserByUsername("john@example.com"))
                    .thenReturn(userDetails);
            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(activeUser));
            when(jwtService.validateToken("validRefreshToken", userDetails))
                    .thenReturn(true);
            when(jwtService.generateToken(any(), anyLong()))
                    .thenReturn("newAccessToken");

            String newToken = authService.refreshToken("validRefreshToken");

            assertEquals("newAccessToken", newToken);
        }

        @Test
        @DisplayName("TC20: Invalid refresh token throws exception")
        void refreshToken_InvalidToken_ThrowsException() {
            CustomUserDetails userDetails = new CustomUserDetails(
                    1L, "john@example.com", "encodedPassword",
                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            );

            when(jwtService.extractUsername("badToken")).thenReturn("john@example.com");
            when(userDetailsService.loadUserByUsername("john@example.com"))
                    .thenReturn(userDetails);
            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(activeUser));
            when(jwtService.validateToken("badToken", userDetails)).thenReturn(false);

            assertThrows(BadRequestException.class,
                    () -> authService.refreshToken("badToken"));
        }

        @Test
        @DisplayName("TC21: User not found during refresh token")
        void refreshToken_UserNotFound_ThrowsException() {
            when(jwtService.extractUsername("someToken")).thenReturn("notfound@example.com");
            when(userDetailsService.loadUserByUsername("notfound@example.com"))
                    .thenThrow(new ResourceNotFoundException("User not found"));

            assertThrows(ResourceNotFoundException.class,
                    () -> authService.refreshToken("someToken"));
        }
    }

    @Nested
    @DisplayName("Register Admin Tests")
    class RegisterAdminTests {

        @Test
        @DisplayName("TC22: Register admin success")
        void registerAdmin_Success() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

            assertDoesNotThrow(() -> authService.registerAdmin(registerRequest));

            verify(userRepository).save(argThat(user ->
                    user.getRole() == UserRole.ROLE_ADMIN &&
                    user.getStatus() == UserStatus.ACTIVE &&
                    Boolean.TRUE.equals(user.getEmailVerified())
            ));
        }

        @Test
        @DisplayName("TC23: Duplicate email for admin throws exception")
        void registerAdmin_DuplicateEmail_ThrowsException() {
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            assertThrows(DuplicateResourceException.class,
                    () -> authService.registerAdmin(registerRequest));
        }

        @Test
        @DisplayName("TC24: Duplicate phone for admin throws exception")
        void registerAdmin_DuplicatePhone_ThrowsException() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhoneNumber(anyString())).thenReturn(true);

            assertThrows(DuplicateResourceException.class,
                    () -> authService.registerAdmin(registerRequest));
        }

        @Test
        @DisplayName("TC25: Password mismatch for admin throws exception")
        void registerAdmin_PasswordMismatch_ThrowsException() {
            registerRequest.setConfirmPassword("WrongPass@1");
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);

            assertThrows(BadRequestException.class,
                    () -> authService.registerAdmin(registerRequest));
        }
    }
}