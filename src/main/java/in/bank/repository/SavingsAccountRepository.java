package in.bank.repository;

import in.bank.entity.AccountLifecycleStatus;
import in.bank.entity.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {

    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM SavingsAccount a WHERE a.id = :id")
    Optional<SavingsAccount> findByIdForUpdate(@Param("id") Long id);

    // ✅ FIXED — now actually uses the status parameter
    List<SavingsAccount> findByAccountStatus(AccountLifecycleStatus accountStatus);

    // ✅ Kept as-is — fetches active accounts with product eagerly loaded
    @Query("SELECT s FROM SavingsAccount s JOIN FETCH s.savingsProduct WHERE s.accountStatus = 'ACTIVE'")
    List<SavingsAccount> findAllActiveWithProduct();

    List<SavingsAccount> findByUser_Id(Long userId);

    Optional<SavingsAccount> findByAccountNumber(String accountNumber);

    boolean existsByUser_IdAndSavingsProduct_Id(Long userId, Long productId);
}