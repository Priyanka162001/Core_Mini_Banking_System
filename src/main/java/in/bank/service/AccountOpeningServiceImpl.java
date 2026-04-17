package in.bank.service;

import in.bank.dto.AccountOpeningRequestDTO;
import in.bank.dto.AccountOpeningResponseDTO;
import in.bank.entity.AccountOpeningRequest;
import in.bank.entity.ActionType;
import in.bank.entity.RequestStatus;
import in.bank.exception.BadRequestException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.AccountOpeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountOpeningServiceImpl implements AccountOpeningService {

    private final AccountOpeningRepository repository;
    private final AccountService accountService;

    // ─────────────────────────────────────────────
    // CREATE REQUEST
    // ─────────────────────────────────────────────
    @Override
    public Long createRequest(Long userId, AccountOpeningRequestDTO dto) {

        boolean exists = repository.existsByUserIdAndProductIdAndStatus(
                userId, dto.getProductId(), RequestStatus.PENDING);

        if (exists) {
            throw new BadRequestException("A pending request already exists for this product");
        }

        AccountOpeningRequest request = AccountOpeningRequest.builder()
                .userId(userId)
                .productId(dto.getProductId())
                .initialDeposit(dto.getInitialDeposit())
                .status(RequestStatus.PENDING)
                .build(); // ❌ remove createdBy & createdAt

        repository.save(request);
        return request.getId();
    }

    // ─────────────────────────────────────────────
    // GET MY REQUESTS (CUSTOMER)
    // ─────────────────────────────────────────────
    @Override
    public List<AccountOpeningResponseDTO> getMyRequests(Long userId) {

        return repository.findByUserId(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // GET BY ID (CUSTOMER sees own only / ADMIN sees any)
    // ─────────────────────────────────────────────
    @Override
    public AccountOpeningResponseDTO getById(Long requestId, Long userId, boolean isAdmin) {

        AccountOpeningRequest req = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Request not found with id: " + requestId));

        // CUSTOMER can only view their own request
        if (!isAdmin && !req.getUserId().equals(userId)) {
            throw new AccessDeniedException(
                    "You are not authorized to view this request");
        }

        return toDTO(req);
    }

    // ─────────────────────────────────────────────
    // GET ALL REQUESTS (ADMIN only, optional status filter)
    // ─────────────────────────────────────────────
    @Override
    public List<AccountOpeningResponseDTO> getAllRequests(RequestStatus status) {

        List<AccountOpeningRequest> requests = (status != null)
                ? repository.findByStatus(status)
                : repository.findAll();

        return requests.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // APPROVE / REJECT (ADMIN)
    // ─────────────────────────────────────────────
    @Override
    public void takeAction(Long requestId, Long adminId, ActionType action, String reason) {

        AccountOpeningRequest req = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Request not found with id: " + requestId));

        if (req.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Request has already been processed");
        }

        switch (action) {

            case APPROVE -> {
                req.setStatus(RequestStatus.APPROVED);
                req.setReviewedBy(adminId);
                accountService.createAccountAfterApproval(req); // 🔥 triggers account creation
            }

            case REJECT -> {
                if (reason == null || reason.isBlank()) {
                    throw new BadRequestException("Rejection reason is required");
                }
                req.setStatus(RequestStatus.REJECTED);
                req.setReviewedBy(adminId);
                req.setRejectionReason(reason);
            }
        }

        

        repository.save(req);
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPER — entity → DTO
    // ─────────────────────────────────────────────
    private AccountOpeningResponseDTO toDTO(AccountOpeningRequest req) {
        return AccountOpeningResponseDTO.builder()
                .id(req.getId())
                .productId(req.getProductId())
                .initialDeposit(req.getInitialDeposit())
                .status(req.getStatus())
                .rejectionReason(req.getRejectionReason())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .createdBy(req.getCreatedBy())
                .updatedBy(req.getUpdatedBy())
                .build();
    }
}