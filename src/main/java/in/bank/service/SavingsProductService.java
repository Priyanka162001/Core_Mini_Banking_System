package in.bank.service;



import java.util.List;

import in.bank.dto.SavingsProductRequestDTO;
import in.bank.entity.SavingsProduct;

public interface SavingsProductService {

	Long create(SavingsProductRequestDTO dto);    
    

    SavingsProduct getById(Long id);

    List<SavingsProduct> getAll();

    SavingsProduct update(Long id, SavingsProductRequestDTO dto);

    void delete(Long id);
}