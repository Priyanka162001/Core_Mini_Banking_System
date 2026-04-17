package in.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bank.config.TestSecurityConfig;
import in.bank.dto.*;
import in.bank.exception.BadRequestException;
import in.bank.exception.DuplicateResourceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = in.bank.config.JwtFilter.class
    )
)
@Import(TestSecurityConfig.class)  // ✅ Import test security config
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired 
    private MockMvc mockMvc;
    
    @Autowired 
    private ObjectMapper objectMapper;

    @MockBean 
    private AuthService authService;

    private RegisterRequestDTO registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = buildRegisterRequest();
    }

    // =================== REGISTER CUSTOMER ===================

    @Test
    @DisplayName("TC1: Register customer - Success returns 200")
    void registerCustomer_Success_Returns200() throws Exception {
        doNothing().when(authService).registerCustomer(any());

        mockMvc.perform(post("/api/v1/auth/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Customer registered successfully. OTP sent to email."))
                .andExpect(jsonPath("$.code").value("AUTH_201"));
    }

    @Test
    @DisplayName("TC2: Register customer - Duplicate email returns 409")
    void registerCustomer_DuplicateEmail_Returns409() throws Exception {
        doThrow(new DuplicateResourceException("Email already registered"))
                .when(authService).registerCustomer(any());

        mockMvc.perform(post("/api/v1/auth/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC3: Register customer - Invalid email returns 400")
    void registerCustomer_InvalidEmail_Returns400() throws Exception {
        RegisterRequestDTO request = buildRegisterRequest();
        request.setEmail("invalid-email");

        mockMvc.perform(post("/api/v1/auth/customers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =================== REGISTER ADMIN ===================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC4: Register admin - Admin success returns 200")
    void registerAdmin_Success_Returns200() throws Exception {
        doNothing().when(authService).registerAdmin(any());

        mockMvc.perform(post("/api/v1/auth/admins/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Admin registered successfully"))
                .andExpect(jsonPath("$.code").value("AUTH_201"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC5: Register admin - Duplicate email returns 409")
    void registerAdmin_DuplicateEmail_Returns409() throws Exception {
        doThrow(new DuplicateResourceException("Email already registered"))
                .when(authService).registerAdmin(any());

        mockMvc.perform(post("/api/v1/auth/admins/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC6: Register admin - Invalid email returns 400")
    void registerAdmin_InvalidEmail_Returns400() throws Exception {
        RegisterRequestDTO request = buildRegisterRequest();
        request.setEmail("invalid-email");

        mockMvc.perform(post("/api/v1/auth/admins/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC7: Register admin - Customer role returns forbidden")
    void registerAdmin_CustomerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/admins/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isForbidden());
        
        verify(authService, never()).registerAdmin(any());
    }

    @Test
    @DisplayName("TC8: Register admin - No authentication returns 401")
    void registerAdmin_NoAuth_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/admins/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isUnauthorized());
        
        verify(authService, never()).registerAdmin(any());
    }

    // =================== VERIFY OTP ===================

    @Test
    @DisplayName("TC9: Verify OTP - Success returns 200")
    void verifyOtp_Success_Returns200() throws Exception {
        VerifyOtpRequestDTO request = new VerifyOtpRequestDTO();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        doNothing().when(authService).verifyOtp(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OTP verified successfully. You can now login."))
                .andExpect(jsonPath("$.code").value("AUTH_200"));
    }

    @Test
    @DisplayName("TC10: Verify OTP - Invalid OTP returns 400")
    void verifyOtp_InvalidOtp_Returns400() throws Exception {
        VerifyOtpRequestDTO request = new VerifyOtpRequestDTO();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        doThrow(new BadRequestException("Invalid OTP"))
                .when(authService).verifyOtp(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =================== RESEND OTP ===================

    @Test
    @DisplayName("TC11: Resend OTP - Success returns 200")
    void resendOtp_Success_Returns200() throws Exception {
        ResendOtpRequestDTO request = new ResendOtpRequestDTO();
        request.setEmail("john@example.com");

        doNothing().when(authService).resendOtp(anyString());

        mockMvc.perform(post("/api/v1/auth/otp/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("New OTP sent to email."))
                .andExpect(jsonPath("$.code").value("AUTH_200"));
    }

    @Test
    @DisplayName("TC12: Resend OTP - User not found returns 404")
    void resendOtp_UserNotFound_Returns404() throws Exception {
        ResendOtpRequestDTO request = new ResendOtpRequestDTO();
        request.setEmail("notfound@example.com");

        doThrow(new ResourceNotFoundException("User not found"))
                .when(authService).resendOtp(anyString());

        mockMvc.perform(post("/api/v1/auth/otp/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // =================== LOGIN ===================

    @Test
    @DisplayName("TC13: Login - Success returns 200 with tokens")
    void login_Success_Returns200WithTokens() throws Exception {
        AuthRequest request = new AuthRequest("john@example.com", "Pass@1234");

        AuthResponse response = AuthResponse.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .email("john@example.com")
                .role("CUSTOMER")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.code").value("AUTH_200"));
    }

    @Test
    @DisplayName("TC14: Login - User not found returns 404")
    void login_UserNotFound_Returns404() throws Exception {
        when(authService.login(any()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new AuthRequest("no@one.com", "Pass@1234"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC15: Login - Invalid credentials returns 400")
    void login_InvalidCredentials_Returns400() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadRequestException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new AuthRequest("john@example.com", "wrongpassword"))))
                .andExpect(status().isBadRequest());
    }

    // =================== REFRESH TOKEN ===================

    @Test
    @DisplayName("TC16: Refresh token - Success returns 200")
    void refreshToken_Success_Returns200() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");

        when(authService.refreshToken("validRefreshToken"))
                .thenReturn("newAccessToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.code").value("AUTH_200"));
    }

    @Test
    @DisplayName("TC17: Refresh token - Invalid token returns 400")
    void refreshToken_Invalid_Returns400() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("badToken");

        when(authService.refreshToken("badToken"))
                .thenThrow(new BadRequestException("Invalid or expired refresh token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =================== HELPER ===================

    private RegisterRequestDTO buildRegisterRequest() {
        RegisterRequestDTO r = new RegisterRequestDTO();
        r.setFirstName("John");
        r.setLastName("Doe");
        r.setEmail("john@example.com");
        r.setCountryCode("+91");
        r.setPhoneNumber("9876543210");
        r.setPassword("Pass@1234");
        r.setConfirmPassword("Pass@1234");
        return r;
    }
}