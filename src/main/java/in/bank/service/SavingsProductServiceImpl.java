package in.bank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import in.bank.dto.SavingsProductRequestDTO;
import in.bank.entity.SavingsProduct;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.SavingsProductRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingsProductServiceImpl implements SavingsProductService {

    private final SavingsProductRepository repository;

    @Override
    public Long create(SavingsProductRequestDTO dto) {

        // Duplicate product name validation
        if (repository.existsByProductName(dto.getProductName())) {
            throw new IllegalArgumentException("Product name already exists");
        }

        // Age validation
        if (dto.getMinAge() >= dto.getMaxAge()) {
            throw new IllegalArgumentException("Minimum age must be less than maximum age");
        }

        // Balance validation
        if (dto.getMinimumMaintainingBalanceAmount()
                .compareTo(dto.getMinimumOpeningBalanceAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Maintaining balance cannot be greater than opening balance");
        }

        SavingsProduct product = SavingsProduct.builder()
                .productCode(dto.getProductCode())
                .productName(dto.getProductName())
                .interestRatePercent(dto.getInterestRatePercent())
                .minimumOpeningBalanceAmount(dto.getMinimumOpeningBalanceAmount())
                .minimumMaintainingBalanceAmount(dto.getMinimumMaintainingBalanceAmount())
                .interestApplicationFrequencyCode(dto.getInterestApplicationFrequencyCode())
                .productStatus(dto.getProductStatus())
                .effectiveFromDate(LocalDate.now()) // auto date
                .expiryDate(dto.getExpiryDate())
                .minAge(dto.getMinAge())
                .maxAge(dto.getMaxAge())
                .build();

        SavingsProduct saved = repository.save(product);
        return saved.getId();
    }

    @Override
    public SavingsProduct getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    public List<SavingsProduct> getAll() {
        return repository.findAll();
    }

    @Override
    public SavingsProduct update(Long id, SavingsProductRequestDTO dto) {

        SavingsProduct product = getById(id);

        // Age validation
        if (dto.getMinAge() >= dto.getMaxAge()) {
            throw new IllegalArgumentException("Minimum age must be less than maximum age");
        }

        // Balance validation
        if (dto.getMinimumMaintainingBalanceAmount()
                .compareTo(dto.getMinimumOpeningBalanceAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Maintaining balance cannot be greater than opening balance");
        }

        product.setProductCode(dto.getProductCode());
        product.setProductName(dto.getProductName());
        product.setInterestRatePercent(dto.getInterestRatePercent());
        product.setMinimumOpeningBalanceAmount(dto.getMinimumOpeningBalanceAmount());
        product.setMinimumMaintainingBalanceAmount(dto.getMinimumMaintainingBalanceAmount());
        product.setInterestApplicationFrequencyCode(dto.getInterestApplicationFrequencyCode());
        product.setProductStatus(dto.getProductStatus());
        product.setExpiryDate(dto.getExpiryDate());
        product.setMinAge(dto.getMinAge());
        product.setMaxAge(dto.getMaxAge());

        return repository.save(product);
    }

    @Override
    public void delete(Long id) {
        SavingsProduct product = getById(id);
        repository.delete(product);
    }
}