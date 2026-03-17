package in.bank.service;

import in.bank.dto.KycRequestDTO;
import in.bank.dto.KycResponseDTO;
import in.bank.entity.CustomerKycRecord;
import in.bank.entity.KycVerificationStatus;
import in.bank.repository.CustomerKycRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerKycServiceImpl implements CustomerKycService {

    private final CustomerKycRecordRepository kycRepository;

    @Override
    public KycResponseDTO saveKycRecordDTO(Long customerId, KycRequestDTO request) {
        CustomerKycRecord record = new CustomerKycRecord();
        record.setCustomerId(customerId);
        record.setPanIdentifier(request.getPanIdentifier());
        record.setAadhaarIdentifier(request.getAadhaarIdentifier());
        record.setKycVerificationStatus(KycVerificationStatus.PENDING);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        kycRepository.save(record);
        return mapToDTO(record);
    }

    @Override
    public KycResponseDTO getKycRecordDTO(Long customerId) {
        CustomerKycRecord record = kycRepository.findByCustomerId(customerId);
        if (record == null) throw new RuntimeException("KYC not found");
        return mapToDTO(record);
    }

    @Override
    public List<KycResponseDTO> getByStatusDTO(KycVerificationStatus status) {
        return kycRepository.findByKycVerificationStatus(status)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public KycResponseDTO updateKycStatusDTO(Long customerId, KycVerificationStatus status) {
        CustomerKycRecord record = kycRepository.findByCustomerId(customerId);
        if (record == null) throw new RuntimeException("KYC not found");

        record.setKycVerificationStatus(status);
        if (status == KycVerificationStatus.VERIFIED) {
            record.setVerifiedAt(LocalDateTime.now());
        }
        record.setUpdatedAt(LocalDateTime.now());
        kycRepository.save(record);

        return mapToDTO(record);
    }

    private KycResponseDTO mapToDTO(CustomerKycRecord record) {
        KycResponseDTO dto = new KycResponseDTO();
        dto.setKycRecordId(record.getId());
        dto.setCustomerId(record.getCustomerId());
        dto.setPanIdentifier(record.getPanIdentifier());
        dto.setAadhaarIdentifier(maskAadhaar(record.getAadhaarIdentifier()));
        dto.setKycVerificationStatus(record.getKycVerificationStatus().name());
        dto.setVerifiedAt(record.getVerifiedAt());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }

    private String maskAadhaar(String aadhaar) {
        if (aadhaar != null && aadhaar.length() == 12) {
            return "XXXX-XXXX-" + aadhaar.substring(8);
        }
        return aadhaar;
    }
}