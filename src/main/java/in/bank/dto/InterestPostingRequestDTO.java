package in.bank.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.YearMonth;

@Schema(name = "InterestPostingRequestDTO", description = "Request object for posting interest for a specific month and year")
public record InterestPostingRequestDTO(

    @NotNull(message = "Period must not be null")
    @DateTimeFormat(pattern = "yyyy-MM")
    @Schema(
        description = "The period (month and year) for which interest should be posted, format: yyyy-MM",
        example = "2026-04",
        required = true
    )
    YearMonth period

) {}