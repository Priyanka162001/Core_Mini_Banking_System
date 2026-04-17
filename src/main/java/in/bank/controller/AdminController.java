package in.bank.controller;

import in.bank.entity.AppUser;
import in.bank.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin endpoints for managing users")
public class AdminController {

    private final UserRepository userRepository;

    @Operation(
            summary = "Get all users (paginated)",
            description = "Fetches a paginated list of all registered users. Only accessible by ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AppUser> getAllUsers(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "id")  String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(pageable);
    }
}