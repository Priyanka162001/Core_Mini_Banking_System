package in.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import in.bank.entity.SavingsAccount;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {

    boolean existsByAccountNumber(String accountNumber);
}