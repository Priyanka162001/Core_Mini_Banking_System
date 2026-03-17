package in.bank.service;

import in.bank.dto.SavingsAccountRequestDTO;
import in.bank.entity.AppUser;
import in.bank.entity.SavingsAccount;
import in.bank.entity.SavingsProduct;
import in.bank.entity.AccountLifecycleStatus;
import in.bank.repository.SavingsAccountRepository;
import in.bank.repository.SavingsProductRepository;
import in.bank.repository.UserRepository;
import in.bank.service.SavingsAccountService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SavingsAccountServiceImpl implements SavingsAccountService {

    private final SavingsAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SavingsProductRepository productRepository;

    // Create account
    @Override
    public Long createAccount(SavingsAccountRequestDTO request) {

        AppUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SavingsProduct product = productRepository.findById(request.getSavingsProductId())
                .orElseThrow(() -> new RuntimeException("Savings product not found"));

        String accountNumber = generateAccountNumber();

        SavingsAccount account = SavingsAccount.builder()
                .accountNumber(accountNumber)
                .user(user)
                .savingsProduct(product)
                .currentBalanceAmount(request.getOpeningBalance())
                .accountStatus(AccountLifecycleStatus.ACTIVE)
                .build();

        accountRepository.save(account);

        return account.getId();
    }

    // Get account by id
    @Override
    @Transactional(readOnly = true)
    public SavingsAccount getById(Long id) {

        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    // Get all accounts
    @Override
    @Transactional(readOnly = true)
    public List<SavingsAccount> getAll() {

        return accountRepository.findAll();
    }

    // Freeze account
    @Override
    public void freezeAccount(Long id) {

        SavingsAccount account = getById(id);

        if (account.getAccountStatus() == AccountLifecycleStatus.CLOSED) {
            throw new RuntimeException("Cannot freeze a closed account");
        }

        account.setAccountStatus(AccountLifecycleStatus.FROZEN);

        accountRepository.save(account);
    }

    // Close account
    @Override
    public void closeAccount(Long id) {

        SavingsAccount account = getById(id);

        if (account.getAccountStatus() == AccountLifecycleStatus.CLOSED) {
            throw new RuntimeException("Account already closed");
        }

        account.setAccountStatus(AccountLifecycleStatus.CLOSED);

        accountRepository.save(account);
    }

    // Generate account number
    private String generateAccountNumber() {

        return "SA-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}