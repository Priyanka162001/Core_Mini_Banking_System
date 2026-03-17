package in.bank.dto;

import in.bank.entity.GenderType;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequestDTO {

    private String username;

    private GenderType gender;

    private String permanentAddressLine;

    private String currentAddressLine;

    @Pattern(regexp = "^[A-Za-z ]+$", message = "City must contain only letters")
    private String city;

    @Pattern(regexp = "^[A-Za-z ]+$", message = "State must contain only letters")
    private String state;

    @Pattern(regexp = "^[0-9]{6}$", message = "Postal code must be 6 digits")
    private String postalCode;

}