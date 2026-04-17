package in.bank.repository;

import in.bank.entity.AccountOpeningRequest;
import in.bank.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountOpeningRepository extends JpaRepository<AccountOpeningRequest, Long> {

    boolean existsByUserIdAndProductIdAndStatus(Long userId, Long productId, RequestStatus status);

    List<AccountOpeningRequest> findByUserId(Long userId);

    List<AccountOpeningRequest> findByStatus(RequestStatus status);
}