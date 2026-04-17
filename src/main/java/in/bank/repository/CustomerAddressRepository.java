package in.bank.repository;

import in.bank.entity.AddressType;
import in.bank.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    // ✅ Get all addresses
    List<CustomerAddress> findByCustomerProfile_Id(Long customerProfileId);

    // ✅ Get active addresses
    List<CustomerAddress> findByCustomerProfile_IdAndIsActiveTrue(Long customerProfileId);

    // ✅ Get inactive addresses (🔥 NEW - REQUIRED)
    List<CustomerAddress> findByCustomerProfile_IdAndIsActiveFalse(Long customerProfileId);

    // ✅ Get active addresses by type (used in addAddress)
    List<CustomerAddress> findByCustomerProfile_IdAndAddressTypeAndIsActiveTrue(
            Long customerProfileId, AddressType addressType);
}