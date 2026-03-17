package in.bank.entity;
import in.bank.entity.KycVerificationStatus; 
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_kyc_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerKycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private Long customerId;

    @Column(name = "pan_identifier", nullable = false, unique = true, length = 20)
    private String panIdentifier;

    @Column(name = "aadhaar_identifier", nullable = false, unique = true, length = 20)
    private String aadhaarIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_verification_status", nullable = false)
    private KycVerificationStatus kycVerificationStatus = KycVerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "otp")
    private String otp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}