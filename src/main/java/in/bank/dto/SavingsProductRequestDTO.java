package in.bank.dto;

import jakarta.validation.constraints.*;
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

    // ================= PRODUCT DETAILS =================

    @NotBlank(message = "Product code cannot be blank")
    @Size(max = 50, message = "Product code cannot exceed 50 characters")
    private String productCode;

    @NotBlank(message = "Product name cannot be blank")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
    private String productName;

    // ================= INTEREST DETAILS =================

    @NotNull(message = "Interest rate cannot be null")
    @DecimalMin(value = "0.01", message = "Interest rate must be at least 0.01%")
    @DecimalMax(value = "20.00", message = "Interest rate cannot exceed 20%")
    private BigDecimal interestRatePercent;

    // ================= BALANCE DETAILS =================

    @NotNull(message = "Minimum opening balance cannot be null")
    @DecimalMin(value = "0.0", message = "Minimum opening balance must be zero or more")
    private BigDecimal minimumOpeningBalanceAmount;

    @NotNull(message = "Minimum maintaining balance cannot be null")
    @DecimalMin(value = "0.0", message = "Minimum maintaining balance must be zero or more")
    private BigDecimal minimumMaintainingBalanceAmount;

    // ================= INTEREST FREQUENCY =================

    @NotNull(message = "Interest application frequency cannot be null")
    private InterestApplicationFrequency interestApplicationFrequencyCode;

    // ================= PRODUCT STATUS =================

    @NotNull(message = "Product status cannot be null")
    private ProductStatus productStatus;

    // ================= DATE DETAILS =================

    @NotNull(message = "Expiry date cannot be null")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    // ================= AGE LIMITS =================

    @NotNull(message = "Minimum age cannot be null")
    @Min(value = 10, message = "Minimum age must be at least 10")
    @Max(value = 100, message = "Minimum age cannot exceed 100")
    private Integer minAge;

    @NotNull(message = "Maximum age cannot be null")
    @Min(value = 10, message = "Maximum age must be at least 10")
    @Max(value = 100, message = "Maximum age cannot exceed 100")
    private Integer maxAge;

    // ================= CROSS FIELD VALIDATIONS =================

    @AssertTrue(message = "Opening balance must be greater than or equal to maintaining balance")
    @JsonIgnore
    public boolean isBalanceValid() {
        if (minimumOpeningBalanceAmount == null || minimumMaintainingBalanceAmount == null) {
            return true;
        }
        return minimumOpeningBalanceAmount.compareTo(minimumMaintainingBalanceAmount) >= 0;
    }

    @AssertTrue(message = "Minimum age must be less than maximum age")
    @JsonIgnore
    public boolean isAgeValid() {
        if (minAge == null || maxAge == null) {
            return true;
        }
        return minAge < maxAge;
    }
}