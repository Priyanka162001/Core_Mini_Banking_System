package in.bank.service;

import in.bank.dto.AccountOpeningRequestDTO;
import in.bank.dto.AccountOpeningResponseDTO;
import in.bank.entity.ActionType;
import in.bank.entity.RequestStatus;

import java.util.List;

public interface AccountOpeningService {

    Long createRequest(Long userId, AccountOpeningRequestDTO dto);

    List<AccountOpeningResponseDTO> getMyRequests(Long userId);

    void takeAction(Long requestId, Long adminId, ActionType action, String reason);

    AccountOpeningResponseDTO getById(Long requestId, Long userId, boolean isAdmin);

    List<AccountOpeningResponseDTO> getAllRequests(RequestStatus status); // ← was __RequestStatus__
}