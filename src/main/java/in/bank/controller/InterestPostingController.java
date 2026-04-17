package in.bank.controller;

import in.bank.dto.InterestPostingResponseDTO;
import in.bank.service.InterestPostingService;
import in.bank.service.InterestPostingService.JobSummary;  // ✅ from interface
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Order(8)
@RequestMapping("/api/v1/interest")
@RequiredArgsConstructor
@Tag(name = "Interest Posting", description = "Monthly interest calculation and posting APIs")
@SecurityRequirement(name = "bearerAuth")
public class InterestPostingController {

    private final InterestPostingService interestPostingService;  // ✅ interface, not Impl

    @Operation(
        summary     = "Run interest posting job",
        description = "🔐 ADMIN only | Manually trigger interest posting for a given month and year. " +
                      "Safe to call multiple times — already-posted accounts are skipped (idempotent)."
    )
    @PostMapping("/post")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> postInterest(
            @RequestParam int month,
            @RequestParam int year) {

        InterestPostingService.JobSummary summary = interestPostingService.postInterestForPeriod(month, year);

        String message;

        if (summary.posted() > 0 && summary.failed() == 0) {
            message = "✅ Interest posted successfully for " + month + "/" + year;
        } else if (summary.skipped() == summary.totalAccounts()) {
            message = "⏭ Interest already posted for " + month + "/" + year + " — no accounts updated";
        } else if (summary.failed() > 0 && summary.posted() == 0) {
            message = "❌ Interest posting failed for all accounts for " + month + "/" + year;
        } else {
            message = "⚠️ Interest posting partially completed for " + month + "/" + year;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", message);
        response.put("month", summary.month());
        response.put("year", summary.year());
        response.put("totalAccounts", summary.totalAccounts());
        response.put("posted", summary.posted());
        response.put("skipped", summary.skipped());
        response.put("failed", summary.failed());

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary     = "Get interest history for an account",
        description = "🔐 ADMIN only | Returns all interest postings for a given account, newest first."
    )
    @GetMapping("/history/account/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InterestPostingResponseDTO>> getAccountHistory(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(interestPostingService.getHistoryForAccount(accountId));
    }

    @Operation(
        summary     = "Get all interest postings for a period",
        description = "🔐 ADMIN only | Returns all accounts that received interest in a given month/year."
    )
    @GetMapping("/history/period")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InterestPostingResponseDTO>> getPeriodHistory(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(interestPostingService.getHistoryForPeriod(month, year));
    }

    @Operation(
        summary     = "Get my interest posting history",
        description = "🔐 USER only | Returns interest posting history for the given account."
    )
    @GetMapping("/my-history/{accountId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<InterestPostingResponseDTO>> getMyHistory(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(interestPostingService.getHistoryForAccount(accountId));
    }
}