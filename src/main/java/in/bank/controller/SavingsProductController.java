package in.bank.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import in.bank.dto.SavingsProductRequestDTO;
import in.bank.entity.SavingsProduct;
import in.bank.service.SavingsProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Order(9)
@RequestMapping("/api/v1/savings-products")
@RequiredArgsConstructor
@Tag(name = "Savings Products", description = "Operations related to savings products")
@SecurityRequirement(name = "bearerAuth")
public class SavingsProductController {

    private final SavingsProductService service;

    // ================= CREATE PRODUCT → ADMIN ONLY =================
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new savings product",
            description = "Creates a new savings product. Only users with ADMIN role can create products.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody SavingsProductRequestDTO dto) {
        Long id = service.create(dto);
        Map<String, Long> response = new HashMap<>();
        response.put("savingsProductId", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ================= GET PRODUCT BY ID → ANY AUTHENTICATED USER =================
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    @Operation(
            summary = "Get a savings product by ID",
            description = "Returns the details of a savings product by its ID. Accessible by both ADMIN and CUSTOMER roles.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SavingsProduct> getById(@PathVariable Long id) {
        SavingsProduct product = service.getById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    // ================= GET ALL PRODUCTS → ANY AUTHENTICATED USER =================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    @Operation(
            summary = "Get all savings products",
            description = "Returns a paginated list of all savings products. Accessible by both ADMIN and CUSTOMER roles.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Page<SavingsProduct>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(service.getAll(pageable));
    }

    // ================= UPDATE PRODUCT → ADMIN ONLY =================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update a savings product",
            description = "Updates an existing savings product by its ID with new values. Only ADMIN can update products.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> update(@PathVariable Long id,
                                                      @Valid @RequestBody SavingsProductRequestDTO dto) {
        service.update(id, dto);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Savings product updated successfully");
        return ResponseEntity.ok(response);
    }

    // ================= DELETE PRODUCT → ADMIN ONLY =================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete a savings product",
            description = "Deletes a savings product by its ID. Only ADMIN can delete products.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.delete(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Savings product deleted successfully");
        return ResponseEntity.ok(response);
    }
}