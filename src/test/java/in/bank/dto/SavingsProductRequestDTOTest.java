package in.bank.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import in.bank.entity.InterestApplicationFrequency;
import in.bank.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SavingsProductRequestDTO Tests")
class SavingsProductRequestDTOTest {

    private Validator validator;
    private SavingsProductRequestDTO request;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        request = new SavingsProductRequestDTO();
    }

    private SavingsProductRequestDTO createValidRequest() {
        SavingsProductRequestDTO dto = new SavingsProductRequestDTO();
        dto.setProductCode("SAV001");
        dto.setProductName("Premium Savings Account");
        dto.setInterestRatePercent(new BigDecimal("5.50"));
        dto.setMinimumOpeningBalanceAmount(new BigDecimal("1000.00"));
        dto.setMinimumMaintainingBalanceAmount(new BigDecimal("500.00"));
        dto.setInterestApplicationFrequencyCode(InterestApplicationFrequency.MONTHLY);
        dto.setProductStatus(ProductStatus.ACTIVE);
        dto.setExpiryDate(LocalDate.now().plusYears(1));
        dto.setMinAge(18);
        dto.setMaxAge(60);
        return dto;
    }

    // ================= PRODUCT DETAILS TESTS =================
    @Test
    @DisplayName("TC1: Valid product passes all validations")
    void testValidProduct() {
        SavingsProductRequestDTO validRequest = createValidRequest();
        var violations = validator.validate(validRequest);
        assertThat(violations).isEmpty();
    }

    // ✅ NEW: Parameterized test for multiple invalid product codes
    @ParameterizedTest
    @DisplayName("TC2: Multiple invalid product codes fail validation")
    @ValueSource(strings = {"", "   ", " ", "\t", "\n"})
    void testProductCode_InvalidValues(String invalidCode) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setProductCode(invalidCode);
        
        var violations = validator.validate(validRequest);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Product code cannot be blank"));
    }

    @Test
    @DisplayName("TC3: Product code max size 50 characters")
    void testProductCode_MaxSize() {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setProductCode("A".repeat(50));
        var violations = validator.validate(validRequest);
        assertThat(violations).isEmpty();
        
        validRequest.setProductCode("A".repeat(51));
        violations = validator.validate(validRequest);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("cannot exceed 50 characters"));
    }

    // ✅ NEW: Parameterized test for product name size
    @ParameterizedTest
    @DisplayName("TC4: Product name validation for different lengths")
    @CsvSource({
        "AB, false",           // Too short - invalid
        "ABC, true",           // Min valid
        "A_valid_name, true"   // Valid
    })
    void testProductName_SizeValidation(String productName, boolean isValid) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        
        // Handle special cases for repeat strings
        if (productName.equals("A_valid_name")) {
            validRequest.setProductName("A_valid_name");
        } else {
            validRequest.setProductName(productName);
        }
        
        var violations = validator.validate(validRequest);
        if (isValid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("between 3 and 100"));
        }
    }

    // ================= INTEREST RATE TESTS =================
    // ✅ NEW: Parameterized test for interest rate range
    @ParameterizedTest
    @DisplayName("TC5: Interest rate range validation")
    @CsvSource({
        "0.00, false",   // Below min - invalid
        "0.01, true",    // Min valid
        "5.50, true",    // Valid
        "20.00, true",   // Max valid
        "20.01, false"   // Above max - invalid
    })
    void testInterestRate_RangeValidation(String rate, boolean isValid) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setInterestRatePercent(new BigDecimal(rate));
        
        var violations = validator.validate(validRequest);
        if (isValid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations).isNotEmpty();
        }
    }

    @Test
    @DisplayName("TC6: Interest rate cannot be null")
    void testInterestRate_NotNull() {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setInterestRatePercent(null);
        
        var violations = validator.validate(validRequest);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Interest rate cannot be null"));
    }

    // ================= BALANCE DETAILS TESTS =================
    // ✅ NEW: Parameterized test for balance validation
    @ParameterizedTest
    @DisplayName("TC7: Minimum opening balance validation")
    @ValueSource(strings = {"-100", "-1", "-0.01"})
    void testMinOpeningBalance_NegativeValues(String negativeBalance) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setMinimumOpeningBalanceAmount(new BigDecimal(negativeBalance));
        
        var violations = validator.validate(validRequest);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("must be zero or more"));
    }

    // ✅ NEW: Parameterized test for balance cross-validation
    @ParameterizedTest
    @DisplayName("TC9: Opening balance vs maintaining balance validation")
    @CsvSource({
        "1000, 500, true",   // Opening > Maintaining - valid
        "500, 500, true",    // Equal - valid
        "500, 1000, false"   // Opening < Maintaining - invalid
    })
    void testBalanceValid_CrossFieldValidation(String opening, String maintaining, boolean isValid) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setMinimumOpeningBalanceAmount(new BigDecimal(opening));
        validRequest.setMinimumMaintainingBalanceAmount(new BigDecimal(maintaining));
        
        var violations = validator.validate(validRequest);
        if (isValid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("Opening balance must be greater than or equal to maintaining balance"));
        }
    }

    // ================= EXPIRY DATE TESTS =================
    // ✅ NEW: Parameterized test for expiry date
    @ParameterizedTest
    @DisplayName("TC10: Expiry date future validation")
    @CsvSource({
        "1, true",   // Tomorrow - valid
        "30, true",  // 30 days later - valid
        "365, true", // 1 year later - valid
        "-1, false", // Yesterday - invalid
        "-30, false" // 30 days ago - invalid
    })
    void testExpiryDate_FutureValidation(int daysOffset, boolean isValid) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setExpiryDate(LocalDate.now().plusDays(daysOffset));
        
        var violations = validator.validate(validRequest);
        if (isValid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("must be in the future"));
        }
    }

 // ✅ NEW: Parameterized test for min age range
    @ParameterizedTest
    @DisplayName("TC12: Min age range validation")
    @CsvSource({
        "9, false",    // Below min - invalid
        "10, true",    // Min valid
        "18, true",    // Valid
        "50, true",    // Valid
        "99, true",    // Valid (99 < 100)
        "100, false",  // Equal to maxAge - invalid (must be < maxAge)
        "101, false"   // Above max - invalid
    })
    void testMinAge_RangeValidation(int minAge, boolean isValid) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setMaxAge(100);
        validRequest.setMinAge(minAge);
        
        var violations = validator.validate(validRequest);
        if (isValid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations).isNotEmpty();
        }
    }
    
 // ✅ NEW: Parameterized test for max age range
    @ParameterizedTest
    @DisplayName("TC13: Max age range validation")
    @CsvSource({
        "9, false",    // Below min - invalid
        "11, true",    // Min valid (must be > minAge which is 10)
        "60, true",    // Valid
        "100, true",   // Max valid
        "101, false"   // Above max - invalid
    })
    void testMaxAge_RangeValidation(int maxAge, boolean isValid) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setMinAge(10);  // minAge = 10
        validRequest.setMaxAge(maxAge);
        
        var violations = validator.validate(validRequest);
        if (isValid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations).isNotEmpty();
        }
    }

    // ✅ NEW: Parameterized test for age cross-validation
    @ParameterizedTest
    @DisplayName("TC14: Age cross-validation (min < max)")
    @CsvSource({
        "18, 60, true",   // min < max - valid
        "30, 30, false",  // min = max - invalid
        "60, 18, false"   // min > max - invalid
    })
    void testAgeValid_CrossFieldValidation(int minAge, int maxAge, boolean isValid) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setMinAge(minAge);
        validRequest.setMaxAge(maxAge);
        
        var violations = validator.validate(validRequest);
        if (isValid) {
            assertThat(violations).isEmpty();
        } else {
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getMessage().contains("Minimum age must be less than maximum age"));
        }
    }

    // ================= ENUM TESTS =================
    // ✅ NEW: Parameterized test for InterestApplicationFrequency enum
    @ParameterizedTest
    @DisplayName("TC15: All interest application frequencies work")
    @EnumSource(InterestApplicationFrequency.class)
    void testAllInterestApplicationFrequencies(InterestApplicationFrequency frequency) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setInterestApplicationFrequencyCode(frequency);
        
        assertThat(validRequest.getInterestApplicationFrequencyCode()).isEqualTo(frequency);
    }

    // ✅ NEW: Parameterized test for ProductStatus enum
    @ParameterizedTest
    @DisplayName("TC16: All product statuses work")
    @EnumSource(ProductStatus.class)
    void testAllProductStatuses(ProductStatus status) {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setProductStatus(status);
        
        assertThat(validRequest.getProductStatus()).isEqualTo(status);
    }

    // ================= NULL HANDLING TESTS =================
    @Test
    @DisplayName("TC17: Multiple validation violations at once")
    void testMultipleValidationViolations() {
        request.setProductCode("");
        request.setProductName("A");
        request.setInterestRatePercent(new BigDecimal("0.00"));
        request.setMinimumOpeningBalanceAmount(new BigDecimal("-100"));
        request.setMinimumMaintainingBalanceAmount(new BigDecimal("-50"));
        request.setInterestApplicationFrequencyCode(null);
        request.setProductStatus(null);
        request.setExpiryDate(LocalDate.now().minusDays(1));
        request.setMinAge(5);
        request.setMaxAge(200);
        
        var violations = validator.validate(request);
        assertThat(violations).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    @DisplayName("TC18: Getters and setters work correctly")
    void testGettersAndSetters() {
        SavingsProductRequestDTO dto = new SavingsProductRequestDTO();
        LocalDate expiryDate = LocalDate.now().plusYears(1);
        
        dto.setProductCode("TEST001");
        dto.setProductName("Test Product");
        dto.setInterestRatePercent(new BigDecimal("4.50"));
        dto.setMinimumOpeningBalanceAmount(new BigDecimal("5000"));
        dto.setMinimumMaintainingBalanceAmount(new BigDecimal("2000"));
        dto.setInterestApplicationFrequencyCode(InterestApplicationFrequency.QUARTERLY);
        dto.setProductStatus(ProductStatus.ACTIVE);
        dto.setExpiryDate(expiryDate);
        dto.setMinAge(21);
        dto.setMaxAge(65);
        
        assertThat(dto.getProductCode()).isEqualTo("TEST001");
        assertThat(dto.getProductName()).isEqualTo("Test Product");
        assertThat(dto.getInterestRatePercent()).isEqualByComparingTo("4.50");
        assertThat(dto.getMinimumOpeningBalanceAmount()).isEqualByComparingTo("5000");
        assertThat(dto.getMinimumMaintainingBalanceAmount()).isEqualByComparingTo("2000");
        assertThat(dto.getInterestApplicationFrequencyCode()).isEqualTo(InterestApplicationFrequency.QUARTERLY);
        assertThat(dto.getProductStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(dto.getExpiryDate()).isEqualTo(expiryDate);
        assertThat(dto.getMinAge()).isEqualTo(21);
        assertThat(dto.getMaxAge()).isEqualTo(65);
    }

    // ================= EDGE CASE TESTS =================
    @Test
    @DisplayName("TC19: Zero balance is allowed")
    void testZeroBalance() {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setMinimumOpeningBalanceAmount(BigDecimal.ZERO);
        validRequest.setMinimumMaintainingBalanceAmount(BigDecimal.ZERO);
        
        var violations = validator.validate(validRequest);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TC20: Age boundary values work")
    void testAgeBoundaryValues() {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setMinAge(10);
        validRequest.setMaxAge(100);
        
        var violations = validator.validate(validRequest);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TC21: Balance validation with null values (should pass)")
    void testBalanceValidationWithNulls() {
        SavingsProductRequestDTO dto = new SavingsProductRequestDTO();
        dto.setMinimumOpeningBalanceAmount(null);
        dto.setMinimumMaintainingBalanceAmount(null);
        
        assertThat(dto.isBalanceValid()).isTrue();
    }

    @Test
    @DisplayName("TC22: Age validation with null values (should pass)")
    void testAgeValidationWithNulls() {
        SavingsProductRequestDTO dto = new SavingsProductRequestDTO();
        dto.setMinAge(null);
        dto.setMaxAge(null);
        
        assertThat(dto.isAgeValid()).isTrue();
    }

    @Test
    @DisplayName("TC23: Interest rate with many decimal places")
    void testInterestRateWithManyDecimals() {
        SavingsProductRequestDTO validRequest = createValidRequest();
        validRequest.setInterestRatePercent(new BigDecimal("5.12345"));
        
        var violations = validator.validate(validRequest);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TC24: @Schema annotation is present")
    void testSchemaAnnotation() {
        assertThat(SavingsProductRequestDTO.class.isAnnotationPresent(io.swagger.v3.oas.annotations.media.Schema.class))
            .isTrue();
    }
    
    @Nested
    @DisplayName("Cross-field validation null branch tests")
    class CrossFieldNullBranchTests {
        
        @Test
        @DisplayName("Balance validation returns true when opening balance is null")
        void testBalanceValid_OpeningBalanceNull() {
            SavingsProductRequestDTO dto = new SavingsProductRequestDTO();
            dto.setMinimumOpeningBalanceAmount(null);
            dto.setMinimumMaintainingBalanceAmount(new BigDecimal("500"));
            assertThat(dto.isBalanceValid()).isTrue();
        }
        
        @Test
        @DisplayName("Balance validation returns true when maintaining balance is null")
        void testBalanceValid_MaintainingBalanceNull() {
            SavingsProductRequestDTO dto = new SavingsProductRequestDTO();
            dto.setMinimumOpeningBalanceAmount(new BigDecimal("1000"));
            dto.setMinimumMaintainingBalanceAmount(null);
            assertThat(dto.isBalanceValid()).isTrue();
        }
        
        @Test
        @DisplayName("Age validation returns true when minAge is null")
        void testAgeValid_MinAgeNull() {
            SavingsProductRequestDTO dto = new SavingsProductRequestDTO();
            dto.setMinAge(null);
            dto.setMaxAge(60);
            assertThat(dto.isAgeValid()).isTrue();
        }
        
        @Test
        @DisplayName("Age validation returns true when maxAge is null")
        void testAgeValid_MaxAgeNull() {
            SavingsProductRequestDTO dto = new SavingsProductRequestDTO();
            dto.setMinAge(18);
            dto.setMaxAge(null);
            assertThat(dto.isAgeValid()).isTrue();
        }
    }
}