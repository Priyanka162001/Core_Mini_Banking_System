package in.bank.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestPostingResponseDTO {

    private Long id;
    private Long accountId;
    private BigDecimal interestAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal annualInterestRate;
    private Integer postingMonth;
    private Integer postingYear;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;   // ✅ NEW
    private Long createdBy;            // ✅ Long to match entity
    private Long updatedBy;            // ✅ NEW
}