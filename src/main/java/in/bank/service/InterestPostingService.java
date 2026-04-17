package in.bank.service;

import in.bank.dto.InterestPostingResponseDTO;
import java.util.List;

public interface InterestPostingService {

    JobSummary postInterestForPeriod(int month, int year);

    List<InterestPostingResponseDTO> getHistoryForAccount(Long accountId);

    List<InterestPostingResponseDTO> getHistoryForPeriod(int month, int year);

    // ✅ JobSummary lives here in the interface — controller imports it from here
    record JobSummary(
        int month,
        int year,
        int totalAccounts,
        int posted,
        int skipped,
        int failed
    ) {}
}