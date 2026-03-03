package in.bank.service;



import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import in.bank.dto.SavingsProductRequestDTO;
import in.bank.entity.SavingsProduct;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.SavingsProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsProductServiceImpl implements SavingsProductService {

    private final SavingsProductRepository repository;

    @Override
    public Long create(SavingsProductRequestDTO dto) {

        if (repository.existsByProductName(dto.getProductName())) {
            throw new IllegalArgumentException("Product name already exists");
        }

        SavingsProduct product = SavingsProduct.builder()
                .productName(dto.getProductName())
                .interestRatePercent(dto.getInterestRatePercent())
                .minimumOpeningBalanceAmount(dto.getMinimumOpeningBalanceAmount())
                .minimumMaintainingBalanceAmount(dto.getMinimumMaintainingBalanceAmount())
                .interestApplicationFrequencyCode(dto.getInterestApplicationFrequencyCode())
                .productStatus(dto.getProductStatus())
                .effectiveFromDate(dto.getEffectiveFromDate())
                .expiryDate(dto.getExpiryDate())
                .build();

        SavingsProduct saved = repository.save(product);

        return saved.getSavingsProductId();   // ✅ only ID
    }

    @Override
    public SavingsProduct getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    @Override
    public List<SavingsProduct> getAll() {
        return repository.findAll();
    }

    @Override
    public SavingsProduct update(Long id, SavingsProductRequestDTO dto) {

        SavingsProduct product = getById(id);

        product.setProductName(dto.getProductName());
        product.setInterestRatePercent(dto.getInterestRatePercent());
        product.setMinimumOpeningBalanceAmount(dto.getMinimumOpeningBalanceAmount());
        product.setMinimumMaintainingBalanceAmount(dto.getMinimumMaintainingBalanceAmount());
        product.setInterestApplicationFrequencyCode(dto.getInterestApplicationFrequencyCode());
        product.setProductStatus(dto.getProductStatus());
        product.setEffectiveFromDate(dto.getEffectiveFromDate());
        product.setExpiryDate(dto.getExpiryDate());

        return repository.save(product);
    }

    @Override
    public void delete(Long id) {
        SavingsProduct product = getById(id);
        repository.delete(product);
    }
}