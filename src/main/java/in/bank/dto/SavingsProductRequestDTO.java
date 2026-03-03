package in.bank.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import in.bank.entity.InterestApplicationFrequency;
import in.bank.entity.ProductStatus;

@Getter
@Setter
public class SavingsProductRequestDTO {

    @NotBlank(message = "Product name cannot be blank")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
    private String productName;

    @NotNull(message = "Interest rate cannot be null")
    @DecimalMin(value = "0.01", inclusive = true, message = "Interest rate must be at least 0.01%")
    @DecimalMax(value = "50.00", inclusive = true, message = "Interest rate cannot exceed 50%")
    private BigDecimal interestRatePercent;

    @NotNull(message = "Minimum opening balance cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum opening balance must be zero or more")
    @DecimalMax(value = "10000000.0", inclusive = true, message = "Minimum opening balance is too high")
    private BigDecimal minimumOpeningBalanceAmount;

    @NotNull(message = "Minimum maintaining balance cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum maintaining balance must be zero or more")
    @DecimalMax(value = "10000000.0", inclusive = true, message = "Minimum maintaining balance is too high")
    private BigDecimal minimumMaintainingBalanceAmount;

    @NotNull(message = "Interest application frequency cannot be null")
    private InterestApplicationFrequency interestApplicationFrequencyCode;

    @NotNull(message = "Product status cannot be null")
    private ProductStatus productStatus;

    @NotNull(message = "Effective from date cannot be null")
    @FutureOrPresent(message = "Effective from date cannot be in the past")
    private LocalDate effectiveFromDate;

    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    // ================= CROSS FIELD VALIDATIONS =================

    @AssertTrue(message = "Expiry date must be after effective date")
    @JsonIgnore
    public boolean isExpiryDateValid() {
        if (expiryDate == null || effectiveFromDate == null) {
            return true;
        }
        return expiryDate.isAfter(effectiveFromDate);
    }

    @AssertTrue(message = "Opening balance must be greater than or equal to maintaining balance")
    @JsonIgnore
    public boolean isBalanceValid() {
        if (minimumOpeningBalanceAmount == null || minimumMaintainingBalanceAmount == null) {
            return true;
        }
        return minimumOpeningBalanceAmount.compareTo(minimumMaintainingBalanceAmount) >= 0;
    }
}