package in.bank.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterestHistoryDTO {
    private String period;
    private double amount;
    private String postedOn; // YYYY-MM-DD
}