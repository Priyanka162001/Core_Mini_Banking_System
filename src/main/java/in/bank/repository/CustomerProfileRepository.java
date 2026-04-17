package in.bank.repository;

import in.bank.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    // Use underscore notation to navigate nested fields if CustomerProfile has a Customer object
    Optional<CustomerProfile> findByCustomer_Id(Long customerId);

    boolean existsByCustomerId(Long customerId);
    
    @Query("SELECT p FROM CustomerProfile p LEFT JOIN FETCH p.addresses WHERE p.customer.id = :customerId")
    Optional<CustomerProfile> findByCustomerIdWithAddresses(@Param("customerId") Long customerId);
}