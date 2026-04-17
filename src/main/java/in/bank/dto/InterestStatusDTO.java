package in.bank.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterestStatusDTO {
    private String period;
    private long totalAccounts;
    private long success;
    private long failed;
}