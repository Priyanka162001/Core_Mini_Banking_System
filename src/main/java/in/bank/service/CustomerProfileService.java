package in.bank.service;

import in.bank.dto.CompleteProfileRequestDTO;
import in.bank.dto.CustomerProfileResponseDTO;
import in.bank.dto.UpdateProfileRequestDTO;
import in.bank.entity.CustomerProfile;

import java.util.List;

public interface CustomerProfileService {

    void completeProfile(Long customerId, CompleteProfileRequestDTO request);

    CustomerProfile updateProfile(Long customerId, UpdateProfileRequestDTO request);

    CustomerProfile getProfile(Long customerId);

    List<CustomerProfile> getAllProfiles();
    
    CustomerProfileResponseDTO getProfileDTO(Long customerId);

}