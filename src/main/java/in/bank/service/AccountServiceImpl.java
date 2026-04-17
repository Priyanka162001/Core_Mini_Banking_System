package in.bank.service;

import in.bank.dto.SavingsAccountRequestDTO;
import in.bank.dto.SavingsAccountResponseDTO;
import in.bank.entity.AccountOpeningRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private final SavingsAccountService savingsAccountService;

    // ✅ Triggered after admin approves an account request
    @Override
    public void createAccountAfterApproval(AccountOpeningRequest req) {

        SavingsAccountRequestDTO dto = new SavingsAccountRequestDTO();
        dto.setUserId(req.getUserId());
        dto.setSavingsProductId(req.getProductId());
        dto.setOpeningBalance(req.getInitialDeposit());

        savingsAccountService.createAccount(dto);
    }

    // ✅ Delegates to SavingsAccountService
    @Override
    public List<SavingsAccountResponseDTO> findByUserId(Long userId) {
        return savingsAccountService.getAccountsByUserId(userId);
    }
}