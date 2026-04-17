package in.bank.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SavingsAccountTransaction Entity Tests")
class SavingsAccountTransactionTest {

    private SavingsAccount savingsAccount;
    
    @BeforeEach
    void setUp() {
        // Create a mock SavingsAccount for relationship testing
        savingsAccount = SavingsAccount.builder()
                .id(1L)
                .build();
    }

    @Test
    @DisplayName("TC1: Builder creates transaction with all fields set correctly")
    void testBuilder_AllFields() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime interestDate = now.minusDays(5);

        SavingsAccountTransaction transaction = SavingsAccountTransaction.builder()
                .id(1L)
                .savingsAccount(savingsAccount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .amount(new BigDecimal("1000.50"))
                .currency(Currency.INR)
                .paymentMode(PaymentMode.UPI)
                .description("Salary deposit")
                .balanceBeforeTransaction(new BigDecimal("5000.00"))
                .balanceAfterTransaction(new BigDecimal("6000.50"))
                .interestPosting(new BigDecimal("50.25"))
                .interestPostedAt(interestDate)
                .transactionDate(now)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(100L)
                .updatedBy(100L)
                .build();

        assertThat(transaction.getId()).isEqualTo(1L);
        assertThat(transaction.getSavingsAccount()).isEqualTo(savingsAccount);
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(transaction.getAmount()).isEqualByComparingTo("1000.50");
        assertThat(transaction.getCurrency()).isEqualTo(Currency.INR);
        assertThat(transaction.getPaymentMode()).isEqualTo(PaymentMode.UPI);
        assertThat(transaction.getDescription()).isEqualTo("Salary deposit");
        assertThat(transaction.getBalanceBeforeTransaction()).isEqualByComparingTo("5000.00");
        assertThat(transaction.getBalanceAfterTransaction()).isEqualByComparingTo("6000.50");
        assertThat(transaction.getInterestPosting()).isEqualByComparingTo("50.25");
        assertThat(transaction.getInterestPostedAt()).isEqualTo(interestDate);
        assertThat(transaction.getTransactionDate()).isEqualTo(now);
        assertThat(transaction.getCreatedAt()).isEqualTo(now);
        assertThat(transaction.getUpdatedAt()).isEqualTo(now);
        assertThat(transaction.getCreatedBy()).isEqualTo(100L);
        assertThat(transaction.getUpdatedBy()).isEqualTo(100L);
    }

    @Test
    @DisplayName("TC2: NoArgsConstructor creates empty transaction")
    void testNoArgsConstructor() {
        SavingsAccountTransaction transaction = new SavingsAccountTransaction();
        assertThat(transaction).isNotNull();
        assertThat(transaction.getId()).isNull();
        assertThat(transaction.getAmount()).isNull();
        assertThat(transaction.getType()).isNull();
    }

    @Test
    @DisplayName("TC3: AllArgsConstructor sets all fields")
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();

        SavingsAccountTransaction transaction = new SavingsAccountTransaction(
                1L, savingsAccount, TransactionType.WITHDRAWAL, TransactionStatus.PENDING,
                new BigDecimal("500.00"), Currency.USD, PaymentMode.CARD,
                "ATM withdrawal", new BigDecimal("10000.00"), new BigDecimal("9500.00"),
                null, null, now, now, now, 200L, 200L
        );

        assertThat(transaction.getId()).isEqualTo(1L);
        assertThat(transaction.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(transaction.getAmount()).isEqualByComparingTo("500.00");
        assertThat(transaction.getCurrency()).isEqualTo(Currency.USD);
        assertThat(transaction.getPaymentMode()).isEqualTo(PaymentMode.CARD);
    }

    @Test
    @DisplayName("TC4: Setters update fields correctly")
    void testSetters() {
        SavingsAccountTransaction transaction = new SavingsAccountTransaction();
        LocalDateTime now = LocalDateTime.now();

        transaction.setId(2L);
        transaction.setSavingsAccount(savingsAccount);
        transaction.setType(TransactionType.DEPOSIT);  // Changed from TRANSFER to DEPOSIT
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setAmount(new BigDecimal("2500.75"));
        transaction.setCurrency(Currency.EUR);
        transaction.setPaymentMode(PaymentMode.NEFT);
        transaction.setDescription("Bill payment");
        transaction.setBalanceBeforeTransaction(new BigDecimal("15000.00"));
        transaction.setBalanceAfterTransaction(new BigDecimal("12499.25"));
        transaction.setInterestPosting(new BigDecimal("75.50"));
        transaction.setInterestPostedAt(now);
        transaction.setTransactionDate(now);
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        transaction.setCreatedBy(300L);
        transaction.setUpdatedBy(300L);

        assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);  // Changed from TRANSFER to DEPOSIT
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(transaction.getAmount()).isEqualByComparingTo("2500.75");
        assertThat(transaction.getCurrency()).isEqualTo(Currency.EUR);
        assertThat(transaction.getPaymentMode()).isEqualTo(PaymentMode.NEFT);
        assertThat(transaction.getDescription()).isEqualTo("Bill payment");
    }

    @Test
    @DisplayName("TC5: PrePersist sets transactionDate and createdAt when null")
    void testPrePersist_SetsTimestampsWhenNull() {
        SavingsAccountTransaction transaction = new SavingsAccountTransaction();
        
        // Verify null before prePersist
        assertThat(transaction.getTransactionDate()).isNull();
        assertThat(transaction.getCreatedAt()).isNull();
        
        // Call prePersist
        transaction.prePersist();
        
        // Verify timestamps are set
        assertThat(transaction.getTransactionDate()).isNotNull();
        assertThat(transaction.getCreatedAt()).isNotNull();
        assertThat(transaction.getTransactionDate()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(transaction.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("TC6: PrePersist does NOT override existing transactionDate")
    void testPrePersist_DoesNotOverrideExistingTransactionDate() {
        LocalDateTime existingDate = LocalDateTime.of(2024, 1, 1, 10, 30);
        SavingsAccountTransaction transaction = new SavingsAccountTransaction();
        transaction.setTransactionDate(existingDate);
        
        transaction.prePersist();
        
        // Should keep existing date, not override
        assertThat(transaction.getTransactionDate()).isEqualTo(existingDate);
        assertThat(transaction.getCreatedAt()).isNotNull(); // Only createdAt should be set
    }

    @Test
    @DisplayName("TC7: PrePersist does NOT override existing createdAt")
    void testPrePersist_DoesNotOverrideExistingCreatedAt() {
        LocalDateTime existingCreatedAt = LocalDateTime.of(2024, 1, 1, 10, 30);
        SavingsAccountTransaction transaction = new SavingsAccountTransaction();
        transaction.setCreatedAt(existingCreatedAt);
        
        transaction.prePersist();
        
        // Should keep existing createdAt
        assertThat(transaction.getCreatedAt()).isEqualTo(existingCreatedAt);
        assertThat(transaction.getTransactionDate()).isNotNull(); // transactionDate should be set
    }

    @Test
    @DisplayName("TC8: Transaction with zero amount")
    void testTransaction_ZeroAmount() {
        SavingsAccountTransaction transaction = SavingsAccountTransaction.builder()
                .amount(BigDecimal.ZERO)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .build();
        
        assertThat(transaction.getAmount()).isZero();
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    @DisplayName("TC9: Transaction with negative amount (withdrawal)")
    void testTransaction_NegativeAmount() {
        SavingsAccountTransaction transaction = SavingsAccountTransaction.builder()
                .amount(new BigDecimal("-200.00"))
                .type(TransactionType.WITHDRAWAL)
                .build();
        
        assertThat(transaction.getAmount()).isNegative();
        assertThat(transaction.getType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    @Test
    @DisplayName("TC10: Interest posting fields work correctly")
    void testInterestPosting() {
        LocalDateTime interestDate = LocalDateTime.now();
        SavingsAccountTransaction transaction = new SavingsAccountTransaction();
        
        transaction.setInterestPosting(new BigDecimal("125.75"));
        transaction.setInterestPostedAt(interestDate);
        
        assertThat(transaction.getInterestPosting()).isEqualByComparingTo("125.75");
        assertThat(transaction.getInterestPostedAt()).isEqualTo(interestDate);
    }

    @Test
    @DisplayName("TC11: Balance before and after transaction validation")
    void testBalanceBeforeAfterTransaction() {
        BigDecimal before = new BigDecimal("10000.00");
        BigDecimal amount = new BigDecimal("2500.00");
        BigDecimal after = before.add(amount);
        
        SavingsAccountTransaction transaction = SavingsAccountTransaction.builder()
                .balanceBeforeTransaction(before)
                .balanceAfterTransaction(after)
                .amount(amount)
                .build();
        
        assertThat(transaction.getBalanceAfterTransaction())
            .isEqualTo(transaction.getBalanceBeforeTransaction().add(transaction.getAmount()));
    }

    @Test
    @DisplayName("TC12: Enum values - TransactionType")
    void testTransactionTypeEnum() {
        SavingsAccountTransaction transaction = SavingsAccountTransaction.builder()
                .type(TransactionType.DEPOSIT)
                .build();
        
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        
        transaction.setType(TransactionType.WITHDRAWAL);
        assertThat(transaction.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        
        // TRANSFER doesn't exist, so testing only DEPOSIT and WITHDRAWAL
    }

    @Test
    @DisplayName("TC13: Enum values - TransactionStatus")
    void testTransactionStatusEnum() {
        SavingsAccountTransaction transaction = SavingsAccountTransaction.builder()
                .status(TransactionStatus.PENDING)
                .build();
        
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        
        transaction.setStatus(TransactionStatus.SUCCESS);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        
        transaction.setStatus(TransactionStatus.FAILED);
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    @DisplayName("TC14: Enum values - PaymentMode")
    void testPaymentModeEnum() {
        SavingsAccountTransaction transaction = new SavingsAccountTransaction();
        
        transaction.setPaymentMode(PaymentMode.CASH);
        assertThat(transaction.getPaymentMode()).isEqualTo(PaymentMode.CASH);
        
        transaction.setPaymentMode(PaymentMode.UPI);
        assertThat(transaction.getPaymentMode()).isEqualTo(PaymentMode.UPI);
        
        transaction.setPaymentMode(PaymentMode.NEFT);
        assertThat(transaction.getPaymentMode()).isEqualTo(PaymentMode.NEFT);
        
        transaction.setPaymentMode(PaymentMode.RTGS);
        assertThat(transaction.getPaymentMode()).isEqualTo(PaymentMode.RTGS);
        
        transaction.setPaymentMode(PaymentMode.IMPS);
        assertThat(transaction.getPaymentMode()).isEqualTo(PaymentMode.IMPS);
        
        transaction.setPaymentMode(PaymentMode.CARD);
        assertThat(transaction.getPaymentMode()).isEqualTo(PaymentMode.CARD);
        
        transaction.setPaymentMode(PaymentMode.SYSTEM);
        assertThat(transaction.getPaymentMode()).isEqualTo(PaymentMode.SYSTEM);
    }

    @Test
    @DisplayName("TC15: JPA annotations presence")
    void testJpaAnnotations() {
        // Verify class has Entity annotation
        assertThat(SavingsAccountTransaction.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(SavingsAccountTransaction.class.isAnnotationPresent(Table.class)).isTrue();
        assertThat(SavingsAccountTransaction.class.isAnnotationPresent(EntityListeners.class)).isTrue();
    }

    @Test
    @DisplayName("TC16: Audit fields (createdBy, updatedBy)")
    void testAuditFields() {
        SavingsAccountTransaction transaction = SavingsAccountTransaction.builder()
                .createdBy(1L)
                .updatedBy(2L)
                .build();
        
        assertThat(transaction.getCreatedBy()).isEqualTo(1L);
        assertThat(transaction.getUpdatedBy()).isEqualTo(2L);
        
        transaction.setCreatedBy(10L);
        transaction.setUpdatedBy(20L);
        
        assertThat(transaction.getCreatedBy()).isEqualTo(10L);
        assertThat(transaction.getUpdatedBy()).isEqualTo(20L);
    }
}