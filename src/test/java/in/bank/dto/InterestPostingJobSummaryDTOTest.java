package in.bank.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterestPostingJobSummaryDTO Tests")
class InterestPostingJobSummaryDTOTest {

    @Test
    @DisplayName("TC1: NoArgsConstructor creates empty DTO with null/default values")
    void testNoArgsConstructor() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO();
        
        assertThat(dto).isNotNull();
        assertThat(dto.getMonth()).isNull();
        assertThat(dto.getYear()).isNull();
        assertThat(dto.getTotalAccounts()).isEqualTo(0);
        assertThat(dto.getPosted()).isEqualTo(0);
        assertThat(dto.getSkipped()).isEqualTo(0);
        assertThat(dto.getFailed()).isEqualTo(0);
    }

    @Test
    @DisplayName("TC2: AllArgsConstructor creates DTO with all fields set correctly")
    void testAllArgsConstructor() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO(
            4, 2026, 1000, 850, 120, 30
        );
        
        assertThat(dto.getMonth()).isEqualTo(4);
        assertThat(dto.getYear()).isEqualTo(2026);
        assertThat(dto.getTotalAccounts()).isEqualTo(1000);
        assertThat(dto.getPosted()).isEqualTo(850);
        assertThat(dto.getSkipped()).isEqualTo(120);
        assertThat(dto.getFailed()).isEqualTo(30);
    }

    @Test
    @DisplayName("TC3: All fields with zero values")
    void testAllZeroValues() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO(0, 0, 0, 0, 0, 0);
        
        assertThat(dto.getMonth()).isEqualTo(0);
        assertThat(dto.getYear()).isEqualTo(0);
        assertThat(dto.getTotalAccounts()).isEqualTo(0);
        assertThat(dto.getPosted()).isEqualTo(0);
        assertThat(dto.getSkipped()).isEqualTo(0);
        assertThat(dto.getFailed()).isEqualTo(0);
    }

    @Test
    @DisplayName("TC4: Summary with all accounts posted successfully")
    void testAllAccountsPosted() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO(
            1, 2026, 500, 500, 0, 0
        );
        
        assertThat(dto.getTotalAccounts()).isEqualTo(500);
        assertThat(dto.getPosted()).isEqualTo(500);
        assertThat(dto.getSkipped()).isEqualTo(0);
        assertThat(dto.getFailed()).isEqualTo(0);
        assertThat(dto.getPosted() + dto.getSkipped() + dto.getFailed())
            .isEqualTo(dto.getTotalAccounts());
    }

    @Test
    @DisplayName("TC5: Summary with all accounts skipped")
    void testAllAccountsSkipped() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO(
            2, 2026, 300, 0, 300, 0
        );
        
        assertThat(dto.getTotalAccounts()).isEqualTo(300);
        assertThat(dto.getPosted()).isEqualTo(0);
        assertThat(dto.getSkipped()).isEqualTo(300);
        assertThat(dto.getFailed()).isEqualTo(0);
        assertThat(dto.getPosted() + dto.getSkipped() + dto.getFailed())
            .isEqualTo(dto.getTotalAccounts());
    }

    @Test
    @DisplayName("TC6: Summary with all accounts failed")
    void testAllAccountsFailed() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO(
            3, 2026, 200, 0, 0, 200
        );
        
        assertThat(dto.getTotalAccounts()).isEqualTo(200);
        assertThat(dto.getPosted()).isEqualTo(0);
        assertThat(dto.getSkipped()).isEqualTo(0);
        assertThat(dto.getFailed()).isEqualTo(200);
        assertThat(dto.getPosted() + dto.getSkipped() + dto.getFailed())
            .isEqualTo(dto.getTotalAccounts());
    }

    @Test
    @DisplayName("TC7: Summary with mixed results")
    void testMixedResults() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO(
            12, 2025, 1000, 750, 200, 50
        );
        
        assertThat(dto.getMonth()).isEqualTo(12);
        assertThat(dto.getYear()).isEqualTo(2025);
        assertThat(dto.getTotalAccounts()).isEqualTo(1000);
        assertThat(dto.getPosted()).isEqualTo(750);
        assertThat(dto.getSkipped()).isEqualTo(200);
        assertThat(dto.getFailed()).isEqualTo(50);
        
        // Verify sum matches total
        assertThat(dto.getPosted() + dto.getSkipped() + dto.getFailed())
            .isEqualTo(dto.getTotalAccounts());
    }

    @Test
    @DisplayName("TC8: Large numbers work correctly")
    void testLargeNumbers() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO(
            6, 2030, 1000000, 750000, 200000, 50000
        );
        
        assertThat(dto.getTotalAccounts()).isEqualTo(1000000);
        assertThat(dto.getPosted()).isEqualTo(750000);
        assertThat(dto.getSkipped()).isEqualTo(200000);
        assertThat(dto.getFailed()).isEqualTo(50000);
    }

    @Test
    @DisplayName("TC9: Different month values work")
    void testDifferentMonths() {
        InterestPostingJobSummaryDTO january = new InterestPostingJobSummaryDTO(1, 2026, 100, 100, 0, 0);
        InterestPostingJobSummaryDTO june = new InterestPostingJobSummaryDTO(6, 2026, 100, 100, 0, 0);
        InterestPostingJobSummaryDTO december = new InterestPostingJobSummaryDTO(12, 2026, 100, 100, 0, 0);
        
        assertThat(january.getMonth()).isEqualTo(1);
        assertThat(june.getMonth()).isEqualTo(6);
        assertThat(december.getMonth()).isEqualTo(12);
    }

    @Test
    @DisplayName("TC10: Different year values work")
    void testDifferentYears() {
        InterestPostingJobSummaryDTO year2024 = new InterestPostingJobSummaryDTO(1, 2024, 100, 100, 0, 0);
        InterestPostingJobSummaryDTO year2025 = new InterestPostingJobSummaryDTO(1, 2025, 100, 100, 0, 0);
        InterestPostingJobSummaryDTO year2026 = new InterestPostingJobSummaryDTO(1, 2026, 100, 100, 0, 0);
        
        assertThat(year2024.getYear()).isEqualTo(2024);
        assertThat(year2025.getYear()).isEqualTo(2025);
        assertThat(year2026.getYear()).isEqualTo(2026);
    }

    @Test
    @DisplayName("TC11: Negative numbers for edge cases")
    void testNegativeNumbers() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO(
            -1, -2026, -100, -50, -30, -20
        );
        
        assertThat(dto.getMonth()).isEqualTo(-1);
        assertThat(dto.getYear()).isEqualTo(-2026);
        assertThat(dto.getTotalAccounts()).isEqualTo(-100);
        assertThat(dto.getPosted()).isEqualTo(-50);
        assertThat(dto.getSkipped()).isEqualTo(-30);
        assertThat(dto.getFailed()).isEqualTo(-20);
    }

    @Test
    @DisplayName("TC12: Getters return correct values after creation")
    void testGetters() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO(5, 2026, 750, 600, 100, 50);
        
        // Verify all getters
        assertThat(dto.getMonth()).isEqualTo(5);
        assertThat(dto.getYear()).isEqualTo(2026);
        assertThat(dto.getTotalAccounts()).isEqualTo(750);
        assertThat(dto.getPosted()).isEqualTo(600);
        assertThat(dto.getSkipped()).isEqualTo(100);
        assertThat(dto.getFailed()).isEqualTo(50);
    }

    @Test
    @DisplayName("TC13: Success rate calculation scenario")
    void testSuccessRateScenario() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO(4, 2026, 1000, 850, 100, 50);
        
        double successRate = (double) dto.getPosted() / dto.getTotalAccounts() * 100;
        double failureRate = (double) dto.getFailed() / dto.getTotalAccounts() * 100;
        
        assertThat(successRate).isEqualTo(85.0);
        assertThat(failureRate).isEqualTo(5.0);
        assertThat(successRate + failureRate).isEqualTo(90.0);
    }

    @Test
    @DisplayName("TC14: Can create multiple DTOs for different periods")
    void testMultiplePeriods() {
        InterestPostingJobSummaryDTO jan2026 = new InterestPostingJobSummaryDTO(1, 2026, 500, 450, 40, 10);
        InterestPostingJobSummaryDTO feb2026 = new InterestPostingJobSummaryDTO(2, 2026, 500, 460, 35, 5);
        InterestPostingJobSummaryDTO mar2026 = new InterestPostingJobSummaryDTO(3, 2026, 500, 470, 25, 5);
        
        assertThat(jan2026.getPosted()).isEqualTo(450);
        assertThat(feb2026.getPosted()).isEqualTo(460);
        assertThat(mar2026.getPosted()).isEqualTo(470);
    }

    @Test
    @DisplayName("TC15: Verify int primitive types return 0 by default")
    void testDefaultValuesForPrimitives() {
        InterestPostingJobSummaryDTO dto = new InterestPostingJobSummaryDTO();
        
        // int primitives default to 0, not null
        assertThat(dto.getTotalAccounts()).isEqualTo(0);
        assertThat(dto.getPosted()).isEqualTo(0);
        assertThat(dto.getSkipped()).isEqualTo(0);
        assertThat(dto.getFailed()).isEqualTo(0);
    }
}