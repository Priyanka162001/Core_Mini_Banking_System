package in.bank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KycRequestDTO {

    private String panIdentifier;
    private String aadhaarIdentifier;
    private Long customerId; // ✅ add this

}