package in.bank.service;

import in.bank.dto.*;
import in.bank.entity.*;
import in.bank.exception.AccessDeniedException;
import in.bank.exception.AccountFrozenException;
import in.bank.exception.BadRequestException;
import in.bank.exception.InsufficientBalanceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.IdempotencyKeyRepository;
import in.bank.repository.SavingsAccountRepository;
import in.bank.repository.SavingsAccountTransactionRepository;
import in.bank.security.CustomUserDetails;
import in.bank.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionServiceImpl Tests")
class TransactionServiceImplTest {

    @Mock private SavingsAccountRepository accountRepository;
    @Mock private SavingsAccountTransactionRepository transactionRepository;
    @Mock private IdempotencyKeyRepository idempotencyRepo;
    @Mock private JwtService jwtService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private AppUser fullKycUser;
    private AppUser minKycUser;
    private SavingsAccount activeAccount;
    private TransactionRequestDTO depositRequest;
    private TransactionRequestDTO withdrawalRequest;
    private CustomUserDetails adminDetails;
    private CustomUserDetails customerDetails;

    @BeforeEach
    void setUp() {
        fullKycUser = new AppUser();
        fullKycUser.setId(2L);
        fullKycUser.setKycStatus(KycStatus.FULL_KYC);

        minKycUser = new AppUser();
        minKycUser.setId(3L);
        minKycUser.setKycStatus(KycStatus.MIN_KYC);

        activeAccount = new SavingsAccount();
        activeAccount.setId(10L);
        activeAccount.setUser(fullKycUser);
        activeAccount.setCurrentBalanceAmount(BigDecimal.valueOf(20000));
        activeAccount.setAccountStatus(AccountLifecycleStatus.ACTIVE);

        depositRequest = new TransactionRequestDTO();
        depositRequest.setTransactionType(TransactionType.DEPOSIT);
        depositRequest.setAccountId(10L);
        depositRequest.setAmount(BigDecimal.valueOf(5000));
        depositRequest.setCurrency(Currency.INR);
        depositRequest.setPaymentMode(PaymentMode.UPI);
        depositRequest.setDescription("Test deposit");

        withdrawalRequest = new TransactionRequestDTO();
        withdrawalRequest.setTransactionType(TransactionType.WITHDRAWAL);
        withdrawalRequest.setAccountId(10L);
        withdrawalRequest.setAmount(BigDecimal.valueOf(5000));
        withdrawalRequest.setCurrency(Currency.INR);
        withdrawalRequest.setPaymentMode(PaymentMode.UPI);
        withdrawalRequest.setDescription("Test withdrawal");

        adminDetails = new CustomUserDetails(
            1L, "admin@bank.com", "secret",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        customerDetails = new CustomUserDetails(
            2L, "customer@bank.com", "pass",
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    @Nested
    @DisplayName("processTransaction() Tests")
    class ProcessTransactionTests {

        @Test
        @DisplayName("TC1: Deposit transaction increases balance")
        void processTransaction_deposit_increasesBalance() {
            when(idempotencyRepo.findById("key-1")).thenReturn(Optional.empty());
            when(accountRepository.findById(10L)).thenReturn(Optional.of(activeAccount));
            when(transactionRepository.save(any())).thenAnswer(inv -> {
                SavingsAccountTransaction t = inv.getArgument(0);
                t.setId(100L);
                return t;
            });
            when(idempotencyRepo.save(any(IdempotencyKey.class))).thenReturn(new IdempotencyKey());

            TransactionCreateResponseDTO result =
                transactionService.processTransaction("key-1", depositRequest, adminDetails);

            assertThat(result.getTransactionId()).isEqualTo(100L);
            assertThat(result.getMessage()).contains("successfully");
            assertThat(activeAccount.getCurrentBalanceAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(25000));
        }

        @Test
        @DisplayName("TC2: Withdrawal transaction decreases balance")
        void processTransaction_withdrawal_decreasesBalance() {
            when(idempotencyRepo.findById("key-2")).thenReturn(Optional.empty());
            when(accountRepository.findById(10L)).thenReturn(Optional.of(activeAccount));
            when(transactionRepository.save(any())).thenAnswer(inv -> {
                SavingsAccountTransaction t = inv.getArgument(0);
                t.setId(101L);
                return t;
            });
            when(idempotencyRepo.save(any(IdempotencyKey.class))).thenReturn(new IdempotencyKey());

            transactionService.processTransaction("key-2", withdrawalRequest, adminDetails);

            assertThat(activeAccount.getCurrentBalanceAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(15000));
        }

        @Test
        @DisplayName("TC3: Null idempotency key throws exception")
        void processTransaction_NullIdempotencyKey_ThrowsException() {
            assertThatThrownBy(() ->
                transactionService.processTransaction(null, depositRequest, adminDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency-Key header is required");
        }

        @Test
        @DisplayName("TC4: Blank idempotency key throws exception")
        void processTransaction_BlankIdempotencyKey_ThrowsException() {
            assertThatThrownBy(() ->
                transactionService.processTransaction("", depositRequest, adminDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idempotency-Key header is required");
        }

        @Test
        @DisplayName("TC5: Duplicate idempotency key returns cached result")
        void processTransaction_DuplicateIdempotencyKey_ReturnsCachedResult() {
            IdempotencyKey existing = new IdempotencyKey();
            existing.setKey("dup-key");
            existing.setResponse("55");

            when(idempotencyRepo.findById("dup-key")).thenReturn(Optional.of(existing));

            TransactionCreateResponseDTO result =
                transactionService.processTransaction("dup-key", depositRequest, adminDetails);

            assertThat(result.getTransactionId()).isEqualTo(55L);
            assertThat(result.getMessage()).contains("Duplicate");
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC6: Invalid stored idempotency response throws exception")
        void processTransaction_InvalidStoredResponse_ThrowsException() {
            IdempotencyKey existing = new IdempotencyKey();
            existing.setKey("invalid-key");
            existing.setResponse("invalid-number");

            when(idempotencyRepo.findById("invalid-key")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() ->
                transactionService.processTransaction("invalid-key", depositRequest, adminDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid stored idempotency response");
        }

        @Test
        @DisplayName("TC7: Manual INTEREST transaction throws exception")
        void processTransaction_InterestType_ThrowsIllegalArgument() {
            depositRequest.setTransactionType(TransactionType.INTEREST);
            when(idempotencyRepo.findById("key-8")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                transactionService.processTransaction("key-8", depositRequest, adminDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("system-generated");
        }

        @Test
        @DisplayName("TC8: Withdrawal exceeding balance throws InsufficientBalanceException")
        void processTransaction_withdrawal_insufficientBalance_throwsException() {
            withdrawalRequest.setAmount(BigDecimal.valueOf(99999));

            when(idempotencyRepo.findById("key-3")).thenReturn(Optional.empty());
            when(accountRepository.findById(10L)).thenReturn(Optional.of(activeAccount));

            assertThatThrownBy(() ->
                transactionService.processTransaction("key-3", withdrawalRequest, adminDetails))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
        }

        @Test
        @DisplayName("TC9: Transaction on frozen account throws AccountFrozenException")
        void processTransaction_frozenAccount_throwsException() {
            activeAccount.setAccountStatus(AccountLifecycleStatus.FROZEN);

            when(idempotencyRepo.findById("key-4")).thenReturn(Optional.empty());
            when(accountRepository.findById(10L)).thenReturn(Optional.of(activeAccount));

            assertThatThrownBy(() ->
                transactionService.processTransaction("key-4", depositRequest, adminDetails))
                .isInstanceOf(AccountFrozenException.class)
                .hasMessageContaining("frozen");
        }

        @Test
        @DisplayName("TC10: Customer accessing another user's account throws AccessDeniedException - covers yellow line")
        void processTransaction_customerOnOtherAccount_throwsAccessDenied() {
            AppUser otherUser = new AppUser();
            otherUser.setId(99L);
            activeAccount.setUser(otherUser);

            when(idempotencyRepo.findById("key-6")).thenReturn(Optional.empty());
            when(accountRepository.findById(10L)).thenReturn(Optional.of(activeAccount));

            assertThatThrownBy(() ->
                transactionService.processTransaction("key-6", depositRequest, customerDetails))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("TC11: Account not found throws ResourceNotFoundException")
        void processTransaction_accountNotFound_throwsException() {
            when(idempotencyRepo.findById("key-7")).thenReturn(Optional.empty());
            when(accountRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                transactionService.processTransaction("key-7", depositRequest, adminDetails))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
        }

        @Test
        @DisplayName("TC12: Idempotency save fails and retry finds existing - covers yellow/red lines")
        void processTransaction_IdempotencySaveFails_RetrySucceeds() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(activeAccount));
            when(transactionRepository.save(any(SavingsAccountTransaction.class))).thenAnswer(inv -> {
                SavingsAccountTransaction t = inv.getArgument(0);
                t.setId(200L);
                return t;
            });
            
            IdempotencyKey retryKey = new IdempotencyKey();
            retryKey.setKey("retry-key");
            retryKey.setResponse("200");
            
            when(idempotencyRepo.findById("retry-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(retryKey));
            
            doThrow(new RuntimeException("DB error")).when(idempotencyRepo).save(any(IdempotencyKey.class));

            TransactionCreateResponseDTO result = 
                transactionService.processTransaction("retry-key", depositRequest, adminDetails);

            assertThat(result.getTransactionId()).isEqualTo(200L);
            assertThat(result.getMessage()).contains("Duplicate");
            
            verify(accountRepository, times(1)).findById(10L);
            verify(transactionRepository, times(1)).save(any(SavingsAccountTransaction.class));
        }

        @Test
        @DisplayName("TC13: Idempotency save fails and retry fails - throws original exception")
        void processTransaction_IdempotencySaveFails_RetryFails_ThrowsException() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(activeAccount));
            when(transactionRepository.save(any(SavingsAccountTransaction.class))).thenAnswer(inv -> {
                SavingsAccountTransaction t = inv.getArgument(0);
                t.setId(200L);
                return t;
            });
            
            when(idempotencyRepo.findById("retry-key")).thenReturn(Optional.empty());
            doThrow(new RuntimeException("DB error")).when(idempotencyRepo).save(any(IdempotencyKey.class));
            when(idempotencyRepo.findById("retry-key")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> 
                transactionService.processTransaction("retry-key", depositRequest, adminDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");
        }
    }

    @Nested
    @DisplayName("validateTransaction() Tests")
    class ValidateTransactionTests {

        @Test
        @DisplayName("TC14: MIN_KYC user - transaction within limit succeeds")
        void validateTransaction_MinKyc_WithinLimit_Success() {
            SavingsAccount minKycAccount = new SavingsAccount();
            minKycAccount.setId(20L);
            minKycAccount.setUser(minKycUser);
            minKycAccount.setCurrentBalanceAmount(BigDecimal.valueOf(100000));
            minKycAccount.setAccountStatus(AccountLifecycleStatus.ACTIVE);

            TransactionRequestDTO request = new TransactionRequestDTO();
            request.setTransactionType(TransactionType.DEPOSIT);
            request.setAccountId(20L);
            request.setAmount(BigDecimal.valueOf(40000));
            request.setCurrency(Currency.INR);
            request.setPaymentMode(PaymentMode.UPI);
            request.setDescription("Test deposit");

            when(idempotencyRepo.findById("key-min")).thenReturn(Optional.empty());
            when(accountRepository.findById(20L)).thenReturn(Optional.of(minKycAccount));
            when(transactionRepository.save(any())).thenAnswer(inv -> {
                SavingsAccountTransaction t = inv.getArgument(0);
                t.setId(300L);
                return t;
            });
            when(idempotencyRepo.save(any(IdempotencyKey.class))).thenReturn(new IdempotencyKey());

            TransactionCreateResponseDTO result = 
                transactionService.processTransaction("key-min", request, adminDetails);

            assertThat(result.getTransactionId()).isEqualTo(300L);
        }

        @Test
        @DisplayName("TC15: MIN_KYC user - transaction exceeds limit throws exception - covers yellow/red lines")
        void validateTransaction_MinKyc_ExceedsLimit_ThrowsException() {
            SavingsAccount minKycAccount = new SavingsAccount();
            minKycAccount.setId(21L);
            minKycAccount.setUser(minKycUser);
            minKycAccount.setCurrentBalanceAmount(BigDecimal.valueOf(100000));
            minKycAccount.setAccountStatus(AccountLifecycleStatus.ACTIVE);

            TransactionRequestDTO request = new TransactionRequestDTO();
            request.setTransactionType(TransactionType.DEPOSIT);
            request.setAccountId(21L);
            request.setAmount(BigDecimal.valueOf(60000));
            request.setCurrency(Currency.INR);
            request.setPaymentMode(PaymentMode.UPI);
            request.setDescription("Test deposit");

            when(idempotencyRepo.findById("key-min-exceed")).thenReturn(Optional.empty());
            when(accountRepository.findById(21L)).thenReturn(Optional.of(minKycAccount));

            assertThatThrownBy(() ->
                transactionService.processTransaction("key-min-exceed", request, adminDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transaction exceeds limit for MIN_KYC");
        }

        @Test
        @DisplayName("TC16: FULL_KYC user - transaction within limit succeeds")
        void validateTransaction_FullKyc_WithinLimit_Success() {
            TransactionRequestDTO request = new TransactionRequestDTO();
            request.setTransactionType(TransactionType.DEPOSIT);
            request.setAccountId(10L);
            request.setAmount(BigDecimal.valueOf(500000));
            request.setCurrency(Currency.INR);
            request.setPaymentMode(PaymentMode.UPI);
            request.setDescription("Test deposit");

            when(idempotencyRepo.findById("key-full")).thenReturn(Optional.empty());
            when(accountRepository.findById(10L)).thenReturn(Optional.of(activeAccount));
            when(transactionRepository.save(any())).thenAnswer(inv -> {
                SavingsAccountTransaction t = inv.getArgument(0);
                t.setId(400L);
                return t;
            });
            when(idempotencyRepo.save(any(IdempotencyKey.class))).thenReturn(new IdempotencyKey());

            TransactionCreateResponseDTO result = 
                transactionService.processTransaction("key-full", request, adminDetails);

            assertThat(result.getTransactionId()).isEqualTo(400L);
        }

        @Test
        @DisplayName("TC17: FULL_KYC user - transaction exceeds limit throws exception - covers yellow/red lines")
        void validateTransaction_FullKyc_ExceedsLimit_ThrowsException() {
            TransactionRequestDTO request = new TransactionRequestDTO();
            request.setTransactionType(TransactionType.DEPOSIT);
            request.setAccountId(10L);
            request.setAmount(BigDecimal.valueOf(1500000));
            request.setCurrency(Currency.INR);
            request.setPaymentMode(PaymentMode.UPI);
            request.setDescription("Test deposit");

            when(idempotencyRepo.findById("key-full-exceed")).thenReturn(Optional.empty());
            when(accountRepository.findById(10L)).thenReturn(Optional.of(activeAccount));

            assertThatThrownBy(() ->
                transactionService.processTransaction("key-full-exceed", request, adminDetails))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Transaction exceeds limit for FULL_KYC");
        }

        @Test
        @DisplayName("TC18: User with null KYC status throws exception - covers yellow line")
        void validateTransaction_NullKycStatus_ThrowsException() {
            AppUser noKycUser = new AppUser();
            noKycUser.setId(4L);
            noKycUser.setKycStatus(null);

            SavingsAccount noKycAccount = new SavingsAccount();
            noKycAccount.setId(30L);
            noKycAccount.setUser(noKycUser);
            noKycAccount.setCurrentBalanceAmount(BigDecimal.valueOf(100000));
            noKycAccount.setAccountStatus(AccountLifecycleStatus.ACTIVE);

            TransactionRequestDTO request = new TransactionRequestDTO();
            request.setTransactionType(TransactionType.DEPOSIT);
            request.setAccountId(30L);
            request.setAmount(BigDecimal.valueOf(1000));
            request.setCurrency(Currency.INR);
            request.setPaymentMode(PaymentMode.UPI);
            request.setDescription("Test deposit");

            when(idempotencyRepo.findById("key-null")).thenReturn(Optional.empty());
            when(accountRepository.findById(30L)).thenReturn(Optional.of(noKycAccount));

            assertThatThrownBy(() ->
                transactionService.processTransaction("key-null", request, adminDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("KYC status not available");
        }
    }

    @Nested
    @DisplayName("getAllTransactionsDTO() Tests")
    class GetAllTransactionsDTOTests {

        @Test
        @DisplayName("TC19: Get all transactions for account")
        void getAllTransactionsDTO_Success() {
            SavingsAccountTransaction txn = buildTransaction(1L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(1000), activeAccount);
            when(transactionRepository.findBySavingsAccount_Id(10L)).thenReturn(List.of(txn));

            List<TransactionResponseDTO> result = transactionService.getAllTransactionsDTO(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransactionId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("TC20: Get all transactions - empty list")
        void getAllTransactionsDTO_EmptyList() {
            when(transactionRepository.findBySavingsAccount_Id(10L)).thenReturn(List.of());

            List<TransactionResponseDTO> result = transactionService.getAllTransactionsDTO(10L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTransactionById() Tests")
    class GetTransactionByIdTests {

        @Test
        @DisplayName("TC21: Get transaction by ID - success")
        void getTransactionById_Success() {
            SavingsAccountTransaction txn = buildTransaction(1L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(1000), activeAccount);
            when(transactionRepository.findByIdAndSavingsAccount_Id(1L, 10L))
                .thenReturn(Optional.of(txn));

            TransactionResponseDTO result = transactionService.getTransactionById(10L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getTransactionId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("TC22: Get transaction by ID - not found throws exception")
        void getTransactionById_NotFound_ThrowsException() {
            when(transactionRepository.findByIdAndSavingsAccount_Id(999L, 10L))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                transactionService.getTransactionById(10L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");
        }
    }

    @Nested
    @DisplayName("getTransactionsForLoggedInUser() Tests")
    class GetTransactionsForLoggedInUserTests {

        @Test
        @DisplayName("TC23: Get transactions for logged in user - aggregates across accounts")
        void getTransactionsForLoggedInUser_ReturnsAggregatedTransactions() {
            SavingsAccount acc1 = new SavingsAccount();
            acc1.setId(10L);
            SavingsAccount acc2 = new SavingsAccount();
            acc2.setId(11L);

            SavingsAccountTransaction t1 = buildTransaction(1L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(1000), acc1);
            SavingsAccountTransaction t2 = buildTransaction(2L, TransactionType.WITHDRAWAL,
                BigDecimal.valueOf(500), acc2);

            when(jwtService.getLoggedInUserId()).thenReturn(2L);
            when(accountRepository.findByUser_Id(2L)).thenReturn(List.of(acc1, acc2));
            when(transactionRepository.findBySavingsAccount_Id(10L)).thenReturn(List.of(t1));
            when(transactionRepository.findBySavingsAccount_Id(11L)).thenReturn(List.of(t2));

            List<TransactionResponseDTO> results = transactionService.getTransactionsForLoggedInUser();

            assertThat(results).hasSize(2);
            assertThat(results).extracting(TransactionResponseDTO::getTransactionId)
                .containsExactlyInAnyOrder(1L, 2L);
        }
    }

    @Nested
    @DisplayName("getTransactionSummary() Tests")
    class GetTransactionSummaryTests {

        @Test
        @DisplayName("TC24: Get transaction summary - success")
        void getTransactionSummary_Success() {
            when(accountRepository.findById(10L)).thenReturn(Optional.of(activeAccount));

            TransactionSummaryDTO result = transactionService.getTransactionSummary(10L);

            assertThat(result).isNotNull();
            assertThat(result.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        }

        @Test
        @DisplayName("TC25: Get transaction summary - account not found")
        void getTransactionSummary_AccountNotFound_ThrowsException() {
            when(accountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                transactionService.getTransactionSummary(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
        }
    }

    @Nested
    @DisplayName("filterTransactions() Tests")
    class FilterTransactionsTests {

        @Test
        @DisplayName("TC26: Filter transactions by type")
        void filterTransactions_ByType() {
            SavingsAccountTransaction t1 = buildTransaction(1L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(1000), activeAccount);
            SavingsAccountTransaction t2 = buildTransaction(2L, TransactionType.WITHDRAWAL,
                BigDecimal.valueOf(500), activeAccount);

            when(transactionRepository.findBySavingsAccount_Id(10L))
                .thenReturn(List.of(t1, t2));

            List<TransactionResponseDTO> result = 
                transactionService.filterTransactions(10L, TransactionType.DEPOSIT, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
        }

        @Test
        @DisplayName("TC27: Filter transactions by start date")
        void filterTransactions_ByStartDate() {
            LocalDateTime now = LocalDateTime.now();
            SavingsAccountTransaction t1 = buildTransactionWithDate(1L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(1000), activeAccount, now.minusDays(5));
            SavingsAccountTransaction t2 = buildTransactionWithDate(2L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(500), activeAccount, now.minusDays(1));

            when(transactionRepository.findBySavingsAccount_Id(10L))
                .thenReturn(List.of(t1, t2));

            List<TransactionResponseDTO> result = 
                transactionService.filterTransactions(10L, null, now.minusDays(3), null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransactionId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("TC28: Filter transactions by end date - covers yellow line")
        void filterTransactions_ByEndDate() {
            LocalDateTime now = LocalDateTime.now();
            SavingsAccountTransaction t1 = buildTransactionWithDate(1L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(1000), activeAccount, now.minusDays(5));
            SavingsAccountTransaction t2 = buildTransactionWithDate(2L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(500), activeAccount, now.minusDays(1));

            when(transactionRepository.findBySavingsAccount_Id(10L))
                .thenReturn(List.of(t1, t2));

            List<TransactionResponseDTO> result = 
                transactionService.filterTransactions(10L, null, null, now.minusDays(2));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransactionId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("TC29: Filter transactions by both start and end date")
        void filterTransactions_ByBothDates() {
            LocalDateTime now = LocalDateTime.now();
            SavingsAccountTransaction t1 = buildTransactionWithDate(1L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(1000), activeAccount, now.minusDays(5));
            SavingsAccountTransaction t2 = buildTransactionWithDate(2L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(500), activeAccount, now.minusDays(3));

            when(transactionRepository.findBySavingsAccount_Id(10L))
                .thenReturn(List.of(t1, t2));

            List<TransactionResponseDTO> result = 
                transactionService.filterTransactions(10L, null, now.minusDays(4), now.minusDays(2));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTransactionId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("TC30: Filter transactions - no filters returns all")
        void filterTransactions_NoFilters_ReturnsAll() {
            SavingsAccountTransaction t1 = buildTransaction(1L, TransactionType.DEPOSIT,
                BigDecimal.valueOf(1000), activeAccount);
            SavingsAccountTransaction t2 = buildTransaction(2L, TransactionType.WITHDRAWAL,
                BigDecimal.valueOf(500), activeAccount);

            when(transactionRepository.findBySavingsAccount_Id(10L))
                .thenReturn(List.of(t1, t2));

            List<TransactionResponseDTO> result = 
                transactionService.filterTransactions(10L, null, null, null);

            assertThat(result).hasSize(2);
        }
    }

    // ================= HELPERS =================

    private SavingsAccountTransaction buildTransaction(Long id, TransactionType type,
                                                        BigDecimal amount, SavingsAccount account) {
        SavingsAccountTransaction t = new SavingsAccountTransaction();
        t.setId(id);
        t.setType(type);
        t.setAmount(amount);
        t.setSavingsAccount(account);
        t.setCurrency(Currency.INR);
        t.setPaymentMode(PaymentMode.UPI);
        t.setBalanceBeforeTransaction(BigDecimal.valueOf(10000));
        t.setBalanceAfterTransaction(BigDecimal.valueOf(10000).add(amount));
        t.setInterestPosting(BigDecimal.ZERO);
        t.setTransactionDate(LocalDateTime.now());
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    private SavingsAccountTransaction buildTransactionWithDate(Long id, TransactionType type,
                                                                BigDecimal amount, SavingsAccount account,
                                                                LocalDateTime date) {
        SavingsAccountTransaction t = buildTransaction(id, type, amount, account);
        t.setTransactionDate(date);
        t.setCreatedAt(date);
        return t;
    }
}