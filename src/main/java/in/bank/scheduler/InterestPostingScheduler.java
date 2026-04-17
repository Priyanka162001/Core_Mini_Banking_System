package in.bank.scheduler;

import in.bank.service.InterestPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterestPostingScheduler {

    private final InterestPostingService interestPostingService;

    // Runs on 1st of every month at midnight
    @Scheduled(cron = "0 0 0 1 * ?", zone = "Asia/Kolkata")
    public void runMonthlyInterestPosting() {

        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        log.info("Starting interest posting for {}/{}", month, year);

        InterestPostingService.JobSummary summary =
                interestPostingService.postInterestForPeriod(month, year);

        log.info("Interest posting completed: {}", summary);
    }
}