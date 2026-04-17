package in.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bank.dto.AccountOpeningRequestDTO;
import in.bank.dto.AccountOpeningResponseDTO;
import in.bank.entity.ActionType;
import in.bank.entity.RequestStatus;
import in.bank.exception.GlobalExceptionHandler;
import in.bank.exception.ResourceNotFoundException;
import in.bank.security.JwtService;
import in.bank.service.AccountOpeningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountOpeningController Tests")
class AccountOpeningControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountOpeningService service;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AccountOpeningController controller;

    private ObjectMapper objectMapper;
    private final String TOKEN = "Bearer test.jwt.token";
    private final String TOKEN_VALUE = "test.jwt.token";
    private final Long USER_ID = 1L;
    private final Long ADMIN_ID = 2L;

    @BeforeEach
    void setUp() {
        // Use standalone setup with exception handler
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    private AccountOpeningRequestDTO createValidRequest() {
        AccountOpeningRequestDTO dto = new AccountOpeningRequestDTO();
        dto.setProductId(1L);
        dto.setInitialDeposit(BigDecimal.valueOf(5000));
        return dto;
    }

    private AccountOpeningResponseDTO createResponseDTO(Long id, RequestStatus status) {
        return AccountOpeningResponseDTO.builder()
                .id(id)
                .productId(1L)
                .initialDeposit(BigDecimal.valueOf(5000))
                .status(status)
                .rejectionReason(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(USER_ID)
                .updatedBy(USER_ID)
                .build();
    }

    // ================= CREATE REQUEST TESTS =================

    @Test
    @DisplayName("TC1: Create Request - Success")
    void createRequest_Success() throws Exception {
        AccountOpeningRequestDTO requestDTO = createValidRequest();

        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(USER_ID);
        when(service.createRequest(eq(USER_ID), any(AccountOpeningRequestDTO.class)))
                .thenReturn(100L);

        mockMvc.perform(post("/api/v1/account-openings")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Account opening request created"))
                .andExpect(jsonPath("$.data").value(100L))
                .andExpect(jsonPath("$.code").value("REQ_201"));
    }

    @Test
    @DisplayName("TC2: Create Request - Invalid token")
    void createRequest_InvalidToken() throws Exception {
        AccountOpeningRequestDTO requestDTO = createValidRequest();

        when(jwtService.extractUserId(anyString())).thenThrow(new RuntimeException("Invalid token"));

        mockMvc.perform(post("/api/v1/account-openings")
                        .header("Authorization", "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().is4xxClientError());
    }

    // ================= GET MY REQUESTS TESTS =================

    @Test
    @DisplayName("TC3: Get My Requests - Success with data")
    void getMyRequests_Success() throws Exception {
        List<AccountOpeningResponseDTO> mockRequests = Arrays.asList(
                createResponseDTO(1L, RequestStatus.PENDING),
                createResponseDTO(2L, RequestStatus.APPROVED)
        );

        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(USER_ID);
        when(service.getMyRequests(USER_ID)).thenReturn(mockRequests);

        mockMvc.perform(get("/api/v1/account-openings/my")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Fetched successfully"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.code").value("REQ_200"));
    }

    @Test
    @DisplayName("TC4: Get My Requests - Empty list")
    void getMyRequests_EmptyList() throws Exception {
        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(USER_ID);
        when(service.getMyRequests(USER_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/account-openings/my")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ================= GET BY ID TESTS =================

    @Test
    @DisplayName("TC5: Get By ID - Customer owns request")
    void getById_CustomerOwnRequest_Success() throws Exception {
        AccountOpeningResponseDTO mockResponse = createResponseDTO(100L, RequestStatus.PENDING);

        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(USER_ID);
        when(jwtService.extractRoles(TOKEN_VALUE)).thenReturn(List.of("ROLE_CUSTOMER"));
        when(service.getById(eq(100L), eq(USER_ID), eq(false))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/account-openings/100")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    @DisplayName("TC6: Get By ID - Admin access")
    void getById_AdminAccess_Success() throws Exception {
        AccountOpeningResponseDTO mockResponse = createResponseDTO(100L, RequestStatus.PENDING);

        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(ADMIN_ID);
        when(jwtService.extractRoles(TOKEN_VALUE)).thenReturn(List.of("ROLE_ADMIN"));
        when(service.getById(eq(100L), eq(ADMIN_ID), eq(true))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/account-openings/100")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    @DisplayName("TC7: Get By ID - Not found")
    void getById_NotFound() throws Exception {
        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(USER_ID);
        when(jwtService.extractRoles(TOKEN_VALUE)).thenReturn(List.of("ROLE_CUSTOMER"));
        when(service.getById(eq(999L), eq(USER_ID), eq(false)))
                .thenThrow(new ResourceNotFoundException("Request not found")); // ✅ Use specific exception

        mockMvc.perform(get("/api/v1/account-openings/999")
                        .header("Authorization", TOKEN))
                .andExpect(status().isNotFound()); // ✅ Expect 404
    }

    // ================= GET ALL REQUESTS (ADMIN) TESTS =================

    @Test
    @DisplayName("TC8: Get All Requests - Admin with data")
    void getAllRequests_AdminWithData_Success() throws Exception {
        List<AccountOpeningResponseDTO> mockRequests = Arrays.asList(
                createResponseDTO(1L, RequestStatus.PENDING),
                createResponseDTO(2L, RequestStatus.PENDING)
        );

        when(service.getAllRequests(null)).thenReturn(mockRequests);

        mockMvc.perform(get("/api/v1/account-openings")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Fetched successfully"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.code").value("REQ_200"));
    }

    @Test
    @DisplayName("TC9: Get All Requests - Admin with status filter")
    void getAllRequests_AdminWithStatusFilter_Success() throws Exception {
        List<AccountOpeningResponseDTO> mockRequests = Arrays.asList(
                createResponseDTO(1L, RequestStatus.PENDING),
                createResponseDTO(3L, RequestStatus.PENDING)
        );

        when(service.getAllRequests(RequestStatus.PENDING)).thenReturn(mockRequests);

        mockMvc.perform(get("/api/v1/account-openings")
                        .header("Authorization", TOKEN)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("TC10: Get All Requests - Admin with empty result (covers dynamic message)")
    void getAllRequests_AdminEmptyResult() throws Exception {
        when(service.getAllRequests(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/account-openings")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("No account opening requests found"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ================= TAKE ACTION TESTS =================

    @Test
    @DisplayName("TC11: Take Action - Admin Approve (covers APPROVE switch case)")
    void takeAction_AdminApprove_Success() throws Exception {
        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(ADMIN_ID);
        doNothing().when(service).takeAction(eq(1L), eq(ADMIN_ID), eq(ActionType.APPROVE), isNull());

        mockMvc.perform(put("/api/v1/account-openings/1/action")
                        .header("Authorization", TOKEN)
                        .param("action", "APPROVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Account opening approved successfully"))
                .andExpect(jsonPath("$.code").value("REQ_200"));
    }

    @Test
    @DisplayName("TC12: Take Action - Admin Reject with reason (covers REJECT switch case)")
    void takeAction_AdminRejectWithReason_Success() throws Exception {
        String rejectionReason = "Incomplete KYC documents";
        
        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(ADMIN_ID);
        doNothing().when(service).takeAction(eq(1L), eq(ADMIN_ID), eq(ActionType.REJECT), eq(rejectionReason));

        mockMvc.perform(put("/api/v1/account-openings/1/action")
                        .header("Authorization", TOKEN)
                        .param("action", "REJECT")
                        .param("reason", rejectionReason))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Account opening rejected successfully"))
                .andExpect(jsonPath("$.code").value("REQ_200"));
    }

    @Test
    @DisplayName("TC13: Take Action - Admin Reject without reason (should still work)")
    void takeAction_AdminRejectWithoutReason_Success() throws Exception {
        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(ADMIN_ID);
        doNothing().when(service).takeAction(eq(1L), eq(ADMIN_ID), eq(ActionType.REJECT), isNull());

        mockMvc.perform(put("/api/v1/account-openings/1/action")
                        .header("Authorization", TOKEN)
                        .param("action", "REJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account opening rejected successfully"));
    }

    @Test
    @DisplayName("TC14: Take Action - Request not found")
    void takeAction_RequestNotFound() throws Exception {
        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(ADMIN_ID);
        doThrow(new ResourceNotFoundException("Request not found"))
                .when(service).takeAction(eq(999L), eq(ADMIN_ID), eq(ActionType.APPROVE), isNull());

        mockMvc.perform(put("/api/v1/account-openings/999/action")
                        .header("Authorization", TOKEN)
                        .param("action", "APPROVE"))
                .andExpect(status().isNotFound()); // Expect 404
    }
    // ================= EXTRACT TOKEN TESTS (Covering yellow/red lines) =================

    @Test
    @DisplayName("TC15: Extract Token - Missing Authorization header")
    void extractToken_MissingAuthHeader() throws Exception {
        AccountOpeningRequestDTO requestDTO = createValidRequest();

        mockMvc.perform(post("/api/v1/account-openings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC16: Extract Token - Authorization header without Bearer")
    void extractToken_HeaderWithoutBearer() throws Exception {
        AccountOpeningRequestDTO requestDTO = createValidRequest();

        mockMvc.perform(post("/api/v1/account-openings")
                        .header("Authorization", "Basic token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC17: Extract Token - Empty Authorization header")
    void extractToken_EmptyAuthHeader() throws Exception {
        AccountOpeningRequestDTO requestDTO = createValidRequest();

        mockMvc.perform(post("/api/v1/account-openings")
                        .header("Authorization", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("TC18: Extract Token - Null Authorization header")
    void extractToken_NullAuthHeader() throws Exception {
        AccountOpeningRequestDTO requestDTO = createValidRequest();

        mockMvc.perform(post("/api/v1/account-openings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().is4xxClientError());
    }

    // ================= IS ADMIN TESTS =================

    @Test
    @DisplayName("TC19: isAdmin - Returns true for ADMIN role")
    void isAdmin_WithAdminRole() throws Exception {
        AccountOpeningResponseDTO mockResponse = createResponseDTO(100L, RequestStatus.PENDING);

        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(USER_ID);
        when(jwtService.extractRoles(TOKEN_VALUE)).thenReturn(List.of("ROLE_ADMIN"));
        when(service.getById(eq(100L), eq(USER_ID), eq(true))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/account-openings/100")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC20: isAdmin - Returns false for CUSTOMER role")
    void isAdmin_WithCustomerRole() throws Exception {
        AccountOpeningResponseDTO mockResponse = createResponseDTO(100L, RequestStatus.PENDING);

        when(jwtService.extractUserId(TOKEN_VALUE)).thenReturn(USER_ID);
        when(jwtService.extractRoles(TOKEN_VALUE)).thenReturn(List.of("ROLE_CUSTOMER"));
        when(service.getById(eq(100L), eq(USER_ID), eq(false))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/account-openings/100")
                        .header("Authorization", TOKEN))
                .andExpect(status().isOk());
    }
}