package in.bank.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostingResult Enum Tests")
class PostingResultTest {

    @Test
    @DisplayName("TC1: PostingResult enum values exist")
    void testEnumValues() {
        assertThat(PostingResult.values()).containsExactly(
            PostingResult.POSTED,
            PostingResult.DUPLICATE,
            PostingResult.SKIPPED
        );
    }

    @Test
    @DisplayName("TC2: POSTED enum value works correctly")
    void testPostedValue() {
        PostingResult result = PostingResult.POSTED;
        assertThat(result.name()).isEqualTo("POSTED");
        assertThat(result.ordinal()).isEqualTo(0);
    }

    @Test
    @DisplayName("TC3: DUPLICATE enum value works correctly")
    void testDuplicateValue() {
        PostingResult result = PostingResult.DUPLICATE;
        assertThat(result.name()).isEqualTo("DUPLICATE");
        assertThat(result.ordinal()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC4: SKIPPED enum value works correctly")
    void testSkippedValue() {
        PostingResult result = PostingResult.SKIPPED;
        assertThat(result.name()).isEqualTo("SKIPPED");
        assertThat(result.ordinal()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC5: Can convert from String to PostingResult")
    void testValueOf() {
        assertThat(PostingResult.valueOf("POSTED")).isEqualTo(PostingResult.POSTED);
        assertThat(PostingResult.valueOf("DUPLICATE")).isEqualTo(PostingResult.DUPLICATE);
        assertThat(PostingResult.valueOf("SKIPPED")).isEqualTo(PostingResult.SKIPPED);
    }

    @Test
    @DisplayName("TC6: All enum values are unique")
    void testEnumValuesAreUnique() {
        PostingResult[] values = PostingResult.values();
        assertThat(values).doesNotHaveDuplicates();
    }
}