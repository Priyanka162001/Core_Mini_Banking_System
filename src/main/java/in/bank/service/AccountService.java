package in.bank.service;

import java.util.List;

import in.bank.dto.SavingsAccountResponseDTO;
import in.bank.entity.AccountOpeningRequest;
import in.bank.entity.SavingsAccount;

public interface AccountService {
    void createAccountAfterApproval(AccountOpeningRequest req);
    
    List<SavingsAccountResponseDTO> findByUserId(Long userId);

}