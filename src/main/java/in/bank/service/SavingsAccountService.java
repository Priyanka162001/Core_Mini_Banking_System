package in.bank.service;

import in.bank.dto.AccountSnapshot;
import in.bank.dto.SavingsAccountRequestDTO;
import in.bank.dto.SavingsAccountResponseDTO;
import in.bank.entity.AccountLifecycleStatus;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;

public interface SavingsAccountService {
    Long createAccount(SavingsAccountRequestDTO request);

    SavingsAccountResponseDTO getById(Long id, UserDetails userDetails); // ✅ only this one

    List<SavingsAccountResponseDTO> getAll();
    List<SavingsAccountResponseDTO> getAccountsByUserId(Long userId);
    void freezeAccount(Long id);
    void closeAccount(Long id);
    void addInterest(String accountNumber, BigDecimal interest);
    List<AccountSnapshot> findAllActive();
    void createDefaultAccountForCustomer(Long customerId);
    void updateAccountStatus(Long id, AccountLifecycleStatus status);
}