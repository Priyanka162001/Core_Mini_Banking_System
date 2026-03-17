package in.bank.service;

import in.bank.dto.KycRequestDTO;
import in.bank.dto.KycResponseDTO;
import in.bank.entity.KycVerificationStatus;

import java.util.List;

public interface CustomerKycService {

    KycResponseDTO saveKycRecordDTO(Long customerId, KycRequestDTO request);

    KycResponseDTO getKycRecordDTO(Long customerId);

    List<KycResponseDTO> getByStatusDTO(KycVerificationStatus status);

    KycResponseDTO updateKycStatusDTO(Long customerId, KycVerificationStatus status);
}