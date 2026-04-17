package in.bank.controller;

import in.bank.dto.TransactionCreateResponseDTO;
import in.bank.dto.TransactionRequestDTO;
import in.bank.dto.TransactionResponseDTO;
import in.bank.dto.TransactionSummaryDTO;
import in.bank.entity.TransactionType;
import in.bank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Savings account transaction APIs")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    // ================= CREATE TRANSACTION → ADMIN + USER =================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    @Operation(
            summary = "Create a transaction (Deposit / Withdrawal)",
            description = "🔐 ADMIN + CUSTOMER | Create deposit or withdrawal transactions for a savings account.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<TransactionCreateResponseDTO> createTransaction(
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody TransactionRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {  // ✅ added

        return ResponseEntity.ok(
            transactionService.processTransaction(key, request, userDetails));  // ✅ pass it
    }

    // ================= GET ALL OR SINGLE TRANSACTION → ADMIN ONLY =================
    @GetMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all transactions or single transaction by ID",
            description = """
                🔐 ADMIN only |
                
                - No `transactionId` → returns all transactions for the account
                - With `transactionId` → returns single transaction details
                """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public Object getTransactions(
            @Parameter(description = "Account ID", example = "7") @PathVariable Long accountId,
            @Parameter(description = "Optional transaction ID to fetch a single transaction", example = "42")
            @RequestParam(required = false) Long transactionId) {

        if (transactionId != null) {
            return transactionService.getTransactionById(accountId, transactionId);
        }
        return transactionService.getAllTransactionsDTO(accountId);
    }

    // ================= FILTER TRANSACTIONS → ADMIN ONLY =================
    @GetMapping("/{accountId}/filter")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Filter transactions by type and/or date range",
            description = """
                🔐 ADMIN only |
                
                Filter transactions optionally by type (DEPOSIT / WITHDRAWAL) and/or date range.
                
                - `type` only → filter by DEPOSIT or WITHDRAWAL
                - `start` + `end` → filter by date range
                - `type` + `start` + `end` → filter by both
                - no params → returns all transactions
                
                Date format: yyyy-MM-dd'T'HH:mm:ss
                """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<TransactionResponseDTO> filterTransactions(
            @PathVariable Long accountId,
            @Parameter(description = "Filter by transaction type: DEPOSIT or WITHDRAWAL", example = "DEPOSIT")
            @RequestParam(required = false) TransactionType type,
            @Parameter(description = "Start datetime (inclusive)", example = "2024-01-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End datetime (inclusive)", example = "2024-12-31T23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        return transactionService.filterTransactions(accountId, type, start, end);
    }

    // ================= MY TRANSACTIONS → CUSTOMER ONLY =================
    @GetMapping("/my-transactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Get my transactions",
            description = "🔐 CUSTOMER only | Returns transactions of the logged-in customer using JWT.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<TransactionResponseDTO> getMyTransactions() {
        return transactionService.getTransactionsForLoggedInUser();
    }

    // ================= TRANSACTION SUMMARY → ADMIN + CUSTOMER =================
    @GetMapping("/{accountId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    @Operation(
            summary = "Get transaction summary",
            description = "🔐 ADMIN + CUSTOMER | Returns total deposits, withdrawals, balance and count for the account.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public TransactionSummaryDTO getSummary(@PathVariable Long accountId) {
        return transactionService.getTransactionSummary(accountId);
    }
}