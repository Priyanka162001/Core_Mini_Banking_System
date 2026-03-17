package in.bank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequestDTO {

    private String email;
    private String otp;

}