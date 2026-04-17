package in.bank.service;

import in.bank.dto.AccountSnapshot;
import in.bank.dto.SavingsAccountRequestDTO;
import in.bank.dto.SavingsAccountResponseDTO;
import in.bank.entity.AccountLifecycleStatus;
import in.bank.entity.AppUser;
import in.bank.entity.SavingsAccount;
import in.bank.entity.SavingsProduct;
import in.bank.exception.AccessDeniedException;
import in.bank.exception.BadRequestException;
import in.bank.exception.DuplicateResourceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.SavingsAccountRepository;
import in.bank.repository.SavingsProductRepository;
import in.bank.repository.UserRepository;
import in.bank.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingsAccountServiceImpl implements SavingsAccountService {

    private final SavingsAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SavingsProductRepository productRepository;

    // ================= CREATE (MANUAL) =================
    @Override
    public Long createAccount(SavingsAccountRequestDTO request) {

        AppUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found: " + request.getUserId()));

        SavingsProduct product = productRepository.findById(request.getSavingsProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Savings product not found: " + request.getSavingsProductId()));

        // ✅ Prevent duplicate account for same user + product
        if (accountRepository.existsByUser_IdAndSavingsProduct_Id(
                request.getUserId(), request.getSavingsProductId())) {
            throw new DuplicateResourceException(
                "Account already exists for this user and product");
        }

        SavingsAccount account = SavingsAccount.builder()
                .accountNumber(generateAccountNumber())
                .user(user)
                .savingsProduct(product)
                .currentBalanceAmount(request.getOpeningBalance())
                .interestRate(product.getInterestRatePercent())
                .accountStatus(AccountLifecycleStatus.ACTIVE)
                .build();

        accountRepository.save(account);
        System.out.println("✅ Account saved: " + account.getId()
            + " | " + account.getAccountNumber());

        return account.getId();
    }

    // ================= CREATE (AUTO — called after KYC VERIFIED) =================
    @Override
    public void createDefaultAccountForCustomer(Long customerId) {

        // ✅ Prevent duplicate active account
        boolean alreadyExists = accountRepository.findByUser_Id(customerId)
                .stream()
                .anyMatch(acc -> acc.getAccountStatus() == AccountLifecycleStatus.ACTIVE);

        if (alreadyExists) {
            throw new DuplicateResourceException(
                "Active savings account already exists for customerId: " + customerId);
        }

        // ✅ Fetch the user
        AppUser user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found for customerId: " + customerId));

        // ✅ Pick the first available savings product as default
        SavingsProduct defaultProduct = productRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No savings product configured. Please add one first."));

        // ✅ Build and save the account
        SavingsAccount account = SavingsAccount.builder()
                .accountNumber(generateAccountNumber())
                .user(user)
                .savingsProduct(defaultProduct)
                .currentBalanceAmount(BigDecimal.ZERO)
                .interestRate(defaultProduct.getInterestRatePercent())
                .accountStatus(AccountLifecycleStatus.ACTIVE)
                .build();

        accountRepository.save(account);
        System.out.println("✅ Auto account created for customerId: " + customerId
            + " | Account Number: " + account.getAccountNumber());
    }

    // ================= READ =================
    @Override
    @Transactional(readOnly = true)
    public SavingsAccountResponseDTO getById(Long id, UserDetails userDetails) {

        SavingsAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            Long loggedInUserId = ((CustomUserDetails) userDetails).getId();

            if (!account.getUser().getId().equals(loggedInUserId)) {  // ✅ fixed
                throw new AccessDeniedException("You are not authorized to access this account");
            }
        }

        return mapToDTO(account);  // ✅ fixed — use existing helper
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SavingsAccountResponseDTO> getAll() {
        return accountRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsAccountResponseDTO> getAccountsByUserId(Long userId) {
        return accountRepository.findByUser_Id(userId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ================= LIFECYCLE (individual — kept for internal use) =================
    @Override
    public void freezeAccount(Long id) {
        SavingsAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));

        if (account.getAccountStatus() == AccountLifecycleStatus.CLOSED) {
            throw new BadRequestException("Cannot freeze a closed account");
        }

        account.setAccountStatus(AccountLifecycleStatus.FROZEN);
        accountRepository.save(account);
    }

    @Override
    public void closeAccount(Long id) {
        SavingsAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));

        if (account.getAccountStatus() == AccountLifecycleStatus.CLOSED) {
            throw new BadRequestException("Account is already closed");
        }

        account.setAccountStatus(AccountLifecycleStatus.CLOSED);
        accountRepository.save(account);
    }

    // ================= LIFECYCLE (UNIFIED — used by controller path param) =================
    @Override
    public void updateAccountStatus(Long id, AccountLifecycleStatus status) {

        SavingsAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));

        // ❌ CLOSED is irreversible — no further status changes allowed
        if (account.getAccountStatus() == AccountLifecycleStatus.CLOSED) {
            throw new BadRequestException(
                "Account is permanently CLOSED and cannot be modified");
        }

        // ❌ Prevent setting the same status again
        if (account.getAccountStatus() == status) {
            throw new BadRequestException(
                "Account is already " + status.name());
        }

        account.setAccountStatus(status);
        accountRepository.save(account);
    }

    // ================= INTEREST =================
    @Override
    public void addInterest(String accountNumber, BigDecimal interest) {
        SavingsAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Account not found: " + accountNumber));

        if (account.getAccountStatus() != AccountLifecycleStatus.ACTIVE) {
            throw new BadRequestException("Interest can only be added to ACTIVE accounts");
        }

        account.setCurrentBalanceAmount(
            account.getCurrentBalanceAmount().add(interest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountSnapshot> findAllActive() {
        return accountRepository.findAllActiveWithProduct()
                .stream()
                .map(acc -> new AccountSnapshot(
                        acc.getAccountNumber(),
                        acc.getCurrentBalanceAmount(),
                        toDecimalRate(acc.getSavingsProduct().getInterestRatePercent())
                ))
                .toList();
    }

    // ================= HELPERS =================
    private SavingsAccountResponseDTO mapToDTO(SavingsAccount account) {
        return SavingsAccountResponseDTO.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountStatus(account.getAccountStatus().name())
                .currentBalanceAmount(account.getCurrentBalanceAmount())
                .userId(account.getUser().getId())
                .productName(account.getSavingsProduct().getProductName())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .createdBy(account.getCreatedBy())
                .updatedBy(account.getUpdatedBy())
                .build();
    }

    private BigDecimal toDecimalRate(BigDecimal percentRate) {
        return percentRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }

    private String generateAccountNumber() {
        return "SA-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}