package in.bank.service;

import in.bank.dto.SavingsProductRequestDTO;
import in.bank.entity.InterestApplicationFrequency;
import in.bank.entity.ProductStatus;
import in.bank.entity.SavingsProduct;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.SavingsProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavingsProductServiceTest {
 
    @Mock
    private SavingsProductRepository repository;

    @InjectMocks
    private SavingsProductServiceImpl service;

    private SavingsProductRequestDTO dto;
    private SavingsProduct product;

    @BeforeEach
    void setUp() {

        dto = new SavingsProductRequestDTO();
        dto.setProductName("Savings Gold");
        dto.setInterestRatePercent(BigDecimal.valueOf(4.5));
        dto.setMinimumOpeningBalanceAmount(BigDecimal.valueOf(1000));
        dto.setMinimumMaintainingBalanceAmount(BigDecimal.valueOf(500));
        dto.setInterestApplicationFrequencyCode(InterestApplicationFrequency.MONTHLY);
        dto.setProductStatus(ProductStatus.ACTIVE);
        dto.setExpiryDate(LocalDate.now().plusYears(1));

        product = SavingsProduct.builder()
                .id(1L)
                .productName("Savings Gold")
                .interestRatePercent(BigDecimal.valueOf(4.5))
                .minimumOpeningBalanceAmount(BigDecimal.valueOf(1000))
                .minimumMaintainingBalanceAmount(BigDecimal.valueOf(500))
                .interestApplicationFrequencyCode(InterestApplicationFrequency.MONTHLY)
                .productStatus(ProductStatus.ACTIVE)
                .expiryDate(dto.getExpiryDate())
                .build();
    }

    // ✅ 1. Happy Path - Create Product
    @Test
    void createSavingsProduct_success() {

        when(repository.existsByProductName(dto.getProductName())).thenReturn(false);
        when(repository.save(any(SavingsProduct.class))).thenReturn(product);

        Long id = service.create(dto);   // ✅ Now correct

        assertNotNull(id);
        assertEquals(1L, id);
        verify(repository, times(1)).save(any(SavingsProduct.class));
    }
    
    // ✅ 2. Duplicate Name - Should Throw Exception
    @Test
    void createSavingsProduct_duplicateName_shouldThrowException() {

        when(repository.existsByProductName(dto.getProductName())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.create(dto));

        verify(repository, never()).save(any());
    }

    // ✅ 3. Get By Id - Success
    @Test
    void getById_success() {

        when(repository.findById(1L)).thenReturn(Optional.of(product));

        SavingsProduct result = service.getById(1L);

        assertEquals("Savings Gold", result.getProductName());
    }

    // ✅ 4. Get By Id - Not Found
    @Test
    void getById_notFound_shouldThrowException() {

        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getById(1L));
    }

    // ✅ 5. Delete Product - Success
    @Test
    void deleteProduct_success() {

        when(repository.findById(1L)).thenReturn(Optional.of(product));

        service.delete(1L);

        verify(repository, times(1)).delete(product);
    }
 // ✅ 6. Update Product - Success
    @Test
    void updateProduct_success() {

        when(repository.findById(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(SavingsProduct.class))).thenReturn(product);

        dto.setInterestRatePercent(BigDecimal.valueOf(5.0));

        SavingsProduct updated = service.update(1L, dto);

        assertEquals(BigDecimal.valueOf(5.0), updated.getInterestRatePercent());
        verify(repository, times(1)).save(product);
    }
}
