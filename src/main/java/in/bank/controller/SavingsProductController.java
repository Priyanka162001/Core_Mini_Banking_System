package in.bank.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import in.bank.dto.SavingsProductRequestDTO;
import in.bank.entity.SavingsProduct;
import in.bank.service.SavingsProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SecurityRequirement(name = "bearerAuth")
@RestController

@RequestMapping("/api/v1/savings-products")
@RequiredArgsConstructor
public class SavingsProductController {

    private final SavingsProductService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody SavingsProductRequestDTO dto) {
        Long id = service.create(dto);
        Map<String, Long> response = new HashMap<>();
        response.put("savingsProductId", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SavingsProduct> getById(@PathVariable Long id) {

        SavingsProduct product = service.getById(id);

        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(product);
    }

    @GetMapping
    public ResponseEntity<List<SavingsProduct>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsProduct> update(@PathVariable Long id,
                                                 @Valid @RequestBody SavingsProductRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}