package in.bank.service;

import in.bank.dto.TransactionCreateResponseDTO;
import in.bank.dto.TransactionRequestDTO;
import in.bank.dto.TransactionResponseDTO;
import in.bank.dto.TransactionSummaryDTO;
import in.bank.entity.TransactionType;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;

public interface TransactionService {
    TransactionCreateResponseDTO processTransaction(
        String idempotencyKey, TransactionRequestDTO request, UserDetails userDetails); // ✅ added
    List<TransactionResponseDTO> getAllTransactionsDTO(Long accountId);
    TransactionResponseDTO getTransactionById(Long accountId, Long transactionId);
    List<TransactionResponseDTO> filterTransactions(
            Long accountId, TransactionType type, LocalDateTime start, LocalDateTime end);
    List<TransactionResponseDTO> getTransactionsForLoggedInUser();
    TransactionSummaryDTO getTransactionSummary(Long accountId);
}