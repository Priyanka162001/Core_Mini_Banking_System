package in.bank.controller;

import in.bank.dto.*;
import in.bank.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Order(1)
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Endpoints for user authentication and registration")
public class AuthController {

    private final AuthService authService;

    // ================= 1️⃣ Register Customer =================
    @Operation(
            summary = "Register a new customer",
            description = "Registers customer, generates OTP and sends via email"
    )
    @PostMapping("/customers/register")
    public ResponseEntity<ApiResponse<String>> registerCustomer(
            @Valid @RequestBody RegisterRequestDTO request) {
        authService.registerCustomer(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status("SUCCESS")
                .message("Customer registered successfully. OTP sent to email.")
                .code("AUTH_201")
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ================= 2️⃣ Verify OTP =================
    @Operation(
            summary = "Verify OTP",
            description = "Verifies the OTP sent to the user's email and activates the account"
    )
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<String>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequestDTO request) {
        authService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status("SUCCESS")
                .message("OTP verified successfully. You can now login.")
                .code("AUTH_200")
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ================= 3️⃣ Resend OTP =================
    @Operation(
            summary = "Resend OTP",
            description = "Resends new OTP to email if previous expired"
    )
    @PostMapping("/otp/resend")
    public ResponseEntity<ApiResponse<String>> resendOtp(
            @Valid @RequestBody ResendOtpRequestDTO request) {
        authService.resendOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status("SUCCESS")
                .message("New OTP sent to email.")
                .code("AUTH_200")
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ================= 4️⃣ Login =================
    @Operation(
            summary = "User login",
            description = "Authenticates user and returns JWT tokens"
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request) {
        AuthResponse authData = authService.login(request);
        return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                .status("SUCCESS")
                .message("Login successful")
                .data(authData)
                .code("AUTH_200")
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ================= 5️⃣ Refresh Token =================
    @Operation(
            summary = "Refresh access token",
            description = "Generates new access token using refresh token"
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        String newAccessToken = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .status("SUCCESS")
                .message("Token refreshed successfully")
                .data(Map.of("accessToken", newAccessToken))
                .code("AUTH_200")
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ================= 6️⃣ Register Admin =================
    @Operation(
            summary = "Register a new admin",
            description = "Registers admin — no OTP required, directly active",
            security = @SecurityRequirement(name = "bearerAuth") // Swagger shows security required
    )
    @PreAuthorize("hasRole('ADMIN')") // Only ADMIN can register other admins
    @PostMapping("/admins/register")
    public ResponseEntity<ApiResponse<String>> registerAdmin(
            @Valid @RequestBody RegisterRequestDTO request) {
        authService.registerAdmin(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status("SUCCESS")
                .message("Admin registered successfully")
                .code("AUTH_201")
                .timestamp(LocalDateTime.now())
                .build());
    }

}