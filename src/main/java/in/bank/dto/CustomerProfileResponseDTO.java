package in.bank.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerProfileResponseDTO {

    private Long customerProfileId;
    private Long customerId;
    private String firstName;
    private String lastName;
    private String username;
    private LocalDate dateOfBirth;
    private String gender;
    private ContactInfo contact;
    private AddressInfo address;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    public static class ContactInfo {
        private String email;
        private String phoneNumber;
        private Boolean emailVerified;
    }

    @Getter
    @Setter
    public static class AddressInfo {
        private String permanent;
        private String current;
        private String city;
        private String state;
        private String postalCode;
    }
}