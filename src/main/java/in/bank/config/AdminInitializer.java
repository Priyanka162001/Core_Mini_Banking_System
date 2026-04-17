package in.bank.config;

import in.bank.entity.AppUser;
import in.bank.entity.UserRole;
import in.bank.entity.UserStatus;
import in.bank.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository; 
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void createAdmin() {
        System.out.println("🔥 AdminInitializer running...");
        
        if (userRepository.findByEmail("admin@bank.com").isPresent()) {
            System.out.println("✅ Admin already exists, skipping creation");
            return;
        }

        AppUser admin = AppUser.builder()
                .firstName("Super")
                .lastName("Admin")
                .email("admin@bank.com")
                .phoneNumber("9999999999")
                .countryCode("+91")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        userRepository.save(admin);
        System.out.println("✅ Admin created in DB");
    }
}
