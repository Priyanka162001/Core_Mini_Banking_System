package in.bank.service;

import in.bank.dto.CompleteProfileRequestDTO;
import in.bank.dto.CustomerProfileResponseDTO;
import in.bank.dto.UpdateProfileRequestDTO;
import in.bank.entity.AppUser;
import in.bank.entity.CustomerProfile;
import in.bank.repository.CustomerProfileRepository;
import in.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;

    @Override
    public void completeProfile(Long customerId, CompleteProfileRequestDTO request) {

        AppUser user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CustomerProfile profile = CustomerProfile.builder()
                .customer(user)
                .username(request.getUsername())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .permanentAddressLine(request.getPermanentAddressLine())
                .currentAddressLine(request.getCurrentAddressLine())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customerProfileRepository.save(profile);
    }
       
    
    @Override
    public CustomerProfileResponseDTO getProfileDTO(Long customerId) {

        CustomerProfile profile = customerProfileRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        CustomerProfileResponseDTO dto = new CustomerProfileResponseDTO();
        dto.setCustomerProfileId(profile.getId());
        dto.setCustomerId(profile.getCustomer().getId());
        dto.setFirstName(profile.getCustomer().getFirstName());
        dto.setLastName(profile.getCustomer().getLastName());
        dto.setUsername(profile.getUsername());
        dto.setDateOfBirth(profile.getDateOfBirth());
        dto.setGender(profile.getGender().name());  // Enum → String
        dto.setRole(profile.getCustomer().getRole().name()); // Enum → String
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
    }
    
    @Override
    public CustomerProfile updateProfile(Long customerId, UpdateProfileRequestDTO request) {

        CustomerProfile profile = customerProfileRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        if(request.getUsername() != null){
            profile.setUsername(request.getUsername());
        }

        if(request.getGender() != null){
            profile.setGender(request.getGender());
        }

        if(request.getPermanentAddressLine() != null){
            profile.setPermanentAddressLine(request.getPermanentAddressLine());
        }

        if(request.getCurrentAddressLine() != null){
            profile.setCurrentAddressLine(request.getCurrentAddressLine());
        }

        if(request.getCity() != null){
            profile.setCity(request.getCity());
        }

        if(request.getState() != null){
            profile.setState(request.getState());
        }

        if(request.getPostalCode() != null){
            profile.setPostalCode(request.getPostalCode());
        }

        profile.setUpdatedAt(LocalDateTime.now());

        return customerProfileRepository.save(profile);
    }

    @Override
    public CustomerProfile getProfile(Long customerId) {

        return customerProfileRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }

    @Override
    public List<CustomerProfile> getAllProfiles() {

        return customerProfileRepository.findAll();
    }
}