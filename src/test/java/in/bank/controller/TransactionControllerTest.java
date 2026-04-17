package in.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bank.config.JwtFilter;
import in.bank.config.SecurityConfig;
import in.bank.dto.*;
import in.bank.entity.Currency;
import in.bank.entity.PaymentMode;
import in.bank.entity.TransactionType;
import in.bank.exception.GlobalExceptionHandler;
import in.bank.exception.InsufficientBalanceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

@WebMvcTest(controllers = TransactionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("TransactionController Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private TransactionService transactionService;

    private TransactionRequestDTO validDepositRequest;
    private TransactionRequestDTO validWithdrawalRequest;

    @BeforeEach
    void setUp() {
        // ✅ FIX: Mock the filter to pass through the chain
        try {
            doAnswer(invocation -> {
                jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                chain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
                );
                return null;
            }).when(jwtFilter).doFilter(any(), any(), any());
        } catch (Exception e) {
            // This catch block handles IOException and ServletException
            throw new RuntimeException("Failed to mock JwtFilter", e);
        }

        validDepositRequest = new TransactionRequestDTO();
        validDepositRequest.setTransactionType(TransactionType.DEPOSIT);
        validDepositRequest.setAccountId(1L);
        validDepositRequest.setAmount(BigDecimal.valueOf(5000));
        validDepositRequest.setCurrency(Currency.INR);
        validDepositRequest.setPaymentMode(PaymentMode.UPI);
        validDepositRequest.setDescription("Initial deposit");

        validWithdrawalRequest = new TransactionRequestDTO();
        validWithdrawalRequest.setTransactionType(TransactionType.WITHDRAWAL);
        validWithdrawalRequest.setAccountId(1L);
        validWithdrawalRequest.setAmount(BigDecimal.valueOf(1000));
        validWithdrawalRequest.setCurrency(Currency.INR);
        validWithdrawalRequest.setPaymentMode(PaymentMode.UPI);
        validWithdrawalRequest.setDescription("ATM withdrawal");
    }

    // ================= CREATE TRANSACTION TESTS =================

    @Test
    @DisplayName("TC1: Admin can create a deposit transaction")
    @WithMockUser(roles = "ADMIN")
    void createTransaction_asAdmin_returns200() throws Exception {
        TransactionCreateResponseDTO response = TransactionCreateResponseDTO.builder()
            .transactionId(42L)
            .message("Transaction processed successfully.")
            .build();

        when(transactionService.processTransaction(anyString(), any(), any()))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions")
                .with(csrf())
                .header("Idempotency-Key", "unique-key-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDepositRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").value(42));
    }

    @Test
    @DisplayName("TC2: Customer can create a transaction")
    @WithMockUser(roles = "CUSTOMER")
    void createTransaction_asCustomer_returns200() throws Exception {
        TransactionCreateResponseDTO response = TransactionCreateResponseDTO.builder()
            .transactionId(43L)
            .message("Transaction processed successfully.")
            .build();

        when(transactionService.processTransaction(anyString(), any(), any()))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions")
                .with(csrf())
                .header("Idempotency-Key", "unique-key-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDepositRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").value(43));
    }

    @Test
    @DisplayName("TC3: Create transaction with invalid request body returns 400")
    @WithMockUser(roles = "ADMIN")
    void createTransaction_missingFields_returns400() throws Exception {
        TransactionRequestDTO bad = new TransactionRequestDTO();
        bad.setTransactionType(TransactionType.DEPOSIT);
        bad.setAccountId(1L);

        mockMvc.perform(post("/api/v1/transactions")
                .with(csrf())
                .header("Idempotency-Key", "unique-key-003")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC4: Create withdrawal transaction with insufficient balance")
    @WithMockUser(roles = "CUSTOMER")
    void createTransaction_insufficientBalance_returnsError() throws Exception {
        when(transactionService.processTransaction(anyString(), any(), any()))
            .thenThrow(new InsufficientBalanceException("Insufficient balance in account"));

        mockMvc.perform(post("/api/v1/transactions")
                .with(csrf())
                .header("Idempotency-Key", "unique-key-004")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validWithdrawalRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC5: Create transaction with account not found")
    @WithMockUser(roles = "CUSTOMER")
    void createTransaction_accountNotFound_returnsError() throws Exception {
        when(transactionService.processTransaction(anyString(), any(), any()))
            .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(post("/api/v1/transactions")
                .with(csrf())
                .header("Idempotency-Key", "unique-key-005")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDepositRequest)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC6: Unauthenticated request to create transaction returns 401 or 403")
    void createTransaction_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .with(csrf())
                .header("Idempotency-Key", "unique-key-006")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDepositRequest)))
                .andExpect(status().is4xxClientError()); // Accept any 4xx error (401 or 403)
    }

    // ================= GET TRANSACTIONS TESTS =================

    @Test
    @DisplayName("TC7: Admin fetches all transactions for an account")
    @WithMockUser(roles = "ADMIN")
    void getTransactions_allForAccount_returns200() throws Exception {
        TransactionResponseDTO t = buildSampleResponse(1L, TransactionType.DEPOSIT, BigDecimal.valueOf(5000));
        when(transactionService.getAllTransactionsDTO(1L)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/v1/transactions/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].transactionId").value(1));
    }

    @Test
    @DisplayName("TC8: Admin fetches single transaction by ID")
    @WithMockUser(roles = "ADMIN")
    void getTransactions_singleById_returns200() throws Exception {
        TransactionResponseDTO t = buildSampleResponse(99L, TransactionType.WITHDRAWAL, BigDecimal.valueOf(1000));
        when(transactionService.getTransactionById(1L, 99L)).thenReturn(t);

        mockMvc.perform(get("/api/v1/transactions/1")
                .param("transactionId", "99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").value(99));
    }

    @Test
    @DisplayName("TC9: Admin fetches single transaction - not found")
    @WithMockUser(roles = "ADMIN")
    void getTransactions_singleById_notFound() throws Exception {
        when(transactionService.getTransactionById(1L, 999L))
            .thenThrow(new ResourceNotFoundException("Transaction not found"));

        mockMvc.perform(get("/api/v1/transactions/1")
                .param("transactionId", "999"))
            .andExpect(status().isNotFound());
    }

    // ================= ACCESS DENIED TESTS =================

    @Test
    @DisplayName("TC19: Customer trying to access admin endpoint gets forbidden")
    @WithMockUser(roles = "CUSTOMER")
    void getTransactions_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC20: Customer trying to filter transactions gets forbidden")
    @WithMockUser(roles = "CUSTOMER")
    void filterTransactions_asCustomer_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/1/filter")
                .param("type", "DEPOSIT"))
            .andExpect(status().isForbidden());
    }

    // ================= HELPER METHOD =================

    private TransactionResponseDTO buildSampleResponse(Long id, TransactionType type, BigDecimal amount) {
        return TransactionResponseDTO.builder()
            .transactionId(id)
            .type(type)
            .amount(amount)
            .balanceBeforeTransaction(BigDecimal.valueOf(10000))
            .balanceAfterTransaction(BigDecimal.valueOf(10000).add(amount))
            .currency(Currency.INR)
            .paymentMode(PaymentMode.UPI)
            .description("Test transaction")
            .interestPosting(BigDecimal.ZERO)
            .transactionDate(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();
    }
}