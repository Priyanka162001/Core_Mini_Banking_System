package in.bank.service;

import in.bank.dto.InterestPostingResponseDTO;
import in.bank.entity.*;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.InterestPostingRepository;
import in.bank.repository.SavingsAccountRepository;
import in.bank.repository.SavingsAccountTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestPostingServiceImpl implements InterestPostingService {

    private final SavingsAccountRepository accountRepository;
    private final InterestPostingRepository postingRepository;
    private final SavingsAccountTransactionRepository transactionRepository;
    private final InterestCalculationService calculationService;
    private final ApplicationContext applicationContext;

    private InterestPostingServiceImpl self() {
        return applicationContext.getBean(InterestPostingServiceImpl.class);
    }

    // ================= POST INTEREST FOR ALL ACCOUNTS =================
    @Override
    @Transactional(readOnly = true)
    public JobSummary postInterestForPeriod(int month, int year) {

        List<SavingsAccount> activeAccounts =
                accountRepository.findByAccountStatus(AccountLifecycleStatus.ACTIVE);

        int posted = 0, skipped = 0, failed = 0;

        for (SavingsAccount account : activeAccounts) {
            try {
                boolean wasPosted = self().postInterestForAccount(account.getId(), month, year);

                if (wasPosted) {
                    posted++;
                } else {
                    skipped++;
                }

            } catch (Exception e) {
                failed++;

                // 🔥 FULL ERROR LOG (VERY IMPORTANT)
                log.error("❌ ERROR for account {} -> {}", account.getId(), e.getMessage(), e);

                // 🔥 TEMP: Throw to see exact DB error in console
                throw e;

                // ⛔ COMMENT THIS TEMPORARILY WHILE DEBUGGING
                // self().saveFailedRecord(account.getId(), month, year);
            }
        }

        log.info("✅ Job done — Posted: {}, Skipped: {}, Failed: {}", posted, skipped, failed);

        return new JobSummary(month, year, activeAccounts.size(), posted, skipped, failed);
    }
    
    // ================= SAVE FAILED RECORD =================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedRecord(Long accountId, int month, int year) {
        try {
            boolean exists = postingRepository
                    .existsBySavingsAccount_IdAndPostingMonthAndPostingYear(
                            accountId, month, year);

            if (!exists) {
                SavingsAccount account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Account not found: " + accountId));

                InterestPostingRecord failedRecord = InterestPostingRecord.builder()
                        .savingsAccount(account)
                        .postingMonth(month)
                        .postingYear(year)
                        .status("FAILED")
                        .interestAmount(BigDecimal.ZERO)
                        .balanceBefore(account.getCurrentBalanceAmount())
                        .balanceAfter(account.getCurrentBalanceAmount())
                        .annualInterestRate(
                                account.getSavingsProduct().getInterestRatePercent()
                        )
                        .build();

                postingRepository.save(failedRecord);
                log.info("📝 Saved FAILED record for account {}", accountId);
            }
        } catch (Exception e) {
            log.error("⚠️ Could not save FAILED record for account {}", accountId, e);
        }
    }

    // ================= POST INTEREST FOR SINGLE ACCOUNT =================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean postInterestForAccount(Long accountId, int month, int year) {

        SavingsAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountId));

        // ✅ SMART IDEMPOTENCY CHECK
        InterestPostingRecord existing = postingRepository
                .findBySavingsAccount_IdAndPostingMonthAndPostingYear(
                        accountId, month, year)
                .orElse(null);

        // ✅ Skip only if SUCCESS
        if (existing != null && "SUCCESS".equals(existing.getStatus())) {
            log.debug("⏭ Skipping account {} — already SUCCESS for {}/{}",
                    accountId, month, year);
            return false;
        }

        // ✅ Retry if FAILED
        if (existing != null && "FAILED".equals(existing.getStatus())) {
            log.warn("🔁 Retrying FAILED interest posting for account {}", accountId);
            postingRepository.delete(existing);
        }

        BigDecimal balanceBefore = account.getCurrentBalanceAmount();

        // ✅ Balance validation
        if (balanceBefore == null || balanceBefore.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("⚠️ Skipping account {} due to invalid balance", accountId);
            return false;
        }

        // ✅ Interest from PRODUCT
        if (account.getSavingsProduct() == null ||
                account.getSavingsProduct().getInterestRatePercent() == null) {

            throw new RuntimeException("Interest rate missing for account " + accountId);
        }

        BigDecimal annualRate =
                account.getSavingsProduct().getInterestRatePercent();

        // 1. Calculate interest
        BigDecimal interestAmount =
                calculationService.calculateMonthlyInterest(balanceBefore, annualRate);

        BigDecimal balanceAfter = balanceBefore.add(interestAmount);

        // 2. Update account balance
        account.setCurrentBalanceAmount(balanceAfter);
        accountRepository.save(account);

        // 3. Save SUCCESS record
        InterestPostingRecord record = InterestPostingRecord.builder()
                .savingsAccount(account)
                .interestAmount(interestAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .annualInterestRate(annualRate)
                .postingMonth(month)
                .postingYear(year)
                .status("SUCCESS")
                .build();

        postingRepository.save(record);

        // 4. Save transaction
        SavingsAccountTransaction txn = SavingsAccountTransaction.builder()
                .savingsAccount(account)
                .type(TransactionType.INTEREST)
                .status(TransactionStatus.SUCCESS)   // ✅ FIX ADDED
                .amount(interestAmount)
                .currency(Currency.INR)
                .paymentMode(PaymentMode.SYSTEM)
                .description("Monthly interest credited")
                .balanceBeforeTransaction(balanceBefore)
                .balanceAfterTransaction(balanceAfter)
                .interestPosting(interestAmount)
                .interestPostedAt(LocalDateTime.now())
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(txn);

        log.info("✅ Interest ₹{} posted to account {}", interestAmount, accountId);

        return true;
    }

    // ================= HISTORY BY ACCOUNT =================
    @Override
    public List<InterestPostingResponseDTO> getHistoryForAccount(Long accountId) {

        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountId));

        return postingRepository
                .findBySavingsAccount_IdOrderByPostingYearDescPostingMonthDesc(accountId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ================= HISTORY BY PERIOD =================
    @Override
    public List<InterestPostingResponseDTO> getHistoryForPeriod(int month, int year) {

        return postingRepository
                .findByPostingMonthAndPostingYear(month, year)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ================= MAPPER =================
    private InterestPostingResponseDTO toDTO(InterestPostingRecord r) {
        return InterestPostingResponseDTO.builder()
                .id(r.getId())
                .accountId(r.getSavingsAccount().getId())
                .interestAmount(r.getInterestAmount())
                .balanceBefore(r.getBalanceBefore())
                .balanceAfter(r.getBalanceAfter())
                .annualInterestRate(r.getAnnualInterestRate())
                .postingMonth(r.getPostingMonth())
                .postingYear(r.getPostingYear())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .createdBy(r.getCreatedBy())
                .updatedBy(r.getUpdatedBy())
                .build();
    }
}