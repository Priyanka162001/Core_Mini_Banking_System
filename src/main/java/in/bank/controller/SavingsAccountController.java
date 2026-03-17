package in.bank.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import in.bank.dto.SavingsAccountRequestDTO;
import in.bank.entity.SavingsAccount;
import in.bank.service.SavingsAccountService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class SavingsAccountController {

    private final SavingsAccountService service;

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(
            @Valid @RequestBody SavingsAccountRequestDTO request) {

        Long id = service.createAccount(request);

        return ResponseEntity.ok(Map.of("accountId", id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsAccount> get(@PathVariable Long id) {

        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<SavingsAccount>> getAll() {

        return ResponseEntity.ok(service.getAll());
    }

    @PostMapping("/{id}/freeze")
    public ResponseEntity<String> freeze(@PathVariable Long id) {

        service.freezeAccount(id);

        return ResponseEntity.ok("Account frozen successfully");
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<String> close(@PathVariable Long id) {

        service.closeAccount(id);

        return ResponseEntity.ok("Account closed successfully");
    }
}