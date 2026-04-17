package in.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bank.dto.SavingsAccountResponseDTO;
import in.bank.entity.AccountLifecycleStatus;
import in.bank.exception.ResourceNotFoundException;
import in.bank.security.JwtService;
import in.bank.service.SavingsAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = SavingsAccountController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = in.bank.config.JwtFilter.class
    )
)
@DisplayName("SavingsAccountController Tests")
class SavingsAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SavingsAccountService service;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private SavingsAccountResponseDTO sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = SavingsAccountResponseDTO.builder()
                .id(100L)
                .accountNumber("SA-123")
                .accountStatus("ACTIVE")
                .currentBalanceAmount(new BigDecimal("5000"))
                .userId(1L)
                .productName("Savings")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ================= GET ALL ACCOUNTS TESTS =================

    @Test
    @DisplayName("TC1: Get all accounts - Success for CUSTOMER role")
    @WithMockUser(roles = "CUSTOMER")
    void getAll_WithValidToken_ShouldReturnAccounts() throws Exception {
        when(jwtService.extractUserId(anyString())).thenReturn(1L);
        when(service.getAccountsByUserId(1L)).thenReturn(List.of(sampleAccount));

        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Accounts fetched"))
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].accountNumber").value("SA-123"));
    }

    @Test
    @DisplayName("TC2: Get all accounts - No accounts for user")
    @WithMockUser(roles = "CUSTOMER")
    void getAll_WhenNoAccounts_ShouldReturnEmptyList() throws Exception {
        when(jwtService.extractUserId(anyString())).thenReturn(1L);
        when(service.getAccountsByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("TC3: Get all accounts - Missing Authorization header returns 401")
    void getAll_WithoutAuthHeader_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    // ================= GET ACCOUNT BY ID TESTS =================

    @Test
    @DisplayName("TC4: Get account by ID - Success for CUSTOMER")
    @WithMockUser(roles = "CUSTOMER")
    void getById_WithValidId_ShouldReturnAccount() throws Exception {
        when(service.getById(eq(100L), any())).thenReturn(sampleAccount);

        mockMvc.perform(get("/api/v1/accounts/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.accountNumber").value("SA-123"));
    }

    @Test
    @DisplayName("TC5: Get account by ID - Success for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void getById_AsAdmin_ShouldReturnAccount() throws Exception {
        when(service.getById(eq(100L), any())).thenReturn(sampleAccount);

        mockMvc.perform(get("/api/v1/accounts/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    @DisplayName("TC6: Get account by ID - Account not found")
    @WithMockUser(roles = "CUSTOMER")
    void getById_WhenAccountNotFound_ShouldReturnNotFound() throws Exception {
        when(service.getById(eq(999L), any())).thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get("/api/v1/accounts/999"))
                .andExpect(status().isNotFound());
    }

    // ================= UPDATE ACCOUNT STATUS TESTS =================

    @Test
    @DisplayName("TC7: Update account status to ACTIVE - ADMIN")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateStatus_ToActive_ByAdmin_Success() throws Exception {
        doNothing().when(service).updateAccountStatus(100L, AccountLifecycleStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/accounts/100/status/ACTIVE")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Account reactivated successfully"))
                .andExpect(jsonPath("$.code").value("ACC_200"));
    }

    @Test
    @DisplayName("TC8: Update account status to FROZEN - ADMIN")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateStatus_ToFrozen_ByAdmin_Success() throws Exception {
        doNothing().when(service).updateAccountStatus(100L, AccountLifecycleStatus.FROZEN);

        mockMvc.perform(put("/api/v1/accounts/100/status/FROZEN")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account frozen successfully"));
    }

    @Test
    @DisplayName("TC9: Update account status to CLOSED - ADMIN")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateStatus_ToClosed_ByAdmin_Success() throws Exception {
        doNothing().when(service).updateAccountStatus(100L, AccountLifecycleStatus.CLOSED);

        mockMvc.perform(put("/api/v1/accounts/100/status/CLOSED")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account closed successfully"));
    }

    @Test
    @DisplayName("TC10: Update account status - Forbidden for CUSTOMER")
    @WithMockUser(roles = "CUSTOMER")
    void updateStatus_AsCustomer_ShouldBeForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/accounts/100/status/ACTIVE"))
                .andExpect(status().isForbidden());
    }

    
    // ================= EDGE CASE TESTS =================

    @Test
    @DisplayName("TC12: Extract userId - Invalid token format")
    @WithMockUser(roles = "CUSTOMER")
    void extractUserId_WithInvalidToken_ShouldThrowException() throws Exception {
        when(jwtService.extractUserId(anyString())).thenThrow(new RuntimeException("Invalid token"));

        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Invalid-Token"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC13: Extract userId - Null token")
    @WithMockUser(roles = "CUSTOMER")
    void extractUserId_WithNullToken_ShouldThrowException() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", ""))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC14: Update status - Verify switch statement covers all cases")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateStatus_VerifyAllSwitchCases() throws Exception {
        // Test ACTIVE case
        doNothing().when(service).updateAccountStatus(100L, AccountLifecycleStatus.ACTIVE);
        mockMvc.perform(put("/api/v1/accounts/100/status/ACTIVE")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account reactivated successfully"));
        
        // Test FROZEN case
        doNothing().when(service).updateAccountStatus(100L, AccountLifecycleStatus.FROZEN);
        mockMvc.perform(put("/api/v1/accounts/100/status/FROZEN")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account frozen successfully"));
        
        // Test CLOSED case
        doNothing().when(service).updateAccountStatus(100L, AccountLifecycleStatus.CLOSED);
        mockMvc.perform(put("/api/v1/accounts/100/status/CLOSED")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account closed successfully"));
    }

    @Test
    @DisplayName("TC15: Extract userId - Missing Bearer prefix")
    @WithMockUser(roles = "CUSTOMER")
    void extractUserId_WithoutBearerPrefix_ShouldThrowException() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "token-without-bearer"))
                .andExpect(status().is4xxClientError());
    }
    @Test
    @DisplayName("TC16: Extract userId - No Authorization header (covers yellow line)")
    @WithMockUser(roles = "CUSTOMER")
    void extractUserId_NoAuthHeader_ShouldThrowException() throws Exception {
        // This tests the condition: header == null
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC17: Extract userId - Authorization header without Bearer prefix (covers yellow line)")
    @WithMockUser(roles = "CUSTOMER")
    void extractUserId_HeaderWithoutBearerPrefix_ShouldThrowException() throws Exception {
        // This tests the condition: !header.startsWith("Bearer ")
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Basic token"))
                .andExpect(status().is4xxClientError());
    }
}