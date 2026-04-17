package in.bank.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfileResponseDTO {

    private Long customerProfileId;
    private Long customerId;
    private String firstName;
    private String lastName;
    private String username;
    private LocalDate dateOfBirth;
    private String gender; 
    private String role;
    private String status;

    private ContactInfo contact;
    private List<AddressInfo> addresses;      // ✅ was: AddressInfo address (single, old fields)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfo {
        private String email;
        private String phoneNumber;
        private Boolean emailVerified;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressInfo {         // ✅ was: permanent, current (flat old fields)
        private Long addressId;
        private String addressType;
        private String addressLine;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private Boolean isActive;
    }
}