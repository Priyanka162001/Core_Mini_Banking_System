package in.bank.controller;

import in.bank.dto.CompleteProfileRequestDTO;
import in.bank.dto.CustomerProfileResponseDTO;
import in.bank.dto.UpdateProfileRequestDTO;
import in.bank.entity.CustomerProfile;
import in.bank.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final CustomerProfileService customerProfileService;

    // Complete Profile
    @PostMapping("/profile/complete")
    public String completeProfile(@RequestParam Long customerId,
                                  @Valid @RequestBody CompleteProfileRequestDTO request){

        customerProfileService.completeProfile(customerId, request);

        return "Customer profile created successfully";
    }

    // Update Profile
    @PatchMapping("/profile/update")
    public CustomerProfile updateProfile(@RequestParam Long customerId,
                                         @RequestBody UpdateProfileRequestDTO request){

        return customerProfileService.updateProfile(customerId, request);
    }

    @GetMapping("/customer/{id}")
    public ResponseEntity<CustomerProfileResponseDTO> getProfile(@PathVariable Long id) {
        CustomerProfileResponseDTO dto = customerProfileService.getProfileDTO(id);
        return ResponseEntity.ok(dto);
    }

    // Get All Profiles
    @GetMapping
    public List<CustomerProfileResponseDTO> getAllProfiles() {
        List<CustomerProfile> profiles = customerProfileService.getAllProfiles();

        // Map entities to DTOs
        return profiles.stream().map(profile -> {
            CustomerProfileResponseDTO dto = new CustomerProfileResponseDTO();
            dto.setCustomerProfileId(profile.getId());
            dto.setCustomerId(profile.getCustomer().getId());
            dto.setFirstName(profile.getCustomer().getFirstName());
            dto.setLastName(profile.getCustomer().getLastName());
            dto.setUsername(profile.getUsername());
            dto.setDateOfBirth(profile.getDateOfBirth());
            dto.setGender(profile.getGender().name());
            dto.setRole(profile.getCustomer().getRole().name());
            dto.setStatus(profile.getCustomer().getStatus().name());
            
            // Contact info
            CustomerProfileResponseDTO.ContactInfo contact = new CustomerProfileResponseDTO.ContactInfo();
            contact.setEmail(profile.getCustomer().getEmail());
            contact.setPhoneNumber(profile.getCustomer().getPhoneNumber());
            contact.setEmailVerified(profile.getCustomer().getEmailVerified());
            dto.setContact(contact);

            // Address info
            CustomerProfileResponseDTO.AddressInfo address = new CustomerProfileResponseDTO.AddressInfo();
            address.setPermanent(profile.getPermanentAddressLine());
            address.setCurrent(profile.getCurrentAddressLine());
            address.setCity(profile.getCity());
            address.setState(profile.getState());
            address.setPostalCode(profile.getPostalCode());
            dto.setAddress(address);

            dto.setCreatedAt(profile.getCreatedAt());
            dto.setUpdatedAt(profile.getUpdatedAt());

            return dto;
        }).toList();
    }
}