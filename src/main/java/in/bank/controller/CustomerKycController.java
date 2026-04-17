package in.bank.controller;

import in.bank.dto.ApiResponse;
import in.bank.dto.KycRequestDTO;
import in.bank.dto.KycResponseDTO;
import in.bank.entity.KycVerificationStatus;
import in.bank.exception.AccessDeniedException;
import in.bank.service.CustomerKycService;
import in.bank.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Order(4)
@RequestMapping("/kyc/v1")
@Tag(name = "KYC", description = "KYC Verification Management")
@RequiredArgsConstructor
public class CustomerKycController {

    private final CustomerKycService kycService;
    private final JwtService jwtService;

    // ================= SUBMIT KYC =================
    @PreAuthorize("hasRole('CUSTOMER') and #customerId == principal.id")
    @Operation(
            summary = "Submit KYC",
            description = "Customer submits KYC document. If previously REJECTED, resubmission is allowed.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/submit/{customerId}")
    public ResponseEntity<Map<String, String>> saveKyc(
            @PathVariable Long customerId,
            @Valid @RequestBody KycRequestDTO request,
            HttpServletRequest httpRequest) {

        Long userId = extractUserId(httpRequest);
        kycService.saveKycRecordDTO(customerId, userId, request);

        return ResponseEntity.ok(
            Map.of("message", "KYC submitted successfully")
        );
    }

    // ================= GET KYC =================
    @Operation(
            summary = "Get KYC details by Customer ID",
            description = "CUSTOMER can view their own KYC. ADMIN can view any customer's KYC.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<KycResponseDTO>> getKyc(
            @PathVariable Long customerId,
            HttpServletRequest httpRequest) {

        Long loggedInUserId = extractUserId(httpRequest);
        boolean isAdmin = jwtService.extractRoles(
                httpRequest.getHeader("Authorization").substring(7))
                .contains("ROLE_ADMIN");

        if (!isAdmin && !loggedInUserId.equals(customerId)) {
            throw new AccessDeniedException(
                    "You are not authorized to view this KYC");
        }

        return ResponseEntity.ok(ApiResponse.<KycResponseDTO>builder()
                .status("SUCCESS")
                .message("KYC fetched successfully")
                .data(kycService.getKycRecordDTO(customerId))
                .code("KYC_200")
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ================= GET KYC BY STATUS =================
    @Operation(
            summary = "Get KYC list by Status",
            description = "🔐 ADMIN only | Returns paginated KYC records filtered by status.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Page<KycResponseDTO>>> getByStatus(
            @Parameter(description = "Filter by KYC status", required = true,
                       schema = @io.swagger.v3.oas.annotations.media.Schema(
                           allowableValues = {"PENDING", "VERIFIED", "REJECTED"}))
            @RequestParam KycVerificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<KycResponseDTO> pagedResult = kycService.getByStatusPaginated(status, page, size);

        String message = pagedResult.isEmpty()
                ? "No KYC records found with status: " + status.name()
                : "KYC list fetched successfully. Total records: " + pagedResult.getTotalElements();

        return ResponseEntity.ok(ApiResponse.<Page<KycResponseDTO>>builder()
                .status("SUCCESS")
                .message(message)
                .data(pagedResult)
                .code("KYC_200")
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ================= UPDATE KYC STATUS =================
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update KYC Status",
            description = "Admin approves or rejects KYC. Rejection reason is required when rejecting.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/status/{customerId}")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long customerId,
            @RequestParam KycVerificationStatus status,
            @Parameter(description = "Required when status is REJECTED")
            @RequestParam(required = false) String rejectionReason, // ✅ NEW
            HttpServletRequest httpRequest) {

        Long userId = extractUserId(httpRequest);
        kycService.updateKycStatusDTO(customerId, userId, status, rejectionReason);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status("SUCCESS")
                .message("KYC status updated to " + status.name() + " successfully")
                .data(null)
                .code("KYC_200")
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ================= EXTRACT USER ID FROM JWT =================
    private Long extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        return jwtService.extractUserId(authHeader.substring(7));
    }
}