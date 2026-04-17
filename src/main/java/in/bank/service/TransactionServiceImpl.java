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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final SavingsAccountRepository accountRepository;
    private final SavingsAccountTransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyRepo;
    private final JwtService jwtService;

    // ================= CREATE TRANSACTION =================
    @Override
    @Transactional
    public TransactionCreateResponseDTO processTransaction(
            String idempotencyKey,
            TransactionRequestDTO request,
            UserDetails userDetails) {  // ✅ added

        // STEP 0: VALIDATE IDEMPOTENCY KEY
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        // STEP 1: IDEMPOTENCY CHECK
        Optional<IdempotencyKey> existing = idempotencyRepo.findById(idempotencyKey);
        if (existing.isPresent()) {
            try {
                return TransactionCreateResponseDTO.builder()
                        .transactionId(Long.valueOf(existing.get().getResponse()))
                        .message("Duplicate request detected. Returning original transaction result.")  // ✅
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Invalid stored idempotency response");
            }
        }

        // BLOCK manual INTEREST transactions
        if (request.getTransactionType() == TransactionType.INTEREST) {
            throw new IllegalArgumentException("Interest is system-generated only");
        }

        // Fetch account
        SavingsAccount account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // ✅ OWNERSHIP CHECK — CUSTOMER can only transact on their own account
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            Long loggedInUserId = ((CustomUserDetails) userDetails).getId();
            if (!account.getUser().getId().equals(loggedInUserId)) {
                throw new AccessDeniedException(
                        "You are not authorized to perform transactions on this account");
            }
        }

        // Account frozen check
        if (account.getAccountStatus() == AccountLifecycleStatus.FROZEN) {
            throw new AccountFrozenException("Account is frozen");
        }

        // KYC validation
        validateTransaction(account.getUser(), request.getAmount());

        BigDecimal balanceBefore = account.getCurrentBalanceAmount();
        BigDecimal balanceAfter;

        if (request.getTransactionType() == TransactionType.WITHDRAWAL) {
            if (balanceBefore.compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance");
            }
            balanceAfter = balanceBefore.subtract(request.getAmount());
        } else {
            balanceAfter = balanceBefore.add(request.getAmount());
        }

        // Update balance
        account.setCurrentBalanceAmount(balanceAfter);

        // Create transaction
        SavingsAccountTransaction txn = SavingsAccountTransaction.builder()
                .savingsAccount(account)
                .type(request.getTransactionType())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMode(request.getPaymentMode())
                .description(request.getDescription())
                .balanceBeforeTransaction(balanceBefore)
                .balanceAfterTransaction(balanceAfter)
                .transactionDate(LocalDateTime.now())
                .interestPosting(BigDecimal.ZERO)
                .interestPostedAt(null)
                .build();

        transactionRepository.save(txn);

        // Save idempotency
        try {
            IdempotencyKey entity = new IdempotencyKey();
            entity.setKey(idempotencyKey);
            entity.setResponse(txn.getId().toString());
            entity.setStatusCode(200);
            entity.setCreatedAt(LocalDateTime.now());
            idempotencyRepo.save(entity);
        } catch (Exception e) {
            Optional<IdempotencyKey> retry = idempotencyRepo.findById(idempotencyKey);
            if (retry.isPresent()) {
                return TransactionCreateResponseDTO.builder()
                        .transactionId(Long.valueOf(retry.get().getResponse()))
                        .message("Duplicate request detected. Returning original transaction result.")  // ✅
                        .build();
            }
            throw e;
        }

        return TransactionCreateResponseDTO.builder()
                .transactionId(txn.getId())
                .message("Transaction processed successfully.")  // ✅
                .build();
    }

    // ================= KYC VALIDATION =================
    private void validateTransaction(AppUser user, BigDecimal amount) {
        if (user == null || user.getKycStatus() == null) {
            throw new RuntimeException("KYC status not available");
        }

        switch (user.getKycStatus()) {
            case MIN_KYC:
                if (amount.compareTo(BigDecimal.valueOf(50000)) > 0) {
                    throw new RuntimeException("Transaction exceeds limit for MIN_KYC (50,000)");
                }
                break;
            case FULL_KYC:
                if (amount.compareTo(BigDecimal.valueOf(1000000)) > 0) {
                    throw new BadRequestException("Transaction exceeds limit for FULL_KYC (10,00,000)");
                }
                break;
            default:
                throw new BadRequestException("Unknown KYC status");
        }
    }

    // ================= GET ALL =================
    @Override
    public List<TransactionResponseDTO> getAllTransactionsDTO(Long accountId) {
        return transactionRepository.findBySavingsAccount_Id(accountId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    // ================= GET SINGLE =================
    @Override
    public TransactionResponseDTO getTransactionById(Long accountId, Long transactionId) {
        SavingsAccountTransaction txn = transactionRepository
                .findByIdAndSavingsAccount_Id(transactionId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + transactionId +
                                " for account: " + accountId));

        return mapToResponseDTO(txn);
    }

    // ================= MY TRANSACTIONS =================
    @Override
    public List<TransactionResponseDTO> getTransactionsForLoggedInUser() {
        Long userId = jwtService.getLoggedInUserId();
        List<SavingsAccount> accounts = accountRepository.findByUser_Id(userId);

        return accounts.stream()
                .flatMap(acc -> transactionRepository.findBySavingsAccount_Id(acc.getId()).stream())
                .map(this::mapToResponseDTO)
                .toList();
    }

    // ================= SUMMARY =================
    @Override
    public TransactionSummaryDTO getTransactionSummary(Long accountId) {
        SavingsAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + accountId));

        return TransactionSummaryDTO.builder()
                .currentBalance(account.getCurrentBalanceAmount())
                .build();
    }

    // ================= FILTER =================
    @Override
    public List<TransactionResponseDTO> filterTransactions(
            Long accountId,
            TransactionType type,
            LocalDateTime start,
            LocalDateTime end) {

        return transactionRepository
                .findBySavingsAccount_Id(accountId)
                .stream()
                .filter(t -> type == null || t.getType() == type)
                .filter(t -> start == null || !t.getTransactionDate().isBefore(start))
                .filter(t -> end == null || !t.getCreatedAt().isAfter(end))
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // ================= MAPPER =================
    private TransactionResponseDTO mapToResponseDTO(SavingsAccountTransaction txn) {
        return TransactionResponseDTO.builder()
                .transactionId(txn.getId())
                .type(txn.getType())
                .amount(txn.getAmount())
                .balanceBeforeTransaction(txn.getBalanceBeforeTransaction())
                .balanceAfterTransaction(txn.getBalanceAfterTransaction())
                .currency(txn.getCurrency())
                .paymentMode(txn.getPaymentMode())
                .description(txn.getDescription())
                .interestPosting(txn.getInterestPosting())
                .interestPostedAt(txn.getInterestPostedAt())
                .transactionDate(txn.getTransactionDate())
                .createdAt(txn.getCreatedAt())
                .updatedAt(txn.getUpdatedAt())
                .createdBy(txn.getCreatedBy())
                .updatedBy(txn.getUpdatedBy())
                .build();
    }
}