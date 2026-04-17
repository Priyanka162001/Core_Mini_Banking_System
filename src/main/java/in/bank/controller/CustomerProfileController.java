package in.bank.controller;

import in.bank.dto.CompleteProfileRequestDTO;
import in.bank.dto.CustomerProfileResponseDTO;
import in.bank.dto.UpdateProfileRequestDTO;
import in.bank.service.CustomerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Order(2)
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Customer Profile", description = "APIs to manage customer profiles")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    // ================= COMPLETE PROFILE → CUSTOMER only =================
    @PostMapping("/customers/{customerId}/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Complete customer profile",
            description = "Allows a CUSTOMER to complete their profile details for the first time.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> completeProfile(
            @PathVariable Long customerId,
            @Valid @RequestBody CompleteProfileRequestDTO request) {

        customerProfileService.completeProfile(customerId, request);
        return ResponseEntity.ok("Profile completed successfully");
    }

    // ================= UPDATE PROFILE → CUSTOMER only =================
    @PatchMapping("/customers/{customerId}/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Update customer profile",
            description = "Allows a CUSTOMER to update their existing profile details.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> updateProfile(
            @PathVariable Long customerId,
            @RequestBody UpdateProfileRequestDTO request) {

        customerProfileService.updateProfile(customerId, request);
        return ResponseEntity.ok("Profile updated successfully");
    }

    // ================= GET PROFILE → CUSTOMER or ADMIN =================
    @GetMapping("/customer/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    @Operation(
            summary = "Get customer profile by ID",
            description = "Fetch a customer's profile. ADMIN can fetch any customer; CUSTOMER can fetch their own profile.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<CustomerProfileResponseDTO> getProfile(
            @PathVariable Long id) {

        return ResponseEntity.ok(customerProfileService.getProfileDTO(id));
    }

    // ================= GET ALL PROFILES → ADMIN only =================
 // ================= GET ALL PROFILES → ADMIN only =================
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all customer profiles (paginated)",
            description = "ADMIN can fetch all customer profiles in the system.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<CustomerProfileResponseDTO>> getAllProfiles(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "id")  String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(customerProfileService.getAllProfileDTOs(pageable));
    }
}