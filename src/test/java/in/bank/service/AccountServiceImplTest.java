package in.bank.service;

import in.bank.dto.SavingsAccountRequestDTO;
import in.bank.dto.SavingsAccountResponseDTO;
import in.bank.entity.AccountOpeningRequest;
import in.bank.entity.RequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImpl Tests")
class AccountServiceImplTest {

    @Mock
    private SavingsAccountService savingsAccountService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private AccountOpeningRequest accountOpeningRequest;
    private SavingsAccountResponseDTO savingsAccountResponse;

    @BeforeEach
    void setUp() {
        accountOpeningRequest = AccountOpeningRequest.builder()
                .id(1L)
                .userId(100L)
                .productId(200L)
                .initialDeposit(new BigDecimal("5000.00"))
                .status(RequestStatus.PENDING)
                .build();

        savingsAccountResponse = SavingsAccountResponseDTO.builder()
                .id(1L)
                .accountNumber("SAV100001")
                .userId(100L)
                .productName("Premium Savings")
                .currentBalanceAmount(new BigDecimal("5000.00"))
                .accountStatus("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(1L)
                .updatedBy(1L)
                .build();
    }

    @Nested
    @DisplayName("createAccountAfterApproval() Tests")
    class CreateAccountAfterApprovalTests {

        @Test
        @DisplayName("TC1: Create account after approval successfully - covers all red lines")
        void createAccountAfterApproval_Success() {
            // Arrange - createAccount returns a Long, so use when().thenReturn()
            when(savingsAccountService.createAccount(any(SavingsAccountRequestDTO.class)))
                    .thenReturn(1L);

            // Act
            accountService.createAccountAfterApproval(accountOpeningRequest);

            // Assert
            ArgumentCaptor<SavingsAccountRequestDTO> dtoCaptor = 
                    ArgumentCaptor.forClass(SavingsAccountRequestDTO.class);
            verify(savingsAccountService, times(1)).createAccount(dtoCaptor.capture());

            SavingsAccountRequestDTO capturedDto = dtoCaptor.getValue();
            assertThat(capturedDto.getUserId()).isEqualTo(accountOpeningRequest.getUserId());
            assertThat(capturedDto.getSavingsProductId()).isEqualTo(accountOpeningRequest.getProductId());
            assertThat(capturedDto.getOpeningBalance()).isEqualTo(accountOpeningRequest.getInitialDeposit());
        }

        @Test
        @DisplayName("TC2: Create account with different user ID")
        void createAccountAfterApproval_DifferentUserId() {
            // Arrange
            AccountOpeningRequest differentUserRequest = AccountOpeningRequest.builder()
                    .id(2L)
                    .userId(999L)
                    .productId(200L)
                    .initialDeposit(new BigDecimal("10000.00"))
                    .status(RequestStatus.PENDING)
                    .build();

            when(savingsAccountService.createAccount(any(SavingsAccountRequestDTO.class)))
                    .thenReturn(2L);

            // Act
            accountService.createAccountAfterApproval(differentUserRequest);

            // Assert
            ArgumentCaptor<SavingsAccountRequestDTO> dtoCaptor = 
                    ArgumentCaptor.forClass(SavingsAccountRequestDTO.class);
            verify(savingsAccountService).createAccount(dtoCaptor.capture());

            assertThat(dtoCaptor.getValue().getUserId()).isEqualTo(999L);
            assertThat(dtoCaptor.getValue().getOpeningBalance()).isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("TC3: Create account with different product ID")
        void createAccountAfterApproval_DifferentProductId() {
            // Arrange
            AccountOpeningRequest differentProductRequest = AccountOpeningRequest.builder()
                    .id(3L)
                    .userId(100L)
                    .productId(500L)
                    .initialDeposit(new BigDecimal("2500.00"))
                    .status(RequestStatus.PENDING)
                    .build();

            when(savingsAccountService.createAccount(any(SavingsAccountRequestDTO.class)))
                    .thenReturn(3L);

            // Act
            accountService.createAccountAfterApproval(differentProductRequest);

            // Assert
            ArgumentCaptor<SavingsAccountRequestDTO> dtoCaptor = 
                    ArgumentCaptor.forClass(SavingsAccountRequestDTO.class);
            verify(savingsAccountService).createAccount(dtoCaptor.capture());

            assertThat(dtoCaptor.getValue().getSavingsProductId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("TC4: Create account with zero opening balance")
        void createAccountAfterApproval_ZeroBalance() {
            // Arrange
            AccountOpeningRequest zeroBalanceRequest = AccountOpeningRequest.builder()
                    .id(4L)
                    .userId(100L)
                    .productId(200L)
                    .initialDeposit(BigDecimal.ZERO)
                    .status(RequestStatus.PENDING)
                    .build();

            when(savingsAccountService.createAccount(any(SavingsAccountRequestDTO.class)))
                    .thenReturn(4L);

            // Act
            accountService.createAccountAfterApproval(zeroBalanceRequest);

            // Assert
            ArgumentCaptor<SavingsAccountRequestDTO> dtoCaptor = 
                    ArgumentCaptor.forClass(SavingsAccountRequestDTO.class);
            verify(savingsAccountService).createAccount(dtoCaptor.capture());

            assertThat(dtoCaptor.getValue().getOpeningBalance()).isZero();
        }

        @Test
        @DisplayName("TC5: When savingsAccountService throws exception, it propagates")
        void createAccountAfterApproval_ServiceThrowsException() {
            // Arrange
            when(savingsAccountService.createAccount(any(SavingsAccountRequestDTO.class)))
                    .thenThrow(new RuntimeException("Failed to create account"));

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> 
                    accountService.createAccountAfterApproval(accountOpeningRequest));

            verify(savingsAccountService, times(1)).createAccount(any(SavingsAccountRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("findByUserId() Tests")
    class FindByUserIdTests {

        @Test
        @DisplayName("TC6: Find accounts by user ID successfully - covers red line")
        void findByUserId_Success() {
            // Arrange
            Long userId = 100L;
            List<SavingsAccountResponseDTO> expectedAccounts = Arrays.asList(
                    savingsAccountResponse,
                    SavingsAccountResponseDTO.builder()
                            .id(2L)
                            .accountNumber("SAV100002")
                            .userId(100L)
                            .productName("Regular Savings")
                            .currentBalanceAmount(new BigDecimal("10000.00"))
                            .accountStatus("ACTIVE")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .createdBy(1L)
                            .updatedBy(1L)
                            .build()
            );

            when(savingsAccountService.getAccountsByUserId(userId)).thenReturn(expectedAccounts);

            // Act
            List<SavingsAccountResponseDTO> result = accountService.findByUserId(userId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUserId()).isEqualTo(userId);
            assertThat(result.get(0).getAccountNumber()).isEqualTo("SAV100001");
            assertThat(result.get(1).getAccountNumber()).isEqualTo("SAV100002");
            
            verify(savingsAccountService, times(1)).getAccountsByUserId(userId);
        }

        @Test
        @DisplayName("TC7: Find accounts by user ID - returns empty list when no accounts")
        void findByUserId_EmptyList() {
            // Arrange
            Long userId = 999L;
            when(savingsAccountService.getAccountsByUserId(userId)).thenReturn(List.of());

            // Act
            List<SavingsAccountResponseDTO> result = accountService.findByUserId(userId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
            
            verify(savingsAccountService, times(1)).getAccountsByUserId(userId);
        }

        @Test
        @DisplayName("TC8: Find accounts by user ID - returns single account")
        void findByUserId_SingleAccount() {
            // Arrange
            Long userId = 100L;
            List<SavingsAccountResponseDTO> expectedAccounts = List.of(savingsAccountResponse);

            when(savingsAccountService.getAccountsByUserId(userId)).thenReturn(expectedAccounts);

            // Act
            List<SavingsAccountResponseDTO> result = accountService.findByUserId(userId);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAccountNumber()).isEqualTo("SAV100001");
            
            verify(savingsAccountService, times(1)).getAccountsByUserId(userId);
        }

        @Test
        @DisplayName("TC9: Find accounts by user ID with different user ID")
        void findByUserId_DifferentUserId() {
            // Arrange
            Long userId = 200L;
            SavingsAccountResponseDTO differentUserAccount = SavingsAccountResponseDTO.builder()
                    .id(3L)
                    .accountNumber("SAV200001")
                    .userId(200L)
                    .productName("Premium Savings")
                    .currentBalanceAmount(new BigDecimal("7500.00"))
                    .accountStatus("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy(1L)
                    .updatedBy(1L)
                    .build();

            when(savingsAccountService.getAccountsByUserId(userId)).thenReturn(List.of(differentUserAccount));

            // Act
            List<SavingsAccountResponseDTO> result = accountService.findByUserId(userId);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(200L);
            assertThat(result.get(0).getAccountNumber()).isEqualTo("SAV200001");
            
            verify(savingsAccountService, times(1)).getAccountsByUserId(userId);
        }

        @Test
        @DisplayName("TC10: When savingsAccountService throws exception, it propagates")
        void findByUserId_ServiceThrowsException() {
            // Arrange
            Long userId = 100L;
            when(savingsAccountService.getAccountsByUserId(userId))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> 
                    accountService.findByUserId(userId));

            verify(savingsAccountService, times(1)).getAccountsByUserId(userId);
        }
    }

    @Nested
    @DisplayName("Integration-like Tests (Multiple Calls)")
    class MultipleCallsTests {

        @Test
        @DisplayName("TC11: Multiple calls to create account")
        void multipleCreateAccountCalls() {
            // Arrange
            AccountOpeningRequest request1 = AccountOpeningRequest.builder()
                    .id(1L).userId(100L).productId(200L).initialDeposit(new BigDecimal("5000.00"))
                    .status(RequestStatus.PENDING).build();
            AccountOpeningRequest request2 = AccountOpeningRequest.builder()
                    .id(2L).userId(101L).productId(201L).initialDeposit(new BigDecimal("6000.00"))
                    .status(RequestStatus.PENDING).build();

            when(savingsAccountService.createAccount(any(SavingsAccountRequestDTO.class)))
                    .thenReturn(1L, 2L);

            // Act
            accountService.createAccountAfterApproval(request1);
            accountService.createAccountAfterApproval(request2);

            // Assert
            verify(savingsAccountService, times(2)).createAccount(any(SavingsAccountRequestDTO.class));
        }

        @Test
        @DisplayName("TC12: Create account then find by user ID")
        void createThenFindByUserId() {
            // Arrange
            Long userId = 100L;
            when(savingsAccountService.createAccount(any(SavingsAccountRequestDTO.class)))
                    .thenReturn(1L);
            when(savingsAccountService.getAccountsByUserId(userId)).thenReturn(List.of(savingsAccountResponse));

            // Act
            accountService.createAccountAfterApproval(accountOpeningRequest);
            List<SavingsAccountResponseDTO> result = accountService.findByUserId(userId);

            // Assert
            verify(savingsAccountService, times(1)).createAccount(any(SavingsAccountRequestDTO.class));
            verify(savingsAccountService, times(1)).getAccountsByUserId(userId);
            assertThat(result).hasSize(1);
        }
    }
}