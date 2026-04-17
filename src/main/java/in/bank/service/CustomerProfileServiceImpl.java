package in.bank.service;

import in.bank.config.CorsConfig;
import in.bank.dto.CompleteProfileRequestDTO;
import in.bank.dto.CustomerProfileResponseDTO;
import in.bank.dto.UpdateProfileRequestDTO;
import in.bank.entity.CustomerAddress;
import in.bank.entity.CustomerProfile;
import in.bank.entity.UserRole;
import in.bank.entity.AppUser;
import in.bank.exception.AccessDeniedException;
import in.bank.exception.DuplicateResourceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.CustomerAddressRepository;
import in.bank.repository.CustomerProfileRepository;
import in.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CorsConfig corsConfig;

    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final UserRepository userRepository;

  

    // ─────────────────────────────────────────────
    // Complete Profile
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public void completeProfile(Long customerId, CompleteProfileRequestDTO request) {

        AppUser user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + customerId));

        if (customerProfileRepository.findByCustomer_Id(customerId).isPresent()) {
            throw new DuplicateResourceException(
                    "Profile already exists for customer id: " + customerId + ". Use the update endpoint instead.");
        }

        CustomerProfile profile = CustomerProfile.builder()
                .customer(user)
                .username(request.getUsername())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .build();

        CustomerProfile savedProfile = customerProfileRepository.save(profile);

        if (request.getAddresses() != null && !request.getAddresses().isEmpty()) {
            List<CustomerAddress> addresses = request.getAddresses().stream()
                    .map(addr -> CustomerAddress.builder()
                            .customerProfile(savedProfile)
                            .addressType(addr.getAddressType())
                            .addressLine(addr.getAddressLine())
                            .city(addr.getCity())
                            .state(addr.getState())
                            .postalCode(addr.getPostalCode())
                            .country(addr.getCountry())
                            .isActive(addr.getIsActive() != null ? addr.getIsActive() : true)
                            .build())
                    .collect(Collectors.toList());

            customerAddressRepository.saveAll(addresses);

            // ✅ Link saved addresses to profile
            savedProfile.setAddresses(addresses);
        }
    }

    // ─────────────────────────────────────────────
    // Get Profile DTO by customer ID
    // ─────────────────────────────────────────────
    @Override
    public CustomerProfileResponseDTO getCustomerProfile(Long id, AppUser currentUser) {
        CustomerProfile profile = customerProfileRepository
                .findByCustomer_Id(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));

        if (currentUser.getRole() == UserRole.ROLE_CUSTOMER && !currentUser.getId().equals(id)) {
            throw new AccessDeniedException("You are not authorized to view this profile");
        }

        return mapToDTO(profile);
    }

    // ─────────────────────────────────────────────
    // Update Profile
    // ─────────────────────────────────────────────
    @Override
    @Transactional
    public void updateProfile(Long customerId, UpdateProfileRequestDTO request) {

        CustomerProfile profile = customerProfileRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profile not found for customer id: " + customerId));

        // Update basic fields
        if (request.getUsername() != null) profile.setUsername(request.getUsername());
        if (request.getGender() != null)   profile.setGender(request.getGender());

        // Address Handling
        if (request.getAddressLine() != null && request.getAddressType() != null) {

            // Deactivate existing addresses of same type
            List<CustomerAddress> duplicates =
                    customerAddressRepository
                            .findByCustomerProfile_IdAndAddressTypeAndIsActiveTrue(
                                    profile.getId(), request.getAddressType());

            if (!duplicates.isEmpty()) {
                duplicates.forEach(old -> old.setIsActive(false));
                customerAddressRepository.saveAll(duplicates);
            }

            // Insert new address
            CustomerAddress newAddress = CustomerAddress.builder()
                    .customerProfile(profile)
                    .addressType(request.getAddressType())
                    .addressLine(request.getAddressLine())
                    .city(request.getCity())
                    .state(request.getState())
                    .postalCode(request.getPostalCode())
                    .country(request.getCountry())
                    .isActive(true)
                    .build();

            customerAddressRepository.save(newAddress);
        }

        customerProfileRepository.save(profile);
    }

    // ─────────────────────────────────────────────
    // Get Raw Profile Entity
    // ─────────────────────────────────────────────
    @Override
    public CustomerProfile getProfile(Long customerId) {
        return customerProfileRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for customer id: " + customerId));
    }

    // ─────────────────────────────────────────────
    // Get All Raw Profiles
    // ─────────────────────────────────────────────
    @Override
    public List<CustomerProfile> getAllProfiles() {
        return customerProfileRepository.findAll();
    }

    // ─────────────────────────────────────────────
    // Get All Profiles as DTOs
    // ─────────────────────────────────────────────
    @Override
    public Page<CustomerProfileResponseDTO> getAllProfileDTOs(Pageable pageable) {
        return customerProfileRepository.findAll(pageable)
                .map(this::mapToDTO); // ✅ was this::toDTO — wrong name
    }
     
    @Override
    public CustomerProfileResponseDTO getProfileDTO(Long customerId) {

        // ✅ Get currently logged-in user from Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        AppUser currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));

        // ✅ CUSTOMER can only view their own profile
        if (currentUser.getRole() == UserRole.ROLE_CUSTOMER
                && !currentUser.getId().equals(customerId)) {
            throw new AccessDeniedException(
                    "You are not authorized to view this profile");
        }

        // ✅ Fetch and return
        CustomerProfile profile = customerProfileRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profile not found for customer id: " + customerId));

        return mapToDTO(profile);
    }
    
    
    // ─────────────────────────────────────────────
    // Reusable Mapper: CustomerProfile → DTO
    // ─────────────────────────────────────────────
    private CustomerProfileResponseDTO mapToDTO(CustomerProfile profile) {

        CustomerProfileResponseDTO dto = new CustomerProfileResponseDTO();
        dto.setCustomerProfileId(profile.getId());
        dto.setCustomerId(profile.getCustomer().getId());
        dto.setFirstName(profile.getCustomer().getFirstName());
        dto.setLastName(profile.getCustomer().getLastName());
        dto.setUsername(profile.getUsername());
        dto.setDateOfBirth(profile.getDateOfBirth());
        dto.setGender(profile.getGender() != null ? profile.getGender().name() : null);
        dto.setRole(profile.getCustomer().getRole() != null ? profile.getCustomer().getRole().name() : null);
        dto.setStatus(profile.getCustomer().getStatus() != null ? profile.getCustomer().getStatus().name() : null);

        // Contact info
        CustomerProfileResponseDTO.ContactInfo contact = new CustomerProfileResponseDTO.ContactInfo();
        contact.setEmail(profile.getCustomer().getEmail());
        contact.setPhoneNumber(profile.getCustomer().getPhoneNumber());
        contact.setEmailVerified(profile.getCustomer().getEmailVerified());
        dto.setContact(contact);

        // Only active addresses returned
        List<CustomerAddress> rawAddresses = profile.getAddresses();
        List<CustomerProfileResponseDTO.AddressInfo> addressInfoList =
                (rawAddresses != null ? rawAddresses : Collections.<CustomerAddress>emptyList())
                        .stream()
                        .filter(addr -> Boolean.TRUE.equals(addr.getIsActive()))
                        .map(addr -> {
                            CustomerProfileResponseDTO.AddressInfo info = new CustomerProfileResponseDTO.AddressInfo();
                            info.setAddressId(addr.getId());
                            info.setAddressType(addr.getAddressType() != null ? addr.getAddressType().name() : null);
                            info.setAddressLine(addr.getAddressLine());
                            info.setCity(addr.getCity());
                            info.setState(addr.getState());
                            info.setPostalCode(addr.getPostalCode());
                            info.setCountry(addr.getCountry());
                            info.setIsActive(addr.getIsActive());
                            return info;
                        })
                        .collect(Collectors.toList());

        dto.setAddresses(addressInfoList);
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        dto.setCreatedBy(profile.getCreatedBy());
        dto.setUpdatedBy(profile.getUpdatedBy());

        return dto;
    }
}