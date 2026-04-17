package in.bank.service;

import in.bank.dto.AuthRequest;
import in.bank.dto.AuthResponse;
import in.bank.dto.RegisterRequestDTO;
import in.bank.entity.AppUser;
import in.bank.entity.UserRole;
import in.bank.entity.UserStatus;
import in.bank.exception.BadRequestException;
import in.bank.exception.DuplicateResourceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.UserRepository;
import in.bank.security.CustomUserDetailsService;
import in.bank.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

// Marks this class as a Spring Service (business logic layer)
@Service
@RequiredArgsConstructor // Lombok annotation generates constructor for all final fields
public class AuthService {

    private final UserRepository userRepository; // DB access for AppUser entity
    private final EmailService emailService;     // Service to send OTP emails
    private final PasswordEncoder passwordEncoder; // For hashing passwords
    private final JwtService jwtService;         // JWT token generation & validation
    private final AuthenticationManager authenticationManager; // Spring Security authentication
    private final CustomUserDetailsService userDetailsService; // Load user details for JWT validation

    // ================= OTP GENERATOR =================
    // Generates a random 6-digit OTP as a string
    private String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000; // 100000 to 999999
        return String.valueOf(otp);
    }

    // ================= REGISTER CUSTOMER =================
    public void registerCustomer(RegisterRequestDTO request) {

        // Check if email is already registered
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new DuplicateResourceException("Email already registered");
        }

        // Check if phone number is already registered
        if (userRepository.existsByPhoneNumber(
                request.getCountryCode() + request.getPhoneNumber().trim())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        // Ensure password and confirm password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Generate OTP for email verification
        String otp = generateOtp();

        // Create new customer user object
        AppUser user = AppUser.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().trim())
                .phoneNumber(request.getCountryCode() + request.getPhoneNumber().trim())
                .countryCode(request.getCountryCode())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.PENDING)
                .otp(otp)  // ⚡ OTP set here
                .otpExpiry(LocalDateTime.now().plusMinutes(5)) // ⚡ Expiry set here
                .emailVerified(false)
                .build();

        // Save user to database
        userRepository.save(user);  
        
        // Send OTP to user's email
        emailService.sendOtp(user.getEmail(), otp);
    }

    // ================= VERIFY OTP =================
    public void verifyOtp(String email, String otp) {

        // Find user by email
        AppUser user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        // Check if OTP is expired
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        // Check if OTP matches
        if (!user.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP. Please try again.");
        }

        // Mark user as verified and active
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);

        // Clear OTP and expiry
        user.setOtp(null);
        user.setOtpExpiry(null);

        // Save updated user
        userRepository.save(user);
    }

    // ================= RESEND OTP =================
    public void resendOtp(String email) {

        // Find user by email
        AppUser user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        // Prevent resending OTP if email already verified
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email is already verified.");
        }

        // Generate new OTP
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        // Save updated OTP
        userRepository.save(user);

        // Send OTP via email
        emailService.sendOtp(user.getEmail(), otp);
    }

    // ================= LOGIN =================
    public AuthResponse login(AuthRequest request) {

        String email = request.getEmail().trim();

        // Find user by email
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        // Check if customer email is verified and account active
        if (user.getRole() == UserRole.ROLE_CUSTOMER) {
            if (Boolean.FALSE.equals(user.getEmailVerified())) {
                throw new BadRequestException("Please verify your email OTP before logging in.");
            }
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BadRequestException("Your account is not active. Please contact support.");
            }
        }

        // Authenticate using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        // Extract authenticated user details
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Generate JWT access and refresh tokens
        String accessToken  = jwtService.generateToken(userDetails, user.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Return tokens along with user info
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .role(user.getRole().name().replace("ROLE_", ""))
                .build();
    }

    // ================= REFRESH TOKEN =================
    public String refreshToken(String refreshToken) {

        // Extract username (email) from refresh token
        String username = jwtService.extractUsername(refreshToken);

        // Load user details for validation
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Find user in DB
        AppUser user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate refresh token
        if (!jwtService.validateToken(refreshToken, userDetails)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        // Generate new access token
        return jwtService.generateToken(userDetails, user.getId());
    }

    // ================= REGISTER ADMIN =================
    public void registerAdmin(RegisterRequestDTO request) {

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new DuplicateResourceException("Email already registered");
        }

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(
                request.getCountryCode() + request.getPhoneNumber().trim())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        // Ensure passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Create new admin user (directly active, no OTP)
        AppUser admin = AppUser.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().trim())
                .phoneNumber(request.getCountryCode() + request.getPhoneNumber().trim())
                .countryCode(request.getCountryCode())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_ADMIN)  // Admin role
                .status(UserStatus.ACTIVE)  // Directly active
                .emailVerified(true)        // No OTP required
                .build();

        // Save admin to database
        userRepository.save(admin);
    }
}
