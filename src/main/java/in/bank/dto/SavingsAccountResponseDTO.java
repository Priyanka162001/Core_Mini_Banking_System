package in.bank.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SavingsAccountResponseDTO {
    private Long id;
    private String accountNumber;
    private String accountStatus;
    private BigDecimal currentBalanceAmount;
    private Long userId;
    private String productName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;  // ✅ make sure this is present
    private Long createdBy;
    private Long updatedBy;
}