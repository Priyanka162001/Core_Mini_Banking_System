package in.bank.service;

import in.bank.dto.KycRequestDTO;
import in.bank.dto.KycResponseDTO;
import in.bank.entity.KycVerificationStatus;
import org.springframework.data.domain.Page;
import java.util.List;

public interface CustomerKycService {
    KycResponseDTO saveKycRecordDTO(Long customerId, Long userId, KycRequestDTO request);
    KycResponseDTO getKycRecordDTO(Long customerId);
    List<KycResponseDTO> getByStatusDTO(KycVerificationStatus status);
    Page<KycResponseDTO> getByStatusPaginated(KycVerificationStatus status, int page, int size);

    // ✅ Updated signature — rejectionReason added
    KycResponseDTO updateKycStatusDTO(Long customerId, Long userId,
            KycVerificationStatus status, String rejectionReason);
}