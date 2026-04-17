package in.bank.service;

import in.bank.dto.KycRequestDTO;
import in.bank.dto.KycResponseDTO;
import in.bank.entity.*;
import in.bank.exception.BadRequestException;
import in.bank.exception.DuplicateResourceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.CustomerKycRecordRepository;
import in.bank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerKycService Tests")
class CustomerKycServiceTest {

    @Mock
    private CustomerKycRecordRepository kycRepository;

    @Mock
    private SavingsAccountService savingsAccountService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerKycServiceImpl kycService;

    private KycRequestDTO kycRequest;
    private CustomerKycRecord kycRecord;
    private AppUser customer;

    @BeforeEach
    void setUp() {
        kycRequest = new KycRequestDTO();
        kycRequest.setDocType("AADHAAR");
        kycRequest.setDocNumber("1234 5678 9012");
        kycRequest.setDocumentImageUrl("https://example.com/aadhaar.jpg");

        kycRecord = CustomerKycRecord.builder()
                .id(1L)
                .customerId(1L)
                .docType("AADHAAR")
                .docNumber("123456789012")
                .documentImageUrl("https://example.com/aadhaar.jpg")
                .kycVerificationStatus(KycVerificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .createdBy(1L)
                .build();

        customer = AppUser.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .role(UserRole.ROLE_CUSTOMER)
                .kycStatus(null)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Nested
    @DisplayName("saveKycRecordDTO() Tests")
    class SaveKycRecordDTOTests {

        @Test
        @DisplayName("TC1: First time KYC submission - success")
        void submitKyc_FirstTime_Success() {
            when(kycRepository.findByCustomerId(1L)).thenReturn(null);
            when(kycRepository.save(any(CustomerKycRecord.class))).thenReturn(kycRecord);

            KycResponseDTO result = kycService.saveKycRecordDTO(1L, 1L, kycRequest);

            assertNotNull(result);
            assertEquals(1L, result.getCustomerId());
            assertEquals("PENDING", result.getKycVerificationStatus());
            verify(kycRepository).save(any(CustomerKycRecord.class));
        }

        @Test
        @DisplayName("TC2: Already VERIFIED - throws exception")
        void submitKyc_AlreadyVerified_ThrowsDuplicateException() {
            kycRecord.setKycVerificationStatus(KycVerificationStatus.VERIFIED);
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);

            DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                    () -> kycService.saveKycRecordDTO(1L, 1L, kycRequest));

            assertEquals("KYC is already verified for customerId: 1", exception.getMessage());
            verify(kycRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC3: Already PENDING - throws exception")
        void submitKyc_AlreadyPending_ThrowsDuplicateException() {
            kycRecord.setKycVerificationStatus(KycVerificationStatus.PENDING);
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);

            DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                    () -> kycService.saveKycRecordDTO(1L, 1L, kycRequest));

            assertEquals("KYC is already under review. Please wait for admin verification.", 
                    exception.getMessage());
            verify(kycRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC4: Resubmit after REJECTED - success")
        void submitKyc_ResubmitAfterRejection_Success() {
            kycRecord.setKycVerificationStatus(KycVerificationStatus.REJECTED);
            kycRecord.setRejectionReason("Invalid document");
            
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);
            when(kycRepository.save(any(CustomerKycRecord.class))).thenReturn(kycRecord);

            KycResponseDTO result = kycService.saveKycRecordDTO(1L, 1L, kycRequest);

            assertNotNull(result);
            assertEquals("PENDING", result.getKycVerificationStatus());
            assertNull(kycRecord.getRejectionReason());
            verify(kycRepository).save(kycRecord);
        }
    }

    @Nested
    @DisplayName("getKycRecordDTO() Tests")
    class GetKycRecordDTOTests {

        @Test
        @DisplayName("TC5: Get KYC - success")
        void getKyc_Success() {
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);

            KycResponseDTO result = kycService.getKycRecordDTO(1L);

            assertNotNull(result);
            assertEquals(1L, result.getCustomerId());
        }

        @Test
        @DisplayName("TC6: Get KYC - not found - covers yellow/red lines")
        void getKyc_NotFound_ThrowsResourceNotFoundException() {
            when(kycRepository.findByCustomerId(99L)).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> kycService.getKycRecordDTO(99L));

            assertEquals("KYC not found for customerId: 99", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getByStatusPaginated() Tests")
    class GetByStatusPaginatedTests {

        @Test
        @DisplayName("TC7: Get KYC by status paginated - success")
        void getKycByStatusPaginated_Success() {
            Page<CustomerKycRecord> page = new PageImpl<>(List.of(kycRecord));
            
            when(kycRepository.findByKycVerificationStatus(eq(KycVerificationStatus.PENDING), 
                    any(PageRequest.class))).thenReturn(page);

            Page<KycResponseDTO> result = kycService.getByStatusPaginated(
                    KycVerificationStatus.PENDING, 0, 20);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("TC8: Get KYC by status - empty page")
        void getKycByStatusPaginated_EmptyPage() {
            Page<CustomerKycRecord> emptyPage = new PageImpl<>(List.of());
            
            when(kycRepository.findByKycVerificationStatus(eq(KycVerificationStatus.VERIFIED), 
                    any(PageRequest.class))).thenReturn(emptyPage);

            Page<KycResponseDTO> result = kycService.getByStatusPaginated(
                    KycVerificationStatus.VERIFIED, 0, 20);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("getByStatusDTO() Tests")
    class GetByStatusDTOTests {

        @Test
        @DisplayName("TC9: Get KYC list by status - success")
        void getByStatusDTO_Success() {
            when(kycRepository.findByKycVerificationStatus(KycVerificationStatus.PENDING))
                    .thenReturn(List.of(kycRecord));

            List<KycResponseDTO> result = kycService.getByStatusDTO(KycVerificationStatus.PENDING);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("PENDING", result.get(0).getKycVerificationStatus());
        }

        @Test
        @DisplayName("TC10: Get KYC list by status - empty list")
        void getByStatusDTO_EmptyList() {
            when(kycRepository.findByKycVerificationStatus(KycVerificationStatus.VERIFIED))
                    .thenReturn(List.of());

            List<KycResponseDTO> result = kycService.getByStatusDTO(KycVerificationStatus.VERIFIED);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("updateKycStatusDTO() Tests")
    class UpdateKycStatusDTOTests {

        @Test
        @DisplayName("TC11: Update to VERIFIED - success")
        void updateKycStatus_ToVerified_Success() {
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(kycRepository.save(any(CustomerKycRecord.class))).thenReturn(kycRecord);
            
            doNothing().when(savingsAccountService).createDefaultAccountForCustomer(1L);

            KycResponseDTO result = kycService.updateKycStatusDTO(1L, 2L, 
                    KycVerificationStatus.VERIFIED, null);

            assertNotNull(result);
            assertEquals("VERIFIED", result.getKycVerificationStatus());
            assertNotNull(kycRecord.getVerifiedAt());
            assertEquals(KycStatus.FULL_KYC, customer.getKycStatus());
            
            verify(savingsAccountService).createDefaultAccountForCustomer(1L);
            verify(userRepository).save(customer);
        }

        @Test
        @DisplayName("TC12: Update to VERIFIED when already VERIFIED - throws exception - covers yellow/red lines")
        void updateKycStatus_AlreadyVerified_ThrowsDuplicateException() {
            kycRecord.setKycVerificationStatus(KycVerificationStatus.VERIFIED);
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);

            DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                    () -> kycService.updateKycStatusDTO(1L, 2L, 
                            KycVerificationStatus.VERIFIED, null));

            assertEquals("KYC is already VERIFIED for customerId: 1", exception.getMessage());
            verify(kycRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC13: Update to REJECTED without reason - throws exception - covers yellow/red lines")
        void updateKycStatus_ToRejectedWithoutReason_ThrowsBadRequestException() {
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> kycService.updateKycStatusDTO(1L, 2L, 
                            KycVerificationStatus.REJECTED, null));

            assertEquals("Rejection reason is required when rejecting KYC.", exception.getMessage());
            verify(kycRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC14: Update to REJECTED with blank reason - throws exception - covers yellow/red lines")
        void updateKycStatus_ToRejectedWithBlankReason_ThrowsBadRequestException() {
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> kycService.updateKycStatusDTO(1L, 2L, 
                            KycVerificationStatus.REJECTED, "   "));

            assertEquals("Rejection reason is required when rejecting KYC.", exception.getMessage());
            verify(kycRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC15: Update to REJECTED with valid reason - success")
        void updateKycStatus_ToRejectedWithReason_Success() {
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);
            when(kycRepository.save(any(CustomerKycRecord.class))).thenReturn(kycRecord);

            KycResponseDTO result = kycService.updateKycStatusDTO(1L, 2L, 
                    KycVerificationStatus.REJECTED, "Document is blurry");

            assertNotNull(result);
            assertEquals("REJECTED", result.getKycVerificationStatus());
            assertEquals("Document is blurry", kycRecord.getRejectionReason());
            assertNull(kycRecord.getVerifiedAt());
        }

        @Test
        @DisplayName("TC16: Update KYC status - record not found - covers yellow/red lines")
        void updateKycStatus_RecordNotFound_ThrowsException() {
            when(kycRepository.findByCustomerId(99L)).thenReturn(null);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> kycService.updateKycStatusDTO(99L, 2L, 
                            KycVerificationStatus.VERIFIED, null));

            assertEquals("KYC not found for customerId: 99", exception.getMessage());
        }

        @Test
        @DisplayName("TC17: Update to VERIFIED - user not found - covers yellow/red lines")
        void updateKycStatus_UserNotFound_ThrowsException() {
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> kycService.updateKycStatusDTO(1L, 2L, 
                            KycVerificationStatus.VERIFIED, null));

            assertEquals("User not found with id: 1", exception.getMessage());
        }

        @Test
        @DisplayName("TC18: Update to REJECTED when already VERIFIED - still allowed (different flow)")
        void updateKycStatus_RejectAfterVerified() {
            kycRecord.setKycVerificationStatus(KycVerificationStatus.VERIFIED);
            when(kycRepository.findByCustomerId(1L)).thenReturn(kycRecord);
            when(kycRepository.save(any(CustomerKycRecord.class))).thenReturn(kycRecord);

            KycResponseDTO result = kycService.updateKycStatusDTO(1L, 2L, 
                    KycVerificationStatus.REJECTED, "Document needs re-verification");

            assertNotNull(result);
            assertEquals("REJECTED", result.getKycVerificationStatus());
            assertEquals("Document needs re-verification", kycRecord.getRejectionReason());
        }
    }
}