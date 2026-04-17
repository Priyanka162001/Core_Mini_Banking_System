package in.bank.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.YearMonth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InterestPostingRequestDTO Tests")
class InterestPostingRequestDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("TC1: Valid period passes validation")
    void testValidPeriod() {
        InterestPostingRequestDTO request = new InterestPostingRequestDTO(YearMonth.of(2026, 4));
        
        var violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("TC2: Null period fails validation")
    void testNullPeriod() {
        InterestPostingRequestDTO request = new InterestPostingRequestDTO(null);
        
        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().equals("Period must not be null"));
    }

    @Test
    @DisplayName("TC3: Record getter works correctly")
    void testRecordGetter() {
        YearMonth period = YearMonth.of(2026, 4);
        InterestPostingRequestDTO request = new InterestPostingRequestDTO(period);
        
        assertThat(request.period()).isEqualTo(period);
        assertThat(request.period().getYear()).isEqualTo(2026);
        assertThat(request.period().getMonthValue()).isEqualTo(4);
    }

    @Test
    @DisplayName("TC4: Record equality works correctly")
    void testRecordEquality() {
        InterestPostingRequestDTO request1 = new InterestPostingRequestDTO(YearMonth.of(2026, 4));
        InterestPostingRequestDTO request2 = new InterestPostingRequestDTO(YearMonth.of(2026, 4));
        InterestPostingRequestDTO request3 = new InterestPostingRequestDTO(YearMonth.of(2026, 5));
        
        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1).hasSameHashCodeAs(request2);
    }

    @Test
    @DisplayName("TC5: Record toString() contains field values")
    void testRecordToString() {
        InterestPostingRequestDTO request = new InterestPostingRequestDTO(YearMonth.of(2026, 4));
        
        assertThat(request.toString()).contains("2026-04");
        assertThat(request.toString()).contains("InterestPostingRequestDTO");
    }

    @Test
    @DisplayName("TC6: Different YearMonth values work")
    void testDifferentPeriods() {
        InterestPostingRequestDTO jan2024 = new InterestPostingRequestDTO(YearMonth.of(2024, 1));
        InterestPostingRequestDTO dec2025 = new InterestPostingRequestDTO(YearMonth.of(2025, 12));
        
        assertThat(jan2024.period().getYear()).isEqualTo(2024);
        assertThat(jan2024.period().getMonthValue()).isEqualTo(1);
        
        assertThat(dec2025.period().getYear()).isEqualTo(2025);
        assertThat(dec2025.period().getMonthValue()).isEqualTo(12);
    }

    @Test
    @DisplayName("TC7: First and last month of year work correctly")
    void testFirstAndLastMonth() {
        InterestPostingRequestDTO january = new InterestPostingRequestDTO(YearMonth.of(2026, 1));
        InterestPostingRequestDTO december = new InterestPostingRequestDTO(YearMonth.of(2026, 12));
        
        assertThat(january.period().getMonthValue()).isEqualTo(1);
        assertThat(december.period().getMonthValue()).isEqualTo(12);
    }

    @Test
    @DisplayName("TC8: Leap year February works correctly")
    void testLeapYearFebruary() {
        InterestPostingRequestDTO leapYearFeb = new InterestPostingRequestDTO(YearMonth.of(2024, 2));
        InterestPostingRequestDTO nonLeapYearFeb = new InterestPostingRequestDTO(YearMonth.of(2023, 2));
        
        assertThat(leapYearFeb.period().lengthOfMonth()).isEqualTo(29);
        assertThat(nonLeapYearFeb.period().lengthOfMonth()).isEqualTo(28);
    }

    @Test
    @DisplayName("TC9: @Schema annotation is present")
    void testSchemaAnnotation() {
        assertThat(InterestPostingRequestDTO.class.isAnnotationPresent(io.swagger.v3.oas.annotations.media.Schema.class))
            .isTrue();
    }

    @Test
    @DisplayName("TC10: Can create request with current month")
    void testCurrentMonth() {
        YearMonth currentMonth = YearMonth.now();
        InterestPostingRequestDTO request = new InterestPostingRequestDTO(currentMonth);
        
        assertThat(request.period()).isEqualTo(currentMonth);
    }

    @Test
    @DisplayName("TC11: Can parse from string using YearMonth.parse()")
    void testParseFromString() {
        YearMonth parsed = YearMonth.parse("2026-04");
        InterestPostingRequestDTO request = new InterestPostingRequestDTO(parsed);
        
        assertThat(request.period().getYear()).isEqualTo(2026);
        assertThat(request.period().getMonthValue()).isEqualTo(4);
    }

    @Test
    @DisplayName("TC12: Invalid format throws exception")
    void testInvalidFormat() {
        assertThatThrownBy(() -> YearMonth.parse("2026/04"))
            .isInstanceOf(java.time.format.DateTimeParseException.class);
            
        assertThatThrownBy(() -> YearMonth.parse("invalid"))
            .isInstanceOf(java.time.format.DateTimeParseException.class);
    }
}