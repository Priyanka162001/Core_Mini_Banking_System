package in.bank.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterestPreviewDTO {
    private Long accountId;
    private double interestAmount;
    private String period; // "2026-03"
}