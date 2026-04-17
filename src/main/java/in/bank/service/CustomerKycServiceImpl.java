package in.bank.service;

import in.bank.dto.KycRequestDTO;
import in.bank.dto.KycResponseDTO;
import in.bank.entity.AppUser;
import in.bank.entity.CustomerKycRecord;
import in.bank.entity.KycStatus;
import in.bank.entity.KycVerificationStatus;
import in.bank.exception.BadRequestException;
import in.bank.exception.DuplicateResourceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.CustomerKycRecordRepository;
import in.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerKycServiceImpl implements CustomerKycService {

    private final CustomerKycRecordRepository kycRepository;
    private final SavingsAccountService savingsAccountService;
    private final UserRepository userRepository;

    // ================= SUBMIT KYC =================
    @Override
    public KycResponseDTO saveKycRecordDTO(Long customerId, Long userId, KycRequestDTO request) {

        CustomerKycRecord existing = kycRepository.findByCustomerId(customerId);

        if (existing != null) {

            // ✅ VERIFIED → block
            if (existing.getKycVerificationStatus() == KycVerificationStatus.VERIFIED) {
                throw new DuplicateResourceException(
                    "KYC is already verified for customerId: " + customerId);
            }

            // ✅ PENDING → block
            if (existing.getKycVerificationStatus() == KycVerificationStatus.PENDING) {
                throw new DuplicateResourceException(
                    "KYC is already under review. Please wait for admin verification.");
            }

            // ✅ REJECTED → allow resubmit
            existing.setDocType(request.getDocType());
            existing.setDocNumber(request.getDocNumber().replaceAll("\\s+", ""));
            existing.setDocumentImageUrl(request.getDocumentImageUrl());
            existing.setKycVerificationStatus(KycVerificationStatus.PENDING);
            existing.setRejectionReason(null);  // clear old reason
            existing.setVerifiedAt(null);
            existing.setUpdatedAt(LocalDateTime.now());
            existing.setUpdatedBy(userId);
            kycRepository.save(existing);
            return mapToDTO(existing);
        }

        // ✅ First time submission
        String cleanDocNumber = request.getDocNumber().replaceAll("\\s+", "");

        CustomerKycRecord record = CustomerKycRecord.builder()
                .customerId(customerId)
                .docType(request.getDocType())
                .docNumber(cleanDocNumber)
                .documentImageUrl(request.getDocumentImageUrl())
                .kycVerificationStatus(KycVerificationStatus.PENDING)
                .rejectionReason(null)
                .createdBy(userId)
                .updatedBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        kycRepository.save(record);
        return mapToDTO(record);
    }

    // ================= GET KYC BY CUSTOMER ID =================
    @Override
    public KycResponseDTO getKycRecordDTO(Long customerId) {
        CustomerKycRecord record = kycRepository.findByCustomerId(customerId);
        if (record == null) {
            throw new ResourceNotFoundException(
                "KYC not found for customerId: " + customerId);
        }
        return mapToDTO(record);
    }

    // ================= GET BY STATUS - LIST =================
    @Override
    public List<KycResponseDTO> getByStatusDTO(KycVerificationStatus status) {
        return kycRepository.findByKycVerificationStatus(status)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ================= GET BY STATUS - PAGINATED =================
    @Override
    public Page<KycResponseDTO> getByStatusPaginated(KycVerificationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return kycRepository.findByKycVerificationStatus(status, pageable)
                .map(this::mapToDTO);
    }

    // ================= UPDATE KYC STATUS =================
    @Override
    public KycResponseDTO updateKycStatusDTO(Long customerId, Long userId,
            KycVerificationStatus status, String rejectionReason) {

        CustomerKycRecord record = kycRepository.findByCustomerId(customerId);
        if (record == null) {
            throw new ResourceNotFoundException(
                "KYC not found for customerId: " + customerId);
        }

        // ✅ Block if already VERIFIED and trying to VERIFY again
        if (record.getKycVerificationStatus() == KycVerificationStatus.VERIFIED
                && status == KycVerificationStatus.VERIFIED) {
            throw new DuplicateResourceException(
                "KYC is already VERIFIED for customerId: " + customerId);
        }

        // ✅ REJECTED → reason is mandatory
        if (status == KycVerificationStatus.REJECTED) {
            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new BadRequestException(
                    "Rejection reason is required when rejecting KYC.");
            }
            record.setRejectionReason(rejectionReason);
            record.setVerifiedAt(null);
        }

        // ✅ VERIFIED → clear rejection reason, set verifiedAt, create account
        if (status == KycVerificationStatus.VERIFIED) {
            record.setRejectionReason(null);
            record.setVerifiedAt(LocalDateTime.now());

            // ✅ Auto create savings account
            savingsAccountService.createDefaultAccountForCustomer(customerId);

            // ✅ Update user KYC status
            AppUser user = userRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + customerId));
            user.setKycStatus(KycStatus.FULL_KYC);
            userRepository.save(user);
        }

        record.setKycVerificationStatus(status);
        record.setUpdatedBy(userId);
        record.setUpdatedAt(LocalDateTime.now());
        kycRepository.save(record);

        return mapToDTO(record);
    }

    // ================= MAPPER =================
    private KycResponseDTO mapToDTO(CustomerKycRecord record) {
        KycResponseDTO dto = new KycResponseDTO();
        dto.setKycRecordId(record.getId());
        dto.setCustomerId(record.getCustomerId());
        dto.setDocType(record.getDocType());
        dto.setDocNumber(record.getDocNumber());
        dto.setDocumentImageUrl(record.getDocumentImageUrl());
        dto.setKycVerificationStatus(record.getKycVerificationStatus().name());
        dto.setRejectionReason(record.getRejectionReason()); // ✅ NEW
        dto.setVerifiedAt(record.getVerifiedAt());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        dto.setCreatedBy(record.getCreatedBy());
        dto.setUpdatedBy(record.getUpdatedBy());
        return dto;
    }
}