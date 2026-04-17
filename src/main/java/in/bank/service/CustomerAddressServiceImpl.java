package in.bank.service;

import in.bank.dto.AddressRequestDTO;
import in.bank.dto.AddressResponseDTO;
import in.bank.entity.AddressStatus;
import in.bank.entity.CustomerAddress;
import in.bank.entity.CustomerProfile;
import in.bank.repository.CustomerAddressRepository;
import in.bank.repository.CustomerProfileRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerProfileRepository customerProfileRepository;

    // ✅ ADD ADDRESS
    @Override
    @Transactional
    public AddressResponseDTO addAddress(Long customerId, AddressRequestDTO request) {

        CustomerProfile profile = customerProfileRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // ✅ Deactivate existing active address of same type
        if (request.getAddressType() != null) {
            List<CustomerAddress> existing = customerAddressRepository
                    .findByCustomerProfile_IdAndAddressTypeAndIsActiveTrue(
                            profile.getId(), request.getAddressType());

            if (!existing.isEmpty()) {
                existing.forEach(old -> old.setIsActive(false));
                customerAddressRepository.saveAll(existing);
            }
        }

        CustomerAddress address = CustomerAddress.builder()
                .customerProfile(profile)
                .addressType(request.getAddressType())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mapToDTO(customerAddressRepository.save(address));
    }

    // ✅ MAIN METHOD (USED BY CONTROLLER)
    @Override
    public List<AddressResponseDTO> getAddressesByStatus(Long customerId, AddressStatus status) {
        switch (status) {
            case ACTIVE:   return getActiveAddresses(customerId);
            case INACTIVE: return getInactiveAddresses(customerId);
            case ALL:
            default:       return getAllAddresses(customerId);
        }
    }

    // ✅ GET ALL — resolves customerId → profileId first
    @Override
    public List<AddressResponseDTO> getAllAddresses(Long customerId) {
        CustomerProfile profile = customerProfileRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        return customerAddressRepository
                .findByCustomerProfile_Id(profile.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ✅ GET ACTIVE — resolves customerId → profileId first
    @Override
    public List<AddressResponseDTO> getActiveAddresses(Long customerId) {
        CustomerProfile profile = customerProfileRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        return customerAddressRepository
                .findByCustomerProfile_IdAndIsActiveTrue(profile.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ✅ GET INACTIVE — resolves customerId → profileId first
    @Override
    public List<AddressResponseDTO> getInactiveAddresses(Long customerId) {
        CustomerProfile profile = customerProfileRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        return customerAddressRepository
                .findByCustomerProfile_IdAndIsActiveFalse(profile.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ✅ UPDATE ADDRESS
    @Override
    @Transactional
    public AddressResponseDTO updateAddress(Long addressId, AddressRequestDTO request) {
        CustomerAddress address = customerAddressRepository
                .findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (request.getAddressType() != null) address.setAddressType(request.getAddressType());
        if (request.getAddressLine() != null) address.setAddressLine(request.getAddressLine());
        if (request.getCity() != null)        address.setCity(request.getCity());
        if (request.getState() != null)       address.setState(request.getState());
        if (request.getPostalCode() != null)  address.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null)     address.setCountry(request.getCountry());

        return mapToDTO(customerAddressRepository.save(address));
    }

    // ✅ DEACTIVATE ADDRESS
    @Override
    @Transactional
    public void deactivateAddress(Long addressId) {
        CustomerAddress address = customerAddressRepository
                .findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        address.setIsActive(false);
        customerAddressRepository.save(address);
    }

    // ✅ MAPPER
    private AddressResponseDTO mapToDTO(CustomerAddress address) {
        return AddressResponseDTO.builder()
                .addressId(address.getId())
                .customerProfileId(address.getCustomerProfile().getId())
                .addressType(address.getAddressType().name())
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .isActive(address.getIsActive())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .createdBy(address.getCreatedBy())   // ✅ NEW
                .updatedBy(address.getUpdatedBy())   // ✅ NEW
                .build();
    }
}