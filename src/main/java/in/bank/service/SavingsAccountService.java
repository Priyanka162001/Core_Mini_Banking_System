package in.bank.service;

import in.bank.dto.SavingsAccountRequestDTO;
import in.bank.entity.SavingsAccount;

import java.util.List;

public interface SavingsAccountService {

    Long createAccount(SavingsAccountRequestDTO request);

    SavingsAccount getById(Long id);

    List<SavingsAccount> getAll();

    void freezeAccount(Long id);

    void closeAccount(Long id);
}