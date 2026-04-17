package in.bank.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

@DisplayName("InterestApplicationFrequency Enum Tests")
class InterestApplicationFrequencyTest {

    @Test
    @DisplayName("TC1: fromValue returns correct enum for valid values")
    void testFromValue_ValidValues() {
        assertThat(InterestApplicationFrequency.fromValue("MONTHLY"))
            .isEqualTo(InterestApplicationFrequency.MONTHLY);
        assertThat(InterestApplicationFrequency.fromValue("monthly"))
            .isEqualTo(InterestApplicationFrequency.MONTHLY);
        assertThat(InterestApplicationFrequency.fromValue("QUARTERLY"))
            .isEqualTo(InterestApplicationFrequency.QUARTERLY);
        assertThat(InterestApplicationFrequency.fromValue("quarterly"))
            .isEqualTo(InterestApplicationFrequency.QUARTERLY);
        assertThat(InterestApplicationFrequency.fromValue("YEARLY"))
            .isEqualTo(InterestApplicationFrequency.YEARLY);
        assertThat(InterestApplicationFrequency.fromValue("yearly"))
            .isEqualTo(InterestApplicationFrequency.YEARLY);
    }

    @Test
    @DisplayName("TC2: fromValue throws exception for invalid values")
    void testFromValue_InvalidValues() {
        assertThatThrownBy(() -> InterestApplicationFrequency.fromValue("INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid interestApplicationFrequencyCode")
            .hasMessageContaining("MONTHLY, QUARTERLY, YEARLY");
            
        assertThatThrownBy(() -> InterestApplicationFrequency.fromValue("WEEKLY"))
            .isInstanceOf(IllegalArgumentException.class);
            
        assertThatThrownBy(() -> InterestApplicationFrequency.fromValue(null))
            .isInstanceOf(IllegalArgumentException.class);
            
        assertThatThrownBy(() -> InterestApplicationFrequency.fromValue(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TC3: Enum values work normally")
    void testEnumValues() {
        assertThat(InterestApplicationFrequency.values()).containsExactly(
            InterestApplicationFrequency.MONTHLY,
            InterestApplicationFrequency.QUARTERLY,
            InterestApplicationFrequency.YEARLY
        );
        
        assertThat(InterestApplicationFrequency.MONTHLY.name()).isEqualTo("MONTHLY");
        assertThat(InterestApplicationFrequency.QUARTERLY.ordinal()).isEqualTo(1);
    }
}