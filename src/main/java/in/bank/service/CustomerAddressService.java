package in.bank.service;

import in.bank.dto.AddressRequestDTO;
import in.bank.dto.AddressResponseDTO;
import in.bank.entity.AddressStatus;

import java.util.List;

public interface CustomerAddressService {

    AddressResponseDTO addAddress(Long customerId, AddressRequestDTO request);

    List<AddressResponseDTO> getAllAddresses(Long customerId);

    List<AddressResponseDTO> getActiveAddresses(Long customerId);

    // ✅ ADD THIS
    List<AddressResponseDTO> getInactiveAddresses(Long customerId);

    // ✅ ADD THIS
    List<AddressResponseDTO> getAddressesByStatus(Long customerId, AddressStatus status);

    AddressResponseDTO updateAddress(Long addressId, AddressRequestDTO request);

    void deactivateAddress(Long addressId);
    
}