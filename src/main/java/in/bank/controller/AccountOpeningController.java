package in.bank.controller;

import in.bank.dto.*;
import in.bank.entity.ActionType;
import in.bank.entity.RequestStatus;
import in.bank.security.JwtService;
import in.bank.service.AccountOpeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/account-openings")
@RequiredArgsConstructor
@Tag(
    name = "Account Opening",
    description = "APIs for managing bank account opening requests. " +
                  "CUSTOMER can create and view their own requests. " +
                  "ADMIN can view all requests and approve or reject them."
)
@SecurityRequirement(name = "bearerAuth")
public class AccountOpeningController {

    private final AccountOpeningService service;
    private final JwtService jwtService;

    // ─────────────────────────────────────────────
    // CUSTOMER: Create account opening request
    // ─────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Submit account opening request",
        description = "Allows an authenticated **CUSTOMER** to submit a new bank account opening request. " +
                      "Returns the generated request ID on success.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body / validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid JWT token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden – only CUSTOMER role is allowed")
    })
    public ApiResponse<Long> create(
            @Valid @RequestBody AccountOpeningRequestDTO dto,
            HttpServletRequest request) {

        Long userId = extractUserId(request);

        return ApiResponse.<Long>builder()
                .status("SUCCESS")
                .message("Account opening request created")
                .data(service.createRequest(userId, dto))
                .code("REQ_201")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────
    // CUSTOMER: View own requests
    // ─────────────────────────────────────────────
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Get my account opening requests",
        description = "Returns all account opening requests submitted by the currently authenticated **CUSTOMER**.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of requests fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid JWT token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden – only CUSTOMER role is allowed")
    })
    public ApiResponse<List<AccountOpeningResponseDTO>> myRequests(HttpServletRequest request) {

        Long userId = extractUserId(request);

        return ApiResponse.<List<AccountOpeningResponseDTO>>builder()
                .status("SUCCESS")
                .message("Fetched successfully")
                .data(service.getMyRequests(userId))
                .code("REQ_200")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────
    // CUSTOMER + ADMIN: Get single request by ID
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get account opening request by ID",
        description = "Returns the full details of a single account opening request. " +
                      "**CUSTOMER** can only fetch their own request. " +
                      "**ADMIN** can fetch any request.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid JWT token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden – CUSTOMER trying to access another user's request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Request not found for the given ID")
    })
    public ApiResponse<AccountOpeningResponseDTO> getById(
            @Parameter(description = "ID of the account opening request", required = true, example = "42")
            @PathVariable Long id,
            HttpServletRequest request) {

        Long userId = extractUserId(request);
        boolean isAdmin = isAdmin(request);

        return ApiResponse.<AccountOpeningResponseDTO>builder()
                .status("SUCCESS")
                .message("Fetched successfully")
                .data(service.getById(id, userId, isAdmin))
                .code("REQ_200")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────
    // ADMIN: Get all requests (optionally filter by status)
    // ─────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get all account opening requests",
        description = "Returns all account opening requests across all customers. " +
                      "Optionally filter by **status** (PENDING, APPROVED, REJECTED). " +
                      "Only accessible by **ADMIN**.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of all requests fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid JWT token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden – only ADMIN role is allowed")
    })
    public ApiResponse<List<AccountOpeningResponseDTO>> getAllRequests(
            @RequestParam(required = false) RequestStatus status) {

        List<AccountOpeningResponseDTO> data = service.getAllRequests(status);

        String message = data.isEmpty()
                ? "No account opening requests found"
                : "Fetched successfully";

        return ApiResponse.<List<AccountOpeningResponseDTO>>builder()
                .status("SUCCESS")
                .message(message) // ✅ dynamic message
                .data(data)
                .code("REQ_200")
                .timestamp(LocalDateTime.now())
                //.count(data.size()) // 👉 optional (if you add field)
                .build();
    }

    // ─────────────────────────────────────────────
    // ADMIN: Approve or Reject request
    // ─────────────────────────────────────────────
    @PutMapping("/{id}/action")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Approve or reject an account opening request",
        description = "Allows an **ADMIN** to approve or reject a pending account opening request by its ID. " +
                      "A `reason` is required when rejecting.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Action taken successfully (approved or rejected)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid action type or missing required reason for rejection"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid JWT token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden – only ADMIN role is allowed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account opening request not found for the given ID")
    })
    public ApiResponse<String> takeAction(
            @Parameter(description = "ID of the account opening request", required = true, example = "42")
            @PathVariable Long id,

            @Parameter(description = "Action to take: APPROVE or REJECT", required = true, example = "APPROVE")
            @RequestParam ActionType action,

            @Parameter(description = "Reason for rejection (required when action = REJECT)", example = "Incomplete documents")
            @RequestParam(required = false) String reason,

            HttpServletRequest request) {

        Long adminId = extractUserId(request);
        service.takeAction(id, adminId, action, reason);

        String message = switch (action) {
            case APPROVE -> "Account opening approved successfully";
            case REJECT  -> "Account opening rejected successfully";
        };

        return ApiResponse.<String>builder()
                .status("SUCCESS")
                .message(message)
                .code("REQ_200")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────
    private Long extractUserId(HttpServletRequest request) {
        String token = extractToken(request);
        return jwtService.extractUserId(token);
    }

    private boolean isAdmin(HttpServletRequest request) {
        String token = extractToken(request);          // ← extract token first
        return jwtService.extractRoles(token).contains("ROLE_ADMIN"); // ← pass token, not request
    }
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token");
        }
        return header.substring(7);
    }
}