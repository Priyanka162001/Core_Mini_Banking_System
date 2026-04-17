package in.bank.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_records")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerKycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private Long customerId;

    
    
    @Column(name = "doc_type", length = 20, nullable = false)
    private String docType;

    @Column(name = "doc_number", length = 20, nullable = false)
    private String docNumber;

    // ✅ Binary image stored in DB
    @Column(name = "document_image_url", nullable = false)
    private String documentImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_verification_status", nullable = false)
    private KycVerificationStatus kycVerificationStatus = KycVerificationStatus.PENDING;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}