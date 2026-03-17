package in.bank.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class KycResponseDTO {
    private Long kycRecordId;
    private Long customerId;
    private String panIdentifier;
    private String aadhaarIdentifier;      // masked
    private String kycVerificationStatus;  // PENDING, VERIFIED, REJECTED
    private LocalDateTime verifiedAt;      // null if not verified
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}