package in.bank.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import in.bank.dto.ApiResponse;
import in.bank.dto.SavingsAccountResponseDTO;
import in.bank.entity.AccountLifecycleStatus;
import in.bank.service.SavingsAccountService;
import in.bank.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Order(5)
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Savings Accounts", description = "APIs to manage customer savings accounts")
public class SavingsAccountController {

    private final SavingsAccountService service;
    private final JwtService jwtService;

    // Helper to extract userId from JWT token
    private Long extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid or missing token");
        }
        return jwtService.extractUserId(header.substring(7));
    }

    // ================= GET ALL ACCOUNTS FOR LOGGED-IN USER =================
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Get all accounts for logged-in customer",
            description = "Fetch all savings accounts associated with the currently authenticated CUSTOMER.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<List<SavingsAccountResponseDTO>> getAll(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ApiResponse.<List<SavingsAccountResponseDTO>>builder()
                .status("SUCCESS")
                .message("Accounts fetched")
                .data(service.getAccountsByUserId(userId))
                .code("ACC_200")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ================= GET ACCOUNT BY ID =================
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(
            summary = "Get account by ID",
            description = "Fetch a savings account by its ID. CUSTOMER can access their own accounts, ADMIN can access any account.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<SavingsAccountResponseDTO> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ApiResponse.<SavingsAccountResponseDTO>builder()
                .status("SUCCESS")
                .message("Account fetched")
                .data(service.getById(id, userDetails))
                .code("ACC_200")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ================= UPDATE ACCOUNT STATUS → ADMIN ONLY =================
    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update account status — ADMIN only",
            description = """
                    Update the lifecycle status of a savings account.

                    **ACTIVE**  → Reactivates a previously FROZEN account
                    **FROZEN**  → Temporarily suspends all transactions
                    **CLOSED**  → Permanently closes the account (irreversible)

                    Only ADMIN can change account status.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @PathVariable AccountLifecycleStatus status) {

        service.updateAccountStatus(id, status);

        String message = switch (status) {
            case ACTIVE -> "Account reactivated successfully";
            case FROZEN -> "Account frozen successfully";
            case CLOSED -> "Account closed successfully";
        };

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status("SUCCESS")
                .message(message)
                .data(null)
                .code("ACC_200")
                .timestamp(LocalDateTime.now())
                .build());
    }
}