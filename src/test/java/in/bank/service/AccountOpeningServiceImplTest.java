package in.bank.service;

import in.bank.dto.AccountOpeningRequestDTO;
import in.bank.dto.AccountOpeningResponseDTO;
import in.bank.entity.AccountOpeningRequest;
import in.bank.entity.ActionType;
import in.bank.entity.RequestStatus;
import in.bank.exception.BadRequestException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.AccountOpeningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountOpeningServiceImpl Tests")
class AccountOpeningServiceImplTest {

    @Mock
    private AccountOpeningRepository repository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountOpeningServiceImpl service;

    private AccountOpeningRequestDTO requestDTO;
    private AccountOpeningRequest pendingRequest;
    private AccountOpeningRequest approvedRequest;
    private AccountOpeningRequest rejectedRequest;
    private final Long USER_ID = 1L;
    private final Long ADMIN_ID = 2L;
    private final Long REQUEST_ID = 100L;

    @BeforeEach
    void setUp() {
        requestDTO = new AccountOpeningRequestDTO();
        requestDTO.setProductId(1L);
        requestDTO.setInitialDeposit(BigDecimal.valueOf(5000));

        pendingRequest = AccountOpeningRequest.builder()
                .id(REQUEST_ID)
                .userId(USER_ID)
                .productId(1L)
                .initialDeposit(BigDecimal.valueOf(5000))
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        approvedRequest = AccountOpeningRequest.builder()
                .id(REQUEST_ID + 1)
                .userId(USER_ID)
                .productId(2L)
                .initialDeposit(BigDecimal.valueOf(10000))
                .status(RequestStatus.APPROVED)
                .reviewedBy(ADMIN_ID)
                .build();

        rejectedRequest = AccountOpeningRequest.builder()
                .id(REQUEST_ID + 2)
                .userId(USER_ID)
                .productId(3L)
                .initialDeposit(BigDecimal.valueOf(7500))
                .status(RequestStatus.REJECTED)
                .rejectionReason("Insufficient credit score")
                .reviewedBy(ADMIN_ID)
                .build();
    }

    @Nested
    @DisplayName("createRequest() Tests")
    class CreateRequestTests {
    	@Test
    	@DisplayName("TC1: Create request successfully - covers red line")
    	void createRequest_Success() {
    	    when(repository.existsByUserIdAndProductIdAndStatus(USER_ID, 1L, RequestStatus.PENDING))
    	            .thenReturn(false);
    	    
    	    // Use doAnswer to modify the saved entity and set its ID
    	    when(repository.save(any(AccountOpeningRequest.class))).thenAnswer(invocation -> {
    	        AccountOpeningRequest requestToSave = invocation.getArgument(0);
    	        // Set the ID on the entity that will be returned
    	        requestToSave.setId(REQUEST_ID);
    	        return requestToSave;
    	    });

    	    Long result = service.createRequest(USER_ID, requestDTO);

    	    assertThat(result).isEqualTo(REQUEST_ID);
    	    
    	    verify(repository, times(1)).save(any(AccountOpeningRequest.class));
    	}

        @Test
        @DisplayName("TC2: Duplicate pending request - covers yellow line")
        void createRequest_DuplicatePendingRequest_ThrowsBadRequestException() {
            when(repository.existsByUserIdAndProductIdAndStatus(USER_ID, 1L, RequestStatus.PENDING))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createRequest(USER_ID, requestDTO))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("pending request already exists");

            verify(repository, never()).save(any(AccountOpeningRequest.class));
        }
    }

    @Nested
    @DisplayName("getMyRequests() Tests")
    class GetMyRequestsTests {

        @Test
        @DisplayName("TC3: Get my requests - returns user requests")
        void getMyRequests_ReturnsUserRequests() {
            List<AccountOpeningRequest> userRequests = Arrays.asList(pendingRequest, approvedRequest, rejectedRequest);
            when(repository.findByUserId(USER_ID)).thenReturn(userRequests);

            List<AccountOpeningResponseDTO> result = service.getMyRequests(USER_ID);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(result.get(1).getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(result.get(2).getStatus()).isEqualTo(RequestStatus.REJECTED);
            verify(repository, times(1)).findByUserId(USER_ID);
        }

        @Test
        @DisplayName("TC4: Get my requests - empty list when no requests")
        void getMyRequests_ReturnsEmptyList_WhenNoRequests() {
            when(repository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<AccountOpeningResponseDTO> result = service.getMyRequests(USER_ID);

            assertThat(result).isEmpty();
            verify(repository, times(1)).findByUserId(USER_ID);
        }
    }

    @Nested
    @DisplayName("getById() Tests")
    class GetByIdTests {

        @Test
        @DisplayName("TC5: Admin can view any request")
        void getById_Admin_CanViewAnyRequest() {
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));

            AccountOpeningResponseDTO result = service.getById(REQUEST_ID, USER_ID, true);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(REQUEST_ID);
            verify(repository, times(1)).findById(REQUEST_ID);
        }

        @Test
        @DisplayName("TC6: Customer can view their own request")
        void getById_Customer_OwnRequest() {
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));

            AccountOpeningResponseDTO result = service.getById(REQUEST_ID, USER_ID, false);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(REQUEST_ID);
        }

        @Test
        @DisplayName("TC7: Customer cannot view another customer's request - covers yellow/red lines")
        void getById_Customer_AnotherCustomerRequest_ThrowsAccessDeniedException() {
            Long differentUserId = 999L;
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));

            assertThatThrownBy(() -> service.getById(REQUEST_ID, differentUserId, false))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("You are not authorized to view this request");
        }

        @Test
        @DisplayName("TC8: Request not found - covers yellow/red lines")
        void getById_RequestNotFound_ThrowsResourceNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(999L, USER_ID, true))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Request not found with id: 999");
        }
    }

    @Nested
    @DisplayName("getAllRequests() Tests")
    class GetAllRequestsTests {

        @Test
        @DisplayName("TC9: Get all requests with status filter - covers ternary operator")
        void getAllRequests_WithStatusFilter() {
            List<AccountOpeningRequest> requests = Arrays.asList(pendingRequest, approvedRequest);
            when(repository.findByStatus(RequestStatus.PENDING)).thenReturn(requests);

            List<AccountOpeningResponseDTO> result = service.getAllRequests(RequestStatus.PENDING);

            assertThat(result).hasSize(2);
            verify(repository, times(1)).findByStatus(RequestStatus.PENDING);
            verify(repository, never()).findAll();
        }

        @Test
        @DisplayName("TC10: Get all requests without status filter - covers ternary operator else branch")
        void getAllRequests_WithoutStatusFilter() {
            List<AccountOpeningRequest> requests = Arrays.asList(pendingRequest, approvedRequest, rejectedRequest);
            when(repository.findAll()).thenReturn(requests);

            List<AccountOpeningResponseDTO> result = service.getAllRequests(null);

            assertThat(result).hasSize(3);
            verify(repository, times(1)).findAll();
            verify(repository, never()).findByStatus(any());
        }

        @Test
        @DisplayName("TC11: Get all requests - empty list")
        void getAllRequests_EmptyList() {
            when(repository.findAll()).thenReturn(Collections.emptyList());

            List<AccountOpeningResponseDTO> result = service.getAllRequests(null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("takeAction() Tests")
    class TakeActionTests {

        @Test
        @DisplayName("TC12: Approve request successfully - covers switch case APPROVE")
        void takeAction_ApproveRequest_CreatesAccount() {
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));
            doNothing().when(accountService).createAccountAfterApproval(pendingRequest);
            when(repository.save(any(AccountOpeningRequest.class))).thenReturn(pendingRequest);

            service.takeAction(REQUEST_ID, ADMIN_ID, ActionType.APPROVE, null);

            assertThat(pendingRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(pendingRequest.getReviewedBy()).isEqualTo(ADMIN_ID);
            verify(accountService, times(1)).createAccountAfterApproval(pendingRequest);
            verify(repository, times(1)).save(pendingRequest);
        }

        @Test
        @DisplayName("TC13: Reject request with valid reason - covers switch case REJECT")
        void takeAction_RejectRequest_WithValidReason() {
            String rejectionReason = "Insufficient KYC documents";
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));
            when(repository.save(any(AccountOpeningRequest.class))).thenReturn(pendingRequest);

            service.takeAction(REQUEST_ID, ADMIN_ID, ActionType.REJECT, rejectionReason);

            assertThat(pendingRequest.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(pendingRequest.getRejectionReason()).isEqualTo(rejectionReason);
            assertThat(pendingRequest.getReviewedBy()).isEqualTo(ADMIN_ID);
            verify(accountService, never()).createAccountAfterApproval(any());
            verify(repository, times(1)).save(pendingRequest);
        }

        @Test
        @DisplayName("TC14: Reject request without reason - throws exception - covers yellow/red lines")
        void takeAction_RejectRequest_WithoutReason_ThrowsBadRequestException() {
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));

            assertThatThrownBy(() -> service.takeAction(REQUEST_ID, ADMIN_ID, ActionType.REJECT, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Rejection reason is required");

            assertThat(pendingRequest.getStatus()).isEqualTo(RequestStatus.PENDING);
            verify(accountService, never()).createAccountAfterApproval(any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC15: Reject request with blank reason - throws exception - covers yellow/red lines")
        void takeAction_RejectRequest_WithBlankReason_ThrowsBadRequestException() {
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));

            assertThatThrownBy(() -> service.takeAction(REQUEST_ID, ADMIN_ID, ActionType.REJECT, "   "))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Rejection reason is required");

            verify(accountService, never()).createAccountAfterApproval(any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC16: Request already processed - covers yellow/red lines")
        void takeAction_RequestAlreadyProcessed_ThrowsBadRequestException() {
            when(repository.findById(REQUEST_ID + 1)).thenReturn(Optional.of(approvedRequest));

            assertThatThrownBy(() -> service.takeAction(REQUEST_ID + 1, ADMIN_ID, ActionType.APPROVE, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Request has already been processed");

            verify(accountService, never()).createAccountAfterApproval(any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC17: Request already processed - REJECT on already processed request")
        void takeAction_RejectOnAlreadyProcessedRequest_ThrowsBadRequestException() {
            when(repository.findById(REQUEST_ID + 1)).thenReturn(Optional.of(approvedRequest));

            assertThatThrownBy(() -> service.takeAction(REQUEST_ID + 1, ADMIN_ID, ActionType.REJECT, "Some reason"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Request has already been processed");

            verify(accountService, never()).createAccountAfterApproval(any());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC18: Request not found - covers yellow/red lines")
        void takeAction_RequestNotFound_ThrowsResourceNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.takeAction(999L, ADMIN_ID, ActionType.APPROVE, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Request not found with id: 999");

            verify(accountService, never()).createAccountAfterApproval(any());
            verify(repository, never()).save(any());
        }
        
        @Test
        @DisplayName("TC19: Verify save is called after approve action")
        void takeAction_Approve_VerifySaveCalled() {
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));
            doNothing().when(accountService).createAccountAfterApproval(pendingRequest);
            when(repository.save(any(AccountOpeningRequest.class))).thenReturn(pendingRequest);

            service.takeAction(REQUEST_ID, ADMIN_ID, ActionType.APPROVE, null);

            verify(repository, times(1)).save(pendingRequest);
        }
        
        @Test
        @DisplayName("TC20: Verify save is called after reject action")
        void takeAction_Reject_VerifySaveCalled() {
            String rejectionReason = "Document verification failed";
            when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(pendingRequest));
            when(repository.save(any(AccountOpeningRequest.class))).thenReturn(pendingRequest);

            service.takeAction(REQUEST_ID, ADMIN_ID, ActionType.REJECT, rejectionReason);

            verify(repository, times(1)).save(pendingRequest);
        }
    }
}