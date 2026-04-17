package in.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import in.bank.entity.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "Request body for creating a transaction")
public class TransactionRequestDTO {

    @Schema(description = "Type of transaction", example = "DEPOSIT")
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @Schema(description = "Account ID to transact on", example = "1")
    @NotNull(message = "Account ID is required")
    private Long accountId;

    @Schema(description = "Amount to deposit or withdraw", example = "5000.00")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "INR")
    @NotNull(message = "Currency is required")
    private Currency currency;

    @Schema(description = "Payment mode", example = "UPI")
    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @Schema(description = "Optional description", example = "Initial deposit")
    @Size(max = 255, message = "Description too long")
    private String description;
}