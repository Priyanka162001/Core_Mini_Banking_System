package in.bank.service;

import in.bank.dto.InterestPostingResponseDTO;
import in.bank.entity.*;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.InterestPostingRepository;
import in.bank.repository.SavingsAccountRepository;
import in.bank.repository.SavingsAccountTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Interest Posting Service Tests")
class InterestPostingServiceImplTest {

    @Mock
    private SavingsAccountRepository accountRepository;

    @Mock
    private InterestPostingRepository postingRepository;

    @Mock
    private SavingsAccountTransactionRepository transactionRepository;

    @Mock
    private InterestCalculationService calculationService;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private InterestPostingServiceImpl selfProxy;

    @InjectMocks
    private InterestPostingServiceImpl interestPostingService;

    private SavingsAccount sampleAccount;
    private SavingsProduct sampleProduct;
    private InterestPostingRecord samplePostingRecord;
    private InterestPostingRecord failedPostingRecord;

    @BeforeEach
    void setUp() {
        sampleProduct = SavingsProduct.builder()
                .id(1L)
                .productName("Regular Savings")
                .interestRatePercent(new BigDecimal("4.5"))
                .build();

        sampleAccount = SavingsAccount.builder()
                .id(1L)
                .accountNumber("SAV100001")
                .currentBalanceAmount(new BigDecimal("10000.00"))
                .accountStatus(AccountLifecycleStatus.ACTIVE)
                .savingsProduct(sampleProduct)
                .build();

        samplePostingRecord = InterestPostingRecord.builder()
                .id(1L)
                .savingsAccount(sampleAccount)
                .interestAmount(new BigDecimal("37.50"))
                .balanceBefore(new BigDecimal("10000.00"))
                .balanceAfter(new BigDecimal("10037.50"))
                .annualInterestRate(new BigDecimal("4.5"))
                .postingMonth(1)
                .postingYear(2024)
                .status("SUCCESS")
                .createdAt(LocalDateTime.now())
                .createdBy(1L)
                .build();

        failedPostingRecord = InterestPostingRecord.builder()
                .id(2L)
                .savingsAccount(sampleAccount)
                .postingMonth(1)
                .postingYear(2024)
                .status("FAILED")
                .interestAmount(BigDecimal.ZERO)
                .balanceBefore(new BigDecimal("10000.00"))
                .balanceAfter(new BigDecimal("10000.00"))
                .annualInterestRate(new BigDecimal("4.5"))
                .build();
    }

    @Nested
    @DisplayName("postInterestForPeriod() Tests")
    class PostInterestForPeriodTests {

        @Test
        @DisplayName("TC1: Successfully post interest for all active accounts")
        void testPostInterestForPeriod_AllSuccess() {
            int month = 1;
            int year = 2024;
            List<SavingsAccount> activeAccounts = Arrays.asList(sampleAccount, 
                    SavingsAccount.builder()
                            .id(2L)
                            .currentBalanceAmount(new BigDecimal("20000.00"))
                            .accountStatus(AccountLifecycleStatus.ACTIVE)
                            .savingsProduct(sampleProduct)
                            .build());
            
            when(accountRepository.findByAccountStatus(AccountLifecycleStatus.ACTIVE))
                    .thenReturn(activeAccounts);
            when(applicationContext.getBean(InterestPostingServiceImpl.class))
                    .thenReturn(selfProxy);
            when(selfProxy.postInterestForAccount(1L, month, year)).thenReturn(true);
            when(selfProxy.postInterestForAccount(2L, month, year)).thenReturn(true);

            InterestPostingService.JobSummary result = interestPostingService.postInterestForPeriod(month, year);

            assertThat(result).isNotNull();
            assertThat(result.month()).isEqualTo(month);
            assertThat(result.year()).isEqualTo(year);
            assertThat(result.totalAccounts()).isEqualTo(2);
            assertThat(result.posted()).isEqualTo(2);
            assertThat(result.skipped()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC2: Some accounts skipped (already posted successfully)")
        void testPostInterestForPeriod_WithSkippedAccounts() {
            int month = 1;
            int year = 2024;
            List<SavingsAccount> activeAccounts = Arrays.asList(sampleAccount,
                    SavingsAccount.builder().id(2L).build());
            
            when(accountRepository.findByAccountStatus(AccountLifecycleStatus.ACTIVE))
                    .thenReturn(activeAccounts);
            when(applicationContext.getBean(InterestPostingServiceImpl.class))
                    .thenReturn(selfProxy);
            when(selfProxy.postInterestForAccount(1L, month, year)).thenReturn(false);
            when(selfProxy.postInterestForAccount(2L, month, year)).thenReturn(true);

            InterestPostingService.JobSummary result = interestPostingService.postInterestForPeriod(month, year);

            assertThat(result.totalAccounts()).isEqualTo(2);
            assertThat(result.posted()).isEqualTo(1);
            assertThat(result.skipped()).isEqualTo(1);
            assertThat(result.failed()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC3: No active accounts found")
        void testPostInterestForPeriod_NoActiveAccounts() {
            int month = 1;
            int year = 2024;
            when(accountRepository.findByAccountStatus(AccountLifecycleStatus.ACTIVE))
                    .thenReturn(List.of());

            InterestPostingService.JobSummary result = interestPostingService.postInterestForPeriod(month, year);

            assertThat(result.totalAccounts()).isEqualTo(0);
            assertThat(result.posted()).isEqualTo(0);
            assertThat(result.skipped()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("postInterestForAccount() Tests")
    class PostInterestForAccountTests {

        @Test
        @DisplayName("TC4: Successfully post interest for a single account")
        void testPostInterestForAccount_Success() {
            Long accountId = 1L;
            int month = 1;
            int year = 2024;
            BigDecimal calculatedInterest = new BigDecimal("37.50");
            
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.findBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(Optional.empty());
            when(calculationService.calculateMonthlyInterest(
                    sampleAccount.getCurrentBalanceAmount(),
                    sampleProduct.getInterestRatePercent()))
                    .thenReturn(calculatedInterest);
            when(accountRepository.save(any(SavingsAccount.class))).thenReturn(sampleAccount);
            when(postingRepository.save(any(InterestPostingRecord.class))).thenReturn(samplePostingRecord);
            when(transactionRepository.save(any(SavingsAccountTransaction.class)))
                    .thenReturn(new SavingsAccountTransaction());

            boolean result = interestPostingService.postInterestForAccount(accountId, month, year);

            assertThat(result).isTrue();
            
            ArgumentCaptor<SavingsAccount> accountCaptor = ArgumentCaptor.forClass(SavingsAccount.class);
            verify(accountRepository).save(accountCaptor.capture());
            SavingsAccount updatedAccount = accountCaptor.getValue();
            assertThat(updatedAccount.getCurrentBalanceAmount())
                    .isEqualByComparingTo(new BigDecimal("10037.50"));
            
            verify(postingRepository).save(any(InterestPostingRecord.class));
            verify(transactionRepository).save(any(SavingsAccountTransaction.class));
        }

        @Test
        @DisplayName("TC5: Skip if SUCCESS record already exists")
        void testPostInterestForAccount_AlreadySuccess() {
            Long accountId = 1L;
            int month = 1;
            int year = 2024;
            
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.findBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(Optional.of(samplePostingRecord));

            boolean result = interestPostingService.postInterestForAccount(accountId, month, year);

            assertThat(result).isFalse();
            verify(accountRepository, never()).save(any());
            verify(postingRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC6: Retry FAILED record - covers yellow line condition")
        void testPostInterestForAccount_RetryFailedRecord() {
            Long accountId = 1L;
            int month = 1;
            int year = 2024;
            BigDecimal calculatedInterest = new BigDecimal("37.50");
            
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.findBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(Optional.of(failedPostingRecord));  // ← FAILED record exists
            when(calculationService.calculateMonthlyInterest(any(), any())).thenReturn(calculatedInterest);
            when(accountRepository.save(any())).thenReturn(sampleAccount);
            when(postingRepository.save(any())).thenReturn(samplePostingRecord);
            when(transactionRepository.save(any())).thenReturn(new SavingsAccountTransaction());

            boolean result = interestPostingService.postInterestForAccount(accountId, month, year);

            assertThat(result).isTrue();
            verify(postingRepository).delete(failedPostingRecord);  // ← Verifies FAILED record was deleted and retried
        }

        @Test
        @DisplayName("TC7: Account not found - covers yellow & red lines")
        void testPostInterestForAccount_AccountNotFound() {
            Long accountId = 999L;
            int month = 1;
            int year = 2024;
            
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> interestPostingService.postInterestForAccount(accountId, month, year))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found: " + accountId);
        }

        @Test
        @DisplayName("TC8: Skip account with zero or negative balance")
        void testPostInterestForAccount_ZeroBalance() {
            Long accountId = 1L;
            int month = 1;
            int year = 2024;
            sampleAccount.setCurrentBalanceAmount(BigDecimal.ZERO);
            
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.findBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(Optional.empty());

            boolean result = interestPostingService.postInterestForAccount(accountId, month, year);

            assertThat(result).isFalse();
            verify(calculationService, never()).calculateMonthlyInterest(any(), any());
        }

        @Test
        @DisplayName("TC9: Missing interest rate - covers yellow line condition")
        void testPostInterestForAccount_MissingInterestRate() {
            Long accountId = 1L;
            int month = 1;
            int year = 2024;
            sampleAccount.getSavingsProduct().setInterestRatePercent(null);
            
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.findBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> interestPostingService.postInterestForAccount(accountId, month, year))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Interest rate missing for account " + accountId);
        }
        
        @Test
        @DisplayName("TC10: Missing SavingsProduct - covers yellow line condition")
        void testPostInterestForAccount_MissingSavingsProduct() {
            Long accountId = 1L;
            int month = 1;
            int year = 2024;
            sampleAccount.setSavingsProduct(null);
            
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.findBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> interestPostingService.postInterestForAccount(accountId, month, year))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Interest rate missing for account " + accountId);
        }
        
        @Test
        @DisplayName("TC11: Null balance - skip account")
        void testPostInterestForAccount_NullBalance() {
            Long accountId = 1L;
            int month = 1;
            int year = 2024;
            sampleAccount.setCurrentBalanceAmount(null);
            
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.findBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(Optional.empty());

            boolean result = interestPostingService.postInterestForAccount(accountId, month, year);

            assertThat(result).isFalse();
            verify(calculationService, never()).calculateMonthlyInterest(any(), any());
        }
    }

    @Nested
    @DisplayName("saveFailedRecord() Tests")
    class SaveFailedRecordTests {

        @Test
        @DisplayName("TC12: Save FAILED record when posting fails")
        void testSaveFailedRecord_Success() {
            Long accountId = 1L;
            int month = 1;
            int year = 2024;
            
            when(postingRepository.existsBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(false);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.save(any(InterestPostingRecord.class))).thenReturn(failedPostingRecord);

            interestPostingService.saveFailedRecord(accountId, month, year);

            verify(postingRepository).save(any(InterestPostingRecord.class));
        }

        @Test
        @DisplayName("TC13: Don't save FAILED record if already exists")
        void testSaveFailedRecord_AlreadyExists() {
            Long accountId = 1L;
            int month = 1;
            int year = 2024;
            
            when(postingRepository.existsBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(true);

            interestPostingService.saveFailedRecord(accountId, month, year);

            verify(postingRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC14: Account not found when saving failed record - covers yellow & red lines")
        void testSaveFailedRecord_AccountNotFound() {
            Long accountId = 999L;
            int month = 1;
            int year = 2024;
            
            when(postingRepository.existsBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(false);
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThatCode(() -> interestPostingService.saveFailedRecord(accountId, month, year))
                    .doesNotThrowAnyException();
            
            verify(postingRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC15: Handle exception when saving FAILED record")
        void testSaveFailedRecord_ExceptionHandled() {
            Long accountId = 1L;
            int month = 1;
            int year = 2024;
            
            when(postingRepository.existsBySavingsAccount_IdAndPostingMonthAndPostingYear(accountId, month, year))
                    .thenReturn(false);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.save(any(InterestPostingRecord.class)))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatCode(() -> interestPostingService.saveFailedRecord(accountId, month, year))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getHistoryForAccount() Tests")
    class GetHistoryForAccountTests {

        @Test
        @DisplayName("TC16: Get interest history for existing account")
        void testGetHistoryForAccount_Success() {
            Long accountId = 1L;
            List<InterestPostingRecord> postingList = Arrays.asList(samplePostingRecord,
                    InterestPostingRecord.builder()
                            .id(3L)
                            .savingsAccount(sampleAccount)
                            .postingMonth(2)
                            .postingYear(2024)
                            .interestAmount(new BigDecimal("37.80"))
                            .build());
            
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.findBySavingsAccount_IdOrderByPostingYearDescPostingMonthDesc(accountId))
                    .thenReturn(postingList);

            List<InterestPostingResponseDTO> result = interestPostingService.getHistoryForAccount(accountId);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("TC17: Account not found - covers yellow & red lines")
        void testGetHistoryForAccount_AccountNotFound() {
            Long accountId = 999L;
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> interestPostingService.getHistoryForAccount(accountId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found: " + accountId);
        }

        @Test
        @DisplayName("TC18: Account exists but no posting history")
        void testGetHistoryForAccount_NoHistory() {
            Long accountId = 1L;
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(sampleAccount));
            when(postingRepository.findBySavingsAccount_IdOrderByPostingYearDescPostingMonthDesc(accountId))
                    .thenReturn(List.of());

            List<InterestPostingResponseDTO> result = interestPostingService.getHistoryForAccount(accountId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getHistoryForPeriod() Tests")
    class GetHistoryForPeriodTests {

        @Test
        @DisplayName("TC19: Get all interest postings for specific period")
        void testGetHistoryForPeriod_Success() {
            int month = 1;
            int year = 2024;
            List<InterestPostingRecord> postingList = Arrays.asList(
                    samplePostingRecord,
                    InterestPostingRecord.builder()
                            .id(4L)
                            .savingsAccount(SavingsAccount.builder().id(2L).build())
                            .postingMonth(month)
                            .postingYear(year)
                            .interestAmount(new BigDecimal("75.00"))
                            .build()
            );
            
            when(postingRepository.findByPostingMonthAndPostingYear(month, year))
                    .thenReturn(postingList);

            List<InterestPostingResponseDTO> result = interestPostingService.getHistoryForPeriod(month, year);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("TC20: No postings found for the period")
        void testGetHistoryForPeriod_NoPostings() {
            int month = 12;
            int year = 2023;
            when(postingRepository.findByPostingMonthAndPostingYear(month, year))
                    .thenReturn(List.of());

            List<InterestPostingResponseDTO> result = interestPostingService.getHistoryForPeriod(month, year);

            assertThat(result).isEmpty();
        }
    }
}