package in.bank.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountSnapshot Record Tests")
class AccountSnapshotTest {

    @Test
    @DisplayName("TC1: Constructor creates record with all fields set correctly")
    void testConstructor_AllFields() {
        AccountSnapshot snapshot = new AccountSnapshot(
            "ACC123456",
            new BigDecimal("50000.75"),
            new BigDecimal("4.5")
        );
        
        assertThat(snapshot.accountId()).isEqualTo("ACC123456");
        assertThat(snapshot.balance()).isEqualByComparingTo("50000.75");
        assertThat(snapshot.annualRate()).isEqualByComparingTo("4.5");
    }

    @Test
    @DisplayName("TC2: Record with zero balance works correctly")
    void testZeroBalance() {
        AccountSnapshot snapshot = new AccountSnapshot(
            "ACC123456",
            BigDecimal.ZERO,
            new BigDecimal("4.5")
        );
        
        assertThat(snapshot.balance()).isZero();
        assertThat(snapshot.accountId()).isEqualTo("ACC123456");
    }

    @Test
    @DisplayName("TC3: Record with null values works")
    void testNullValues() {
        AccountSnapshot snapshot = new AccountSnapshot(null, null, null);
        
        assertThat(snapshot.accountId()).isNull();
        assertThat(snapshot.balance()).isNull();
        assertThat(snapshot.annualRate()).isNull();
    }

    @Test
    @DisplayName("TC4: Record equality - same values are equal")
    void testRecordEquality_SameValues() {
        AccountSnapshot snapshot1 = new AccountSnapshot(
            "ACC123",
            new BigDecimal("1000.00"),
            new BigDecimal("5.0")
        );
        AccountSnapshot snapshot2 = new AccountSnapshot(
            "ACC123",
            new BigDecimal("1000.00"),
            new BigDecimal("5.0")
        );
        
        assertThat(snapshot1).isEqualTo(snapshot2);
        assertThat(snapshot1).hasSameHashCodeAs(snapshot2);
    }

    @Test
    @DisplayName("TC5: Record inequality - different values are not equal")
    void testRecordInequality_DifferentValues() {
        AccountSnapshot snapshot1 = new AccountSnapshot(
            "ACC123",
            new BigDecimal("1000.00"),
            new BigDecimal("5.0")
        );
        AccountSnapshot snapshot2 = new AccountSnapshot(
            "ACC456",
            new BigDecimal("1000.00"),
            new BigDecimal("5.0")
        );
        
        assertThat(snapshot1).isNotEqualTo(snapshot2);
    }

    @Test
    @DisplayName("TC6: Record inequality - different balance")
    void testRecordInequality_DifferentBalance() {
        AccountSnapshot snapshot1 = new AccountSnapshot(
            "ACC123",
            new BigDecimal("1000.00"),
            new BigDecimal("5.0")
        );
        AccountSnapshot snapshot2 = new AccountSnapshot(
            "ACC123",
            new BigDecimal("2000.00"),
            new BigDecimal("5.0")
        );
        
        assertThat(snapshot1).isNotEqualTo(snapshot2);
    }

    @Test
    @DisplayName("TC7: Record inequality - different annual rate")
    void testRecordInequality_DifferentRate() {
        AccountSnapshot snapshot1 = new AccountSnapshot(
            "ACC123",
            new BigDecimal("1000.00"),
            new BigDecimal("5.0")
        );
        AccountSnapshot snapshot2 = new AccountSnapshot(
            "ACC123",
            new BigDecimal("1000.00"),
            new BigDecimal("6.0")
        );
        
        assertThat(snapshot1).isNotEqualTo(snapshot2);
    }

    @Test
    @DisplayName("TC8: toString() contains all field values")
    void testToString() {
        AccountSnapshot snapshot = new AccountSnapshot(
            "ACC123",
            new BigDecimal("1000.50"),
            new BigDecimal("4.75")
        );
        
        String toString = snapshot.toString();
        assertThat(toString).contains("ACC123");
        assertThat(toString).contains("1000.5");
        assertThat(toString).contains("4.75");
        assertThat(toString).contains("AccountSnapshot");
    }

    @Test
    @DisplayName("TC9: Can use with BigDecimal operations")
    void testBigDecimalOperations() {
        AccountSnapshot snapshot = new AccountSnapshot(
            "ACC123",
            new BigDecimal("1000.00"),
            new BigDecimal("5.0")
        );
        
        BigDecimal interest = snapshot.balance()
            .multiply(snapshot.annualRate())
            .divide(new BigDecimal("100"));
        
        assertThat(interest).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("TC10: Different account ID formats work")
    void testDifferentAccountIdFormats() {
        AccountSnapshot snapshot1 = new AccountSnapshot("1234567890", new BigDecimal("100"), new BigDecimal("4.5"));
        AccountSnapshot snapshot2 = new AccountSnapshot("ABC-DEF-123", new BigDecimal("200"), new BigDecimal("4.5"));
        AccountSnapshot snapshot3 = new AccountSnapshot("SAV-2024-001", new BigDecimal("300"), new BigDecimal("4.5"));
        
        assertThat(snapshot1.accountId()).isEqualTo("1234567890");
        assertThat(snapshot2.accountId()).isEqualTo("ABC-DEF-123");
        assertThat(snapshot3.accountId()).isEqualTo("SAV-2024-001");
    }

    @Test
    @DisplayName("TC11: Negative balance works (overdraft scenario)")
    void testNegativeBalance() {
        AccountSnapshot snapshot = new AccountSnapshot(
            "ACC123",
            new BigDecimal("-500.00"),
            new BigDecimal("4.5")
        );
        
        assertThat(snapshot.balance()).isNegative();
        assertThat(snapshot.balance()).isEqualByComparingTo("-500.00");
    }

    @Test
    @DisplayName("TC12: Very large balance works")
    void testLargeBalance() {
        AccountSnapshot snapshot = new AccountSnapshot(
            "ACC123",
            new BigDecimal("999999999999.99"),
            new BigDecimal("8.5")
        );
        
        assertThat(snapshot.balance()).isEqualByComparingTo("999999999999.99");
    }

    @Test
    @DisplayName("TC13: Small interest rate works")
    void testSmallInterestRate() {
        AccountSnapshot snapshot = new AccountSnapshot(
            "ACC123",
            new BigDecimal("10000.00"),
            new BigDecimal("0.01")
        );
        
        BigDecimal interest = snapshot.balance()
            .multiply(snapshot.annualRate())
            .divide(new BigDecimal("100"));
        
        assertThat(interest).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("TC14: Record deconstruction pattern")
    void testRecordDeconstruction() {
        AccountSnapshot snapshot = new AccountSnapshot(
            "ACC123",
            new BigDecimal("15000.00"),
            new BigDecimal("6.5")
        );
        
        // Record deconstruction (Java 16+)
        String accountId = snapshot.accountId();
        BigDecimal balance = snapshot.balance();
        BigDecimal rate = snapshot.annualRate();
        
        assertThat(accountId).isEqualTo("ACC123");
        assertThat(balance).isEqualByComparingTo("15000.00");
        assertThat(rate).isEqualByComparingTo("6.5");
    }

    @Test
    @DisplayName("TC15: Can be used as immutable data carrier")
    void testImmutability() {
        AccountSnapshot snapshot = new AccountSnapshot(
            "ACC123",
            new BigDecimal("1000.00"),
            new BigDecimal("4.5")
        );
        
        // Record fields cannot be modified (no setters)
        // This verifies the record is immutable
        assertThat(snapshot.getClass().getDeclaredMethods())
            .noneMatch(m -> m.getName().startsWith("set"));
    }
}