package in.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bank.config.TestSecurityConfig;
import in.bank.dto.KycRequestDTO;
import in.bank.dto.KycResponseDTO;
import in.bank.entity.KycVerificationStatus;
import in.bank.security.JwtService;
import in.bank.security.WithMockCustomer;
import in.bank.service.CustomerKycService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = CustomerKycController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = in.bank.config.JwtFilter.class
    )
)
@Import(TestSecurityConfig.class)
@DisplayName("CustomerKycController Tests")
class CustomerKycControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomerKycService kycService;

    private KycRequestDTO kycRequest;
    private KycResponseDTO kycResponse;

    @BeforeEach
    void setUp() {
        kycRequest = new KycRequestDTO();
        kycRequest.setDocType("AADHAAR");
        kycRequest.setDocNumber("123456789012");
        kycRequest.setDocumentImageUrl("https://example.com/aadhaar.jpg");

        kycResponse = new KycResponseDTO();
        kycResponse.setKycRecordId(1L);
        kycResponse.setCustomerId(1L);
        kycResponse.setDocType("AADHAAR");
        kycResponse.setDocNumber("123456789012");
        kycResponse.setDocumentImageUrl("https://example.com/aadhaar.jpg");
        kycResponse.setKycVerificationStatus("PENDING");
        kycResponse.setCreatedAt(LocalDateTime.now());
        kycResponse.setCreatedBy(1L);
        
        // Default JwtService mocks
        when(jwtService.extractUserId(anyString())).thenReturn(1L);
        when(jwtService.extractRoles(anyString())).thenReturn(List.of("ROLE_CUSTOMER"));
    }

    // =================== SUBMIT KYC TESTS ===================
    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC1: Submit KYC - Customer success")
    void submitKyc_Customer_Success() throws Exception {
        when(kycService.saveKycRecordDTO(eq(1L), eq(1L), any(KycRequestDTO.class)))
                .thenReturn(kycResponse);

        mockMvc.perform(post("/kyc/v1/submit/{customerId}", 1L)
                .with(csrf())
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("KYC submitted successfully"));

        verify(kycService, times(1)).saveKycRecordDTO(eq(1L), eq(1L), any(KycRequestDTO.class));
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC2: Submit KYC - Customer trying to submit for different customer returns 403")
    void submitKyc_CustomerDifferentId_Returns403() throws Exception {
        mockMvc.perform(post("/kyc/v1/submit/{customerId}", 2L)
                .with(csrf())
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().isForbidden());

        verify(kycService, never()).saveKycRecordDTO(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC3: Submit KYC - Invalid doc number returns 400")
    void submitKyc_InvalidDocNumber_Returns400() throws Exception {
        KycRequestDTO invalidRequest = new KycRequestDTO();
        invalidRequest.setDocType("AADHAAR");
        invalidRequest.setDocNumber("invalid@#$%");
        invalidRequest.setDocumentImageUrl("https://example.com/aadhaar.jpg");

        mockMvc.perform(post("/kyc/v1/submit/{customerId}", 1L)
                .with(csrf())
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(kycService, never()).saveKycRecordDTO(anyLong(), anyLong(), any());
    }

    // =================== GET KYC TESTS ===================

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC4: Get KYC - Customer own success")
    void getKyc_CustomerOwn_Success() throws Exception {
        when(kycService.getKycRecordDTO(1L)).thenReturn(kycResponse);
        when(jwtService.extractUserId(anyString())).thenReturn(1L);
        when(jwtService.extractRoles(anyString())).thenReturn(List.of("ROLE_CUSTOMER"));

        mockMvc.perform(get("/kyc/v1/{customerId}", 1L)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerId").value(1L));
        
        verify(kycService, times(1)).getKycRecordDTO(1L);
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC5: Get KYC - Customer trying to view different customer returns 403")
    void getKyc_CustomerDifferentId_Returns403() throws Exception {
        when(jwtService.extractUserId(anyString())).thenReturn(1L);
        when(jwtService.extractRoles(anyString())).thenReturn(List.of("ROLE_CUSTOMER"));

        mockMvc.perform(get("/kyc/v1/{customerId}", 2L)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        
        verify(kycService, never()).getKycRecordDTO(anyLong());
    }

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC6: Get KYC - Admin can view any customer")
    void getKyc_Admin_CanViewAnyCustomer() throws Exception {
        when(kycService.getKycRecordDTO(1L)).thenReturn(kycResponse);
        when(jwtService.extractUserId(anyString())).thenReturn(999L);
        when(jwtService.extractRoles(anyString())).thenReturn(List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/kyc/v1/{customerId}", 1L)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.customerId").value(1L));
        
        verify(kycService, times(1)).getKycRecordDTO(1L);
    }

    // =================== GET KYC BY STATUS TESTS ===================

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC7: Get KYC by status - Admin success with data")
    void getKycByStatus_Admin_Success() throws Exception {
        Page<KycResponseDTO> page = new PageImpl<>(List.of(kycResponse));
        
        when(kycService.getByStatusPaginated(eq(KycVerificationStatus.PENDING), eq(0), eq(20)))
                .thenReturn(page);
        when(jwtService.extractUserId(anyString())).thenReturn(999L);
        when(jwtService.extractRoles(anyString())).thenReturn(List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/kyc/v1/status")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].kycVerificationStatus").value("PENDING"));
    }

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC8: Get KYC by status - Empty result")
    void getKycByStatus_Admin_EmptyResult() throws Exception {
        Page<KycResponseDTO> emptyPage = new PageImpl<>(List.of());
        
        when(kycService.getByStatusPaginated(eq(KycVerificationStatus.PENDING), eq(0), eq(20)))
                .thenReturn(emptyPage);
        when(jwtService.extractUserId(anyString())).thenReturn(999L);
        when(jwtService.extractRoles(anyString())).thenReturn(List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/kyc/v1/status")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC9: Get KYC by status - Customer not allowed returns 403")
    void getKycByStatus_Customer_Returns403() throws Exception {
        when(jwtService.extractUserId(anyString())).thenReturn(1L);
        when(jwtService.extractRoles(anyString())).thenReturn(List.of("ROLE_CUSTOMER"));

        mockMvc.perform(get("/kyc/v1/status")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());

        verify(kycService, never()).getByStatusPaginated(any(), anyInt(), anyInt());
    }

    // =================== UPDATE KYC STATUS TESTS ===================

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC10: Update KYC status - Admin approve success")
    void updateKycStatus_Admin_Approve_Success() throws Exception {
        when(kycService.updateKycStatusDTO(eq(1L), anyLong(), 
                eq(KycVerificationStatus.VERIFIED), isNull()))
                .thenReturn(kycResponse);
        when(jwtService.extractUserId(anyString())).thenReturn(999L);
        when(jwtService.extractRoles(anyString())).thenReturn(List.of("ROLE_ADMIN"));

        mockMvc.perform(put("/kyc/v1/status/{customerId}", 1L)
                        .with(csrf())
                        .param("status", "VERIFIED")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC11: Update KYC status - Admin reject with reason")
    void updateKycStatus_Admin_RejectWithReason_Success() throws Exception {
        when(kycService.updateKycStatusDTO(eq(1L), anyLong(), 
                eq(KycVerificationStatus.REJECTED), eq("Invalid document")))
                .thenReturn(kycResponse);
        when(jwtService.extractUserId(anyString())).thenReturn(999L);
        when(jwtService.extractRoles(anyString())).thenReturn(List.of("ROLE_ADMIN"));

        mockMvc.perform(put("/kyc/v1/status/{customerId}", 1L)
                        .with(csrf())
                        .param("status", "REJECTED")
                        .param("rejectionReason", "Invalid document")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC12: Update KYC status - Customer not allowed returns 403")
    void updateKycStatus_Customer_Returns403() throws Exception {
        when(jwtService.extractUserId(anyString())).thenReturn(1L);
        when(jwtService.extractRoles(anyString())).thenReturn(List.of("ROLE_CUSTOMER"));

        mockMvc.perform(put("/kyc/v1/status/{customerId}", 1L)
                        .with(csrf())
                        .param("status", "VERIFIED")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());

        verify(kycService, never()).updateKycStatusDTO(anyLong(), anyLong(), any(), any());
    }

    // =================== UNAUTHENTICATED TESTS ===================

    @Test
    @DisplayName("TC13: Get KYC - Unauthenticated returns 401")
    void getKyc_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/kyc/v1/{customerId}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC14: Get KYC by status - Unauthenticated returns 401")
    void getKycByStatus_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/kyc/v1/status")
                        .param("status", "PENDING"))
                .andExpect(status().isUnauthorized());
    }

    // =================== EXTRACT USER ID TESTS (COVERS YELLOW & RED LINES) ===================
 // Replace TC15, TC16, TC17 with these:

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC15: Extract userId - Missing Authorization header throws RuntimeException (with authentication)")
    void extractUserId_MissingAuthHeader_WithAuthentication() throws Exception {
        // Even with @WithMockCustomer, the controller still checks the Authorization header
        // So we don't add the header
        mockMvc.perform(post("/kyc/v1/submit/{customerId}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().is5xxServerError());
        
        verify(kycService, never()).saveKycRecordDTO(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC16: Extract userId - Authorization header without Bearer returns 401")
    void extractUserId_HeaderWithoutBearer_WithAuthentication() throws Exception {
        mockMvc.perform(post("/kyc/v1/submit/{customerId}", 1L)
                        .with(csrf())
                        .header("Authorization", "Basic token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().isUnauthorized()); // Change to 401
        
        verify(kycService, never()).saveKycRecordDTO(anyLong(), anyLong(), any());
    }
    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC17: Extract userId - Empty Authorization header throws RuntimeException")
    void extractUserId_EmptyAuthHeader_WithAuthentication() throws Exception {
        mockMvc.perform(post("/kyc/v1/submit/{customerId}", 1L)
                        .with(csrf())
                        .header("Authorization", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kycRequest)))
                .andExpect(status().is5xxServerError());
        
        verify(kycService, never()).saveKycRecordDTO(anyLong(), anyLong(), any());
    }
}