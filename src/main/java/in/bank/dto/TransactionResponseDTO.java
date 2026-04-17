package in.bank.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import in.bank.entity.Currency;
import in.bank.entity.PaymentMode;
import in.bank.entity.TransactionType;

@Data
@Builder
public class TransactionResponseDTO {

    private Long transactionId;
   private TransactionType type;
    private BigDecimal amount;

    private BigDecimal balanceBeforeTransaction;
    private BigDecimal balanceAfterTransaction;

    private Currency currency;
    private PaymentMode paymentMode;
    private String description;

 // ✅ NEW FIELDS
    private BigDecimal interestPosting;
    private LocalDateTime interestPostedAt;
    private LocalDateTime transactionDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long createdBy;
    private Long updatedBy;
}