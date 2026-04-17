package in.bank.service;

import in.bank.dto.AccountSnapshot;
import in.bank.dto.SavingsAccountRequestDTO;
import in.bank.dto.SavingsAccountResponseDTO;
import in.bank.entity.*;
import in.bank.exception.AccessDeniedException;
import in.bank.exception.BadRequestException;
import in.bank.exception.DuplicateResourceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.SavingsAccountRepository;
import in.bank.repository.SavingsProductRepository;
import in.bank.repository.UserRepository;
import in.bank.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavingsAccountServiceImpl Tests")
class SavingsAccountServiceImplTest {

    @Mock
    private SavingsAccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SavingsProductRepository productRepository;

    @InjectMocks
    private SavingsAccountServiceImpl savingsAccountService;

    private AppUser testUser;
    private AppUser anotherUser;
    private SavingsProduct testProduct;
    private SavingsProduct anotherProduct;
    private SavingsAccount testAccount;
    private SavingsAccount frozenAccount;
    private SavingsAccount closedAccount;
    private SavingsAccountRequestDTO createRequest;
    private UserDetails adminUserDetails;
    private UserDetails customerUserDetails;
    private UserDetails anotherCustomerDetails;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
                .id(1L)
                .email("customer@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        anotherUser = AppUser.builder()
                .id(2L)
                .email("other@example.com")
                .build();

        testProduct = SavingsProduct.builder()
                .id(1L)
                .productName("Basic Savings")
                .interestRatePercent(new BigDecimal("4.00"))
                .build();

        anotherProduct = SavingsProduct.builder()
                .id(2L)
                .productName("Premium Savings")
                .interestRatePercent(new BigDecimal("5.50"))
                .build();

        testAccount = SavingsAccount.builder()
                .id(100L)
                .accountNumber("SA-ABC123")
                .user(testUser)
                .savingsProduct(testProduct)
                .currentBalanceAmount(new BigDecimal("5000.00"))
                .interestRate(new BigDecimal("4.00"))
                .accountStatus(AccountLifecycleStatus.ACTIVE)
                .build();

        frozenAccount = SavingsAccount.builder()
                .id(101L)
                .accountNumber("SA-FROZEN")
                .user(testUser)
                .savingsProduct(testProduct)
                .currentBalanceAmount(new BigDecimal("1000.00"))
                .accountStatus(AccountLifecycleStatus.FROZEN)
                .build();

        closedAccount = SavingsAccount.builder()
                .id(102L)
                .accountNumber("SA-CLOSED")
                .user(testUser)
                .savingsProduct(testProduct)
                .currentBalanceAmount(BigDecimal.ZERO)
                .accountStatus(AccountLifecycleStatus.CLOSED)
                .build();

        createRequest = new SavingsAccountRequestDTO();
        createRequest.setUserId(1L);
        createRequest.setSavingsProductId(1L);
        createRequest.setOpeningBalance(new BigDecimal("1000.00"));

        adminUserDetails = new CustomUserDetails(
                99L, "admin@bank.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        customerUserDetails = new CustomUserDetails(
                1L, "customer@example.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        anotherCustomerDetails = new CustomUserDetails(
                2L, "other@example.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    @Nested
    @DisplayName("createAccount() Tests")
    class CreateAccountTests {

        @Test
        @DisplayName("TC1: Create account successfully")
        void createAccount_ShouldReturnAccountId_WhenValidRequest() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(accountRepository.existsByUser_IdAndSavingsProduct_Id(1L, 1L)).thenReturn(false);
            when(accountRepository.save(any(SavingsAccount.class))).thenAnswer(invocation -> {
                SavingsAccount saved = invocation.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            Long accountId = savingsAccountService.createAccount(createRequest);

            assertThat(accountId).isEqualTo(100L);
            verify(accountRepository, times(1)).save(any(SavingsAccount.class));
        }

        @Test
        @DisplayName("TC2: User not found throws exception")
        void createAccount_UserNotFound_ThrowsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsAccountService.createAccount(createRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found: 1");
        }

        @Test
        @DisplayName("TC3: Product not found throws exception")
        void createAccount_ProductNotFound_ThrowsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsAccountService.createAccount(createRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Savings product not found: 1");
        }

        @Test
        @DisplayName("TC4: Duplicate account throws exception")
        void createAccount_DuplicateAccount_ThrowsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(accountRepository.existsByUser_IdAndSavingsProduct_Id(1L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> savingsAccountService.createAccount(createRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Account already exists");
        }
    }

    @Nested
    @DisplayName("createDefaultAccountForCustomer() Tests")
    class CreateDefaultAccountForCustomerTests {

        @Test
        @DisplayName("TC5: Create default account successfully")
        void createDefaultAccountForCustomer_Success() {
            when(accountRepository.findByUser_Id(1L)).thenReturn(List.of());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findAll()).thenReturn(List.of(testProduct));
            when(accountRepository.save(any(SavingsAccount.class))).thenAnswer(inv -> inv.getArgument(0));

            savingsAccountService.createDefaultAccountForCustomer(1L);

            verify(accountRepository, times(1)).save(any(SavingsAccount.class));
        }

        @Test
        @DisplayName("TC6: Active account already exists - throws exception - covers yellow line")
        void createDefaultAccountForCustomer_AlreadyExists_ThrowsException() {
            when(accountRepository.findByUser_Id(1L)).thenReturn(List.of(testAccount));

            assertThatThrownBy(() -> savingsAccountService.createDefaultAccountForCustomer(1L))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Active savings account already exists");
        }

        @Test
        @DisplayName("TC7: Multiple active accounts exist - covers anyMatch condition")
        void createDefaultAccountForCustomer_MultipleActiveAccounts_ThrowsException() {
            SavingsAccount anotherActiveAccount = SavingsAccount.builder()
                    .id(103L)
                    .accountNumber("SA-ACTIVE2")
                    .user(testUser)
                    .savingsProduct(testProduct)
                    .currentBalanceAmount(new BigDecimal("2000.00"))
                    .accountStatus(AccountLifecycleStatus.ACTIVE)
                    .build();
            
            when(accountRepository.findByUser_Id(1L)).thenReturn(List.of(testAccount, anotherActiveAccount));

            assertThatThrownBy(() -> savingsAccountService.createDefaultAccountForCustomer(1L))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Active savings account already exists");
        }

        @Test
        @DisplayName("TC8: User not found - throws exception")
        void createDefaultAccountForCustomer_UserNotFound_ThrowsException() {
            when(accountRepository.findByUser_Id(1L)).thenReturn(List.of());
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsAccountService.createDefaultAccountForCustomer(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found for customerId: 1");
        }

        @Test
        @DisplayName("TC9: No product configured - throws exception")
        void createDefaultAccountForCustomer_NoProduct_ThrowsException() {
            when(accountRepository.findByUser_Id(1L)).thenReturn(List.of());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findAll()).thenReturn(List.of());

            assertThatThrownBy(() -> savingsAccountService.createDefaultAccountForCustomer(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No savings product configured");
        }
    }

    @Nested
    @DisplayName("getById() Tests")
    class GetByIdTests {

        @Test
        @DisplayName("TC10: Get account by ID as owner - success")
        void getById_ShouldReturnAccountDTO_WhenCustomerOwnsAccount() {
            when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));

            SavingsAccountResponseDTO result = savingsAccountService.getById(100L, customerUserDetails);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getAccountNumber()).isEqualTo("SA-ABC123");
        }

        @Test
        @DisplayName("TC11: Get account by ID as different customer - throws AccessDeniedException - covers yellow line")
        void getById_ShouldThrowAccessDeniedException_WhenCustomerDoesNotOwnAccount() {
            when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));

            assertThatThrownBy(() -> savingsAccountService.getById(100L, anotherCustomerDetails))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("You are not authorized to access this account");
        }

        @Test
        @DisplayName("TC12: Get account by ID as admin - success")
        void getById_ShouldReturnAccountDTO_WhenAdminAccessesAnyAccount() {
            when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));

            SavingsAccountResponseDTO result = savingsAccountService.getById(100L, adminUserDetails);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("TC13: Account not found - throws exception - covers yellow line")
        void getById_AccountNotFound_ThrowsException() {
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsAccountService.getById(999L, adminUserDetails))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found with id: 999");
        }
    }

    @Nested
    @DisplayName("getAll() Tests")
    class GetAllTests {

        @Test
        @DisplayName("TC14: Get all accounts - returns list")
        void getAll_ReturnsList() {
            when(accountRepository.findAll()).thenReturn(List.of(testAccount, frozenAccount));

            List<SavingsAccountResponseDTO> result = savingsAccountService.getAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAccountNumber()).isEqualTo("SA-ABC123");
        }

        @Test
        @DisplayName("TC15: Get all accounts - empty list")
        void getAll_EmptyList() {
            when(accountRepository.findAll()).thenReturn(List.of());

            List<SavingsAccountResponseDTO> result = savingsAccountService.getAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAccountsByUserId() Tests")
    class GetAccountsByUserIdTests {

        @Test
        @DisplayName("TC16: Get accounts by user ID - returns list")
        void getAccountsByUserId_ReturnsList() {
            when(accountRepository.findByUser_Id(1L)).thenReturn(List.of(testAccount, frozenAccount));

            List<SavingsAccountResponseDTO> result = savingsAccountService.getAccountsByUserId(1L);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("TC17: Get accounts by user ID - empty list")
        void getAccountsByUserId_EmptyList() {
            when(accountRepository.findByUser_Id(999L)).thenReturn(List.of());

            List<SavingsAccountResponseDTO> result = savingsAccountService.getAccountsByUserId(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("freezeAccount() Tests")
    class FreezeAccountTests {

        @Test
        @DisplayName("TC18: Freeze account successfully")
        void freezeAccount_Success() {
            when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));

            savingsAccountService.freezeAccount(100L);

            assertThat(testAccount.getAccountStatus()).isEqualTo(AccountLifecycleStatus.FROZEN);
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("TC19: Freeze already closed account - throws exception")
        void freezeAccount_AlreadyClosed_ThrowsException() {
            when(accountRepository.findById(102L)).thenReturn(Optional.of(closedAccount));

            assertThatThrownBy(() -> savingsAccountService.freezeAccount(102L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot freeze a closed account");
        }

        @Test
        @DisplayName("TC20: Freeze account not found - throws exception - covers yellow line")
        void freezeAccount_NotFound_ThrowsException() {
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsAccountService.freezeAccount(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found: 999");
        }
    }

    @Nested
    @DisplayName("closeAccount() Tests")
    class CloseAccountTests {

        @Test
        @DisplayName("TC21: Close account successfully")
        void closeAccount_Success() {
            when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));

            savingsAccountService.closeAccount(100L);

            assertThat(testAccount.getAccountStatus()).isEqualTo(AccountLifecycleStatus.CLOSED);
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("TC22: Close already closed account - throws exception")
        void closeAccount_AlreadyClosed_ThrowsException() {
            when(accountRepository.findById(102L)).thenReturn(Optional.of(closedAccount));

            assertThatThrownBy(() -> savingsAccountService.closeAccount(102L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Account is already closed");
        }

        @Test
        @DisplayName("TC23: Close account not found - throws exception")
        void closeAccount_NotFound_ThrowsException() {
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsAccountService.closeAccount(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateAccountStatus() Tests")
    class UpdateAccountStatusTests {

        @Test
        @DisplayName("TC24: Update account status to FROZEN")
        void updateAccountStatus_ToFrozen_Success() {
            when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));

            savingsAccountService.updateAccountStatus(100L, AccountLifecycleStatus.FROZEN);

            assertThat(testAccount.getAccountStatus()).isEqualTo(AccountLifecycleStatus.FROZEN);
            verify(accountRepository).save(testAccount);
        }

        @Test
        @DisplayName("TC25: Update account status - already CLOSED - throws exception")
        void updateAccountStatus_AlreadyClosed_ThrowsException() {
            when(accountRepository.findById(102L)).thenReturn(Optional.of(closedAccount));

            assertThatThrownBy(() -> savingsAccountService.updateAccountStatus(102L, AccountLifecycleStatus.FROZEN))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Account is permanently CLOSED");
        }

        @Test
        @DisplayName("TC26: Update account status - same status - throws exception - covers yellow line")
        void updateAccountStatus_SameStatus_ThrowsException() {
            when(accountRepository.findById(100L)).thenReturn(Optional.of(testAccount));

            assertThatThrownBy(() -> savingsAccountService.updateAccountStatus(100L, AccountLifecycleStatus.ACTIVE))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Account is already ACTIVE");
        }
        
        @Test
        @DisplayName("TC27: Update account status - account not found - covers yellow line")
        void updateAccountStatus_NotFound_ThrowsException() {
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsAccountService.updateAccountStatus(999L, AccountLifecycleStatus.FROZEN))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found: 999");
        }
    }

    @Nested
    @DisplayName("addInterest() Tests")
    class AddInterestTests {

        @Test
        @DisplayName("TC28: Add interest to active account")
        void addInterest_Success() {
            when(accountRepository.findByAccountNumber("SA-ABC123")).thenReturn(Optional.of(testAccount));

            savingsAccountService.addInterest("SA-ABC123", new BigDecimal("100.00"));

            assertThat(testAccount.getCurrentBalanceAmount()).isEqualByComparingTo("5100.00");
        }

        @Test
        @DisplayName("TC29: Add interest to non-active account - throws exception")
        void addInterest_NonActiveAccount_ThrowsException() {
            when(accountRepository.findByAccountNumber("SA-FROZEN")).thenReturn(Optional.of(frozenAccount));

            assertThatThrownBy(() -> savingsAccountService.addInterest("SA-FROZEN", new BigDecimal("100.00")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Interest can only be added to ACTIVE accounts");
        }

        @Test
        @DisplayName("TC30: Add interest - account not found")
        void addInterest_AccountNotFound_ThrowsException() {
            when(accountRepository.findByAccountNumber("NOT-FOUND")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savingsAccountService.addInterest("NOT-FOUND", new BigDecimal("100.00")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAllActive() Tests")
    class FindAllActiveTests {

        @Test
        @DisplayName("TC31: Find all active accounts - returns snapshots")
        void findAllActive_ReturnsSnapshots() {
            when(accountRepository.findAllActiveWithProduct()).thenReturn(List.of(testAccount));

            List<AccountSnapshot> result = savingsAccountService.findAllActive();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).accountId()).isEqualTo("SA-ABC123");
            assertThat(result.get(0).balance()).isEqualByComparingTo("5000.00");
            assertThat(result.get(0).annualRate()).isEqualByComparingTo("0.04");
        }

        @Test
        @DisplayName("TC32: Find all active accounts - empty list")
        void findAllActive_EmptyList() {
            when(accountRepository.findAllActiveWithProduct()).thenReturn(List.of());

            List<AccountSnapshot> result = savingsAccountService.findAllActive();

            assertThat(result).isEmpty();
        }
    }
}