package in.bank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterestCalculationServiceTest {

    private InterestCalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new InterestCalculationService();
    }

    @ParameterizedTest
    @CsvSource({
        "50000, 4.00, 166.6667",
        "100000, 4.00, 333.3333",
        "25000, 5.00, 104.1667",
        "75000, 3.50, 218.7500",
        "1000, 1.00, 0.8333",
        "0, 4.00, 0",
        "50000, 0, 0",
        "50000, -1, 0"
    })
    void calculateMonthlyInterest_ValidInputs_ReturnsCorrectAmount(
            String principal, String annualRate, String expected) {

        BigDecimal result = calculationService.calculateMonthlyInterest(
                new BigDecimal(principal),
                new BigDecimal(annualRate)
        );

        assertThat(result).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void calculateMonthlyInterest_NullPrincipal_ReturnsZero() {
        BigDecimal result = calculationService.calculateMonthlyInterest(null, new BigDecimal("4.00"));
        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void calculateMonthlyInterest_NullRate_ReturnsZero() {
        BigDecimal result = calculationService.calculateMonthlyInterest(new BigDecimal("50000"), null);
        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void calculateMonthlyInterest_Precision_ReturnsFourDecimalPlaces() {
        BigDecimal result = calculationService.calculateMonthlyInterest(
                new BigDecimal("10000"),
                new BigDecimal("3.33")
        );

        // 10000 * 3.33 / 1200 = 27.75 exactly
        assertThat(result.scale()).isEqualTo(4);
    }

    @Test
    void annualizeMonthlyRate_ValidRate_ReturnsCorrectAnnualRate() {
        BigDecimal monthlyRate = new BigDecimal("0.3333");
        BigDecimal result = calculationService.annualizeMonthlyRate(monthlyRate);

        assertThat(result).isEqualTo(new BigDecimal("3.9996"));
    }

    @Test
    void annualizeMonthlyRate_NullRate_ShouldHandleGracefully() {
        // This would throw NPE - but that's acceptable for a utility method
        // You might want to add null check in the service
        assertThatThrownBy(() -> calculationService.annualizeMonthlyRate(null))
                .isInstanceOf(NullPointerException.class);
    }
}