package in.bank.service;

import in.bank.dto.CompleteProfileRequestDTO;
import in.bank.dto.CustomerProfileResponseDTO;
import in.bank.dto.UpdateProfileRequestDTO;
import in.bank.entity.AppUser;
import in.bank.entity.CustomerProfile;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerProfileService {

    void completeProfile(Long customerId, CompleteProfileRequestDTO request);

    CustomerProfileResponseDTO getProfileDTO(Long customerId);

    void updateProfile(Long customerId, UpdateProfileRequestDTO request); // ✅ void, not CustomerProfile

    CustomerProfile getProfile(Long customerId);

    List<CustomerProfile> getAllProfiles();

    
    CustomerProfileResponseDTO getCustomerProfile(Long customerId, AppUser currentUser);

    Page<CustomerProfileResponseDTO> getAllProfileDTOs(Pageable pageable);

    
}