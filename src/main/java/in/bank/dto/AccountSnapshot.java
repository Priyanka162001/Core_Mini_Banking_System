package in.bank.dto;


import java.math.BigDecimal;

public record AccountSnapshot(
    String accountId,      // maps to SavingsAccount.accountNumber
    BigDecimal balance,    // maps to SavingsAccount.currentBalanceAmount
    BigDecimal annualRate  // maps to SavingsProduct.annualInterestRate
) {}