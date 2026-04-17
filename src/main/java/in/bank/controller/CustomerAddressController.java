package in.bank.controller;

import in.bank.dto.AddressRequestDTO;
import in.bank.dto.AddressResponseDTO;
import in.bank.service.CustomerAddressService;
import in.bank.entity.AddressStatus;
import in.bank.exception.AccessDeniedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Order(3)
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
@Tag(name = "Customer Address", description = "Endpoints for managing customer addresses")
public class CustomerAddressController {

    private final CustomerAddressService customerAddressService;

    /**
     * ✅ Add Address
     * ADMIN    → can add address for ANY customer
     * CUSTOMER → can add address for THEIR OWN account only
     */
    @PostMapping("/{customerId}/addresses")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and #customerId == authentication.principal.id)")
    @Operation(
            summary = "Add a new address",
            description = "ADMIN can add address for any customer. CUSTOMER can add address for their own account only.",
            security = @SecurityRequirement(name = "bearerAuth") // JWT required
    )
    public ResponseEntity<String> addAddress(
            @PathVariable Long customerId,
            @Valid @RequestBody AddressRequestDTO request) {

        customerAddressService.addAddress(customerId, request);
        return ResponseEntity.ok("Address added successfully");
    }

    /**
     * ✅ Get Addresses by Status (ACTIVE / INACTIVE / ALL)
     * ADMIN    → can view ANY customer's addresses with ANY status
     * CUSTOMER → can view ONLY their own addresses (ACTIVE only)
     */
    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and #customerId == authentication.principal.id)")
    @Operation(
            summary = "Get addresses by status",
            description = "ADMIN can view any customer's addresses with any status. CUSTOMER can view only their own ACTIVE addresses.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<AddressResponseDTO>> getAddresses(
            @PathVariable Long customerId,
            @Parameter(
                    description = "Filter by address status: ACTIVE / INACTIVE / ALL (INACTIVE & ALL are ADMIN only)",
                    required = true
            )
            @RequestParam(required = true) AddressStatus status,
            Authentication authentication) {  // ← inject Authentication

        // ✅ NEW: Check if CUSTOMER is trying INACTIVE or ALL
        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        if (isCustomer && status != AddressStatus.ACTIVE) {
            throw new AccessDeniedException(
                "CUSTOMER can only view ACTIVE addresses. INACTIVE and ALL are ADMIN only."
            );
        }

        return ResponseEntity.ok(
                customerAddressService.getAddressesByStatus(customerId, status)
        );
    }
    /**
     * ✅ Update Address (preserves history)
     * ADMIN    → ALLOWED
     * CUSTOMER → NOT ALLOWED (403 Forbidden)
     */
    @PutMapping("/update/{addressId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update an address",
            description = "Updates an address (preserves history). ADMIN only. CUSTOMER cannot update.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<AddressResponseDTO> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequestDTO request) {

        return ResponseEntity.ok(
                customerAddressService.updateAddress(addressId, request)
        );
    }

    /**
     * ✅ Deactivate Address (soft delete)
     * ADMIN    → ALLOWED
     * CUSTOMER → NOT ALLOWED (403 Forbidden)
     */
    @DeleteMapping("/deactivate/{addressId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Deactivate an address",
            description = "Soft deletes an address. ADMIN only. CUSTOMER cannot deactivate.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> deactivateAddress(
            @PathVariable Long addressId) {

        customerAddressService.deactivateAddress(addressId);
        return ResponseEntity.ok("Address deactivated successfully");
    }
}