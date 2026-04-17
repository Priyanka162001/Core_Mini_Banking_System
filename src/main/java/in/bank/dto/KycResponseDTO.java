package in.bank.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class KycResponseDTO {
    private Long kycRecordId;
    private Long customerId;
    private String docType;
    private String docNumber;
    private String documentImageUrl;
    private String rejectionReason;
    private String kycVerificationStatus;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}