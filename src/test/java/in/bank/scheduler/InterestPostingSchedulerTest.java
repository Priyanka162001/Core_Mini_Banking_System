package in.bank.scheduler;

import in.bank.service.InterestPostingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterestPostingScheduler Tests")
class InterestPostingSchedulerTest {

    @Mock
    private InterestPostingService interestPostingService;

    @InjectMocks
    private InterestPostingScheduler interestPostingScheduler;

    @Test
    @DisplayName("TC1: runMonthlyInterestPosting calls service with current month and year")
    void testRunMonthlyInterestPosting() {
        LocalDate now = LocalDate.now();
        int expectedMonth = now.getMonthValue();
        int expectedYear = now.getYear();
        
        InterestPostingService.JobSummary mockSummary = mock(InterestPostingService.JobSummary.class);
        when(interestPostingService.postInterestForPeriod(expectedMonth, expectedYear))
            .thenReturn(mockSummary);
        
        interestPostingScheduler.runMonthlyInterestPosting();
        
        verify(interestPostingService).postInterestForPeriod(expectedMonth, expectedYear);
    }

    @Test
    @DisplayName("TC2: Scheduler propagates exceptions from service")
    void testRunMonthlyInterestPosting_WhenServiceThrowsException() {
        LocalDate now = LocalDate.now();
        int expectedMonth = now.getMonthValue();
        int expectedYear = now.getYear();
        
        when(interestPostingService.postInterestForPeriod(expectedMonth, expectedYear))
            .thenThrow(new RuntimeException("Database error"));
        
        assertThatThrownBy(() -> interestPostingScheduler.runMonthlyInterestPosting())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database error");
        
        verify(interestPostingService).postInterestForPeriod(expectedMonth, expectedYear);
    }

    @Test
    @DisplayName("TC3: Scheduler uses correct cron expression")
    void testScheduledAnnotation() throws NoSuchMethodException {
        var method = InterestPostingScheduler.class.getMethod("runMonthlyInterestPosting");
        var scheduledAnnotation = method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
        
        assertThat(scheduledAnnotation).isNotNull();
        assertThat(scheduledAnnotation.cron()).isEqualTo("0 0 0 1 * ?");
        assertThat(scheduledAnnotation.zone()).isEqualTo("Asia/Kolkata");
    }
}