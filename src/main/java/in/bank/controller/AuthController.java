package in.bank.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import in.bank.dto.AuthResponse;
import in.bank.dto.LoginRequestDTO;
import in.bank.dto.RefreshTokenRequest;
import in.bank.dto.RegisterRequestDTO;
import in.bank.entity.AppUser;
import in.bank.entity.UserRole;
import in.bank.entity.UserStatus;
import in.bank.repository.UserRepository;
import in.bank.security.CustomUserDetailsService;
import in.bank.security.JwtService;
import in.bank.service.EmailService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CustomUserDetailsService userDetailsService;

    // Generate 6-digit OTP
    private String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }

    // --------------------------
    // 1️⃣ Register Customer
    // --------------------------
    @PostMapping("/register-customer")
    public ResponseEntity<String> registerCustomer(@Valid @RequestBody RegisterRequestDTO request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String otp = generateOtp();

        AppUser user = AppUser.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .countryCode(request.getCountryCode())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.PENDING)                .otp(otp)
                .otpExpiry(LocalDateTime.now().plusMinutes(5))
                .emailVerified(false)
                .build();
   
        userRepository.save(user);

        // Send OTP email
        emailService.sendOtp(user.getEmail(), otp);

        return ResponseEntity.ok("Customer registered. OTP sent to email");
    }

    // --------------------------
    // 2️⃣ Verify OTP
    // --------------------------
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestParam String email, @RequestParam String otp) {

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired. Please request a new one.");
        }

        if (!user.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setOtp(null);
        user.setOtpExpiry(null);

        userRepository.save(user);

        return ResponseEntity.ok("OTP verified successfully. You can now login.");
    }

    // --------------------------
    // 3️⃣ Login
    // --------------------------
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequestDTO request) {

        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if email verified and status active
        if (!user.getEmailVerified() || !user.getStatus().equals("ACTIVE")) {
            throw new RuntimeException("Please verify OTP before login.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // ✅ Generate tokens with userId
        String token = jwtService.generateToken(userDetails, user.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        AuthResponse response = new AuthResponse(
                "Login successful",
                token,
                refreshToken,
                userDetails.getUsername(),
                role
        );

        return ResponseEntity.ok(response);
    }

    // --------------------------
    // 4️⃣ Refresh Token
    // --------------------------
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(
            @RequestBody RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();
        String username = jwtService.extractUsername(refreshToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // ✅ Fetch AppUser to get userId
        AppUser user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!jwtService.validateToken(refreshToken, userDetails)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateToken(userDetails, user.getId());

        return ResponseEntity.ok(Map.of("token", newAccessToken));
    }

    // --------------------------
    // 5️⃣ Register Admin
    // --------------------------
    @PostMapping("/register-admin")
    public ResponseEntity<String> registerAdmin(@Valid @RequestBody RegisterRequestDTO request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        AppUser admin = AppUser.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .countryCode(request.getCountryCode())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.PENDING)
                .emailVerified(true)
                .build();

        userRepository.save(admin);

        return ResponseEntity.ok("Admin registered successfully");
    }
}