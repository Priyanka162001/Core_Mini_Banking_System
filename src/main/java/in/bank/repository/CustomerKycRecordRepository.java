package in.bank.repository;

import in.bank.entity.CustomerKycRecord;
import in.bank.entity.KycVerificationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerKycRecordRepository extends JpaRepository<CustomerKycRecord, Long> {

    List<CustomerKycRecord> findByKycVerificationStatus(KycVerificationStatus status);

    CustomerKycRecord findByCustomerId(Long customerId);

}