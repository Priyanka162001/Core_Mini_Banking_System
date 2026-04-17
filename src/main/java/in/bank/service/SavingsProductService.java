package in.bank.service;

import java.util.List;
import in.bank.dto.SavingsProductRequestDTO;
import in.bank.entity.SavingsProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SavingsProductService {
    Long create(SavingsProductRequestDTO dto);
    SavingsProduct getById(Long id);
    Page<SavingsProduct> getAll(Pageable pageable); // ✅ UPDATED
    SavingsProduct update(Long id, SavingsProductRequestDTO dto);
    void delete(Long id);
}