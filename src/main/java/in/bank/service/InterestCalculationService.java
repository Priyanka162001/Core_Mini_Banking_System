package in.bank.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Pure interest calculation logic — no DB, no Spring dependencies.
 * Easy to unit test in isolation.
 *
 * Formula used:
 *   Monthly Interest = (Principal × Annual Rate) / 12
 *
 * Example:
 *   Balance      = ₹50,000
 *   Annual Rate  = 4% (stored as 4.00)
 *   Monthly Int  = (50000 × 4.00) / (100 × 12) = ₹166.67
 */
@Component
public class InterestCalculationService {

    private static final BigDecimal HUNDRED  = new BigDecimal("100");
    private static final BigDecimal TWELVE   = new BigDecimal("12");
    private static final int        SCALE    = 4;
    private static final RoundingMode MODE   = RoundingMode.HALF_UP;

    /**
     * Calculate one month's interest.
     *
     * @param principal  current account balance
     * @param annualRate annual interest rate as a percentage (e.g. 4.00 for 4%)
     * @return           interest amount rounded to 4 decimal places
     */
    public BigDecimal calculateMonthlyInterest(BigDecimal principal, BigDecimal annualRate) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // (principal × annualRate) / (100 × 12)
        return principal
                .multiply(annualRate, MathContext.DECIMAL128)
                .divide(HUNDRED.multiply(TWELVE), SCALE, MODE);
    }

    /**
     * Annualize a monthly rate for display/verification purposes.
     *
     * @param monthlyRate monthly rate as a percentage
     * @return            equivalent annual rate
     */
    public BigDecimal annualizeMonthlyRate(BigDecimal monthlyRate) {
        return monthlyRate.multiply(TWELVE).setScale(SCALE, MODE);
    }
}