package in.bank.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// JPA entity mapped to "users" table
@Entity
@Table(name = "users")
// No @EntityListeners for auditing to prevent createdBy/updatedBy
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    // Primary key with auto-increment
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User's first and last name
    private String firstName;
    private String lastName;

    // Email must be unique and not null
    @Column(unique = true, nullable = false)
    private String email;

    // Phone number must be unique and not null
    @Column(unique = true, nullable = false)
    private String phoneNumber;

    // Country code for phone number
    private String countryCode;

    // Password (hashed)
    private String password;

    // User role (CUSTOMER, ADMIN, etc.)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    // Account status (PENDING, ACTIVE, INACTIVE)
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
 // ✅ Replace with
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    private KycStatus kycStatus;

    // ================= OTP FIELDS =================
    // OTP and expiry time, nullable initially
    private String otp;
    private LocalDateTime otpExpiry;

    // Email verification flag
    private Boolean emailVerified = false;
    
  

    // ================= AUDIT FIELDS =================
    // Timestamp when user is created
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Timestamp when user is updated
    private LocalDateTime updatedAt;

    // Automatically set createdAt and updatedAt before first insert
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Automatically update updatedAt timestamp on every update
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}