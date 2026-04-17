package in.bank.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionCreateResponseDTO {

    private Long transactionId;
    private String message;
}