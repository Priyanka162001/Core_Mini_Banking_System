package in.bank.repository;

import in.bank.entity.SavingsAccountTransaction;
import in.bank.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SavingsAccountTransactionRepository
        extends JpaRepository<SavingsAccountTransaction, Long> {

    List<SavingsAccountTransaction> findBySavingsAccount_Id(Long accountId);

    List<SavingsAccountTransaction> findBySavingsAccount_IdAndType(
            Long accountId, TransactionType type);

    List<SavingsAccountTransaction> findBySavingsAccount_IdAndCreatedAtBetween(
            Long accountId, LocalDateTime start, LocalDateTime end);

    // ✅ NEW — for getTransactionById
    Optional<SavingsAccountTransaction> findByIdAndSavingsAccount_Id(
            Long transactionId, Long accountId);
}