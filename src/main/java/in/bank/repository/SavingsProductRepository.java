package in.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import in.bank.entity.SavingsProduct;
import java.util.Optional;

public interface SavingsProductRepository extends JpaRepository<SavingsProduct, Long> {
    boolean existsByProductName(String productName);
    Optional<SavingsProduct> findByProductName(String productName);
    boolean existsByProductCodeAndIdNot(String productCode, Long id); // ✅ ADD THIS
}