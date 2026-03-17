package in.bank.service;
import in.bank.dto.RegisterRequestDTO;
import in.bank.entity.AppUser;
import in.bank.entity.UserRole;
import in.bank.entity.UserStatus;
import in.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public String register(RegisterRequestDTO request) {

        // ✅ check duplicate email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // ✅ check password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        AppUser user = new AppUser();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        // ✅ Combine phone
        String fullPhone = request.getCountryCode() + request.getPhoneNumber();
        user.setPhoneNumber(fullPhone);

        // ✅ Encrypt password
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // ✅ FIXED ROLE (🔥 IMPORTANT)
        user.setRole(UserRole.ROLE_CUSTOMER);

        // ✅ Initial states
        user.setEmailVerified(false);
        user.setStatus(UserStatus.PENDING);

        // ✅ Generate OTP
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        // ✅ Send OTP
        emailService.sendOtp(user.getEmail(), otp);

        return "User registered successfully. OTP sent to email.";
    }
    // OTP generator
    private String generateOtp() {
        int otp = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }
}  