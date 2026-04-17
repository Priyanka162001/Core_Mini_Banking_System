package in.bank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@AutoConfigureMockMvc
@SpringBootTest
@DisplayName("Main Application Tests")
class CoreMiniBankingSystem1ApplicationTests {
    
    @Test
    @DisplayName("Verify application main method runs")
    void testMainMethod() {
        CoreMiniBankingSystem1Application.main(new String[]{});
    }
}
