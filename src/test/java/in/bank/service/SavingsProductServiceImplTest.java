package in.bank.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import in.bank.dto.SavingsProductRequestDTO;
import in.bank.entity.InterestApplicationFrequency;
import in.bank.entity.ProductStatus;
import in.bank.entity.SavingsProduct;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.SavingsProductRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavingsProductServiceImpl Tests")
class SavingsProductServiceImplTest {

    @Mock
    private SavingsProductRepository repository;

    @InjectMocks
    private SavingsProductServiceImpl service;

    private SavingsProductRequestDTO validDto;
    private SavingsProduct savedProduct;
    private SavingsProduct differentProduct;

    @BeforeEach
    void setUp() {
        validDto = new SavingsProductRequestDTO();
        validDto.setProductCode("SAV001");
        validDto.setProductName("Premium Savings");
        validDto.setInterestRatePercent(new BigDecimal("5.50"));
        validDto.setMinimumOpeningBalanceAmount(new BigDecimal("1000.00"));
        validDto.setMinimumMaintainingBalanceAmount(new BigDecimal("500.00"));
        validDto.setInterestApplicationFrequencyCode(InterestApplicationFrequency.MONTHLY);
        validDto.setProductStatus(ProductStatus.ACTIVE);
        validDto.setExpiryDate(LocalDate.now().plusYears(1));
        validDto.setMinAge(18);
        validDto.setMaxAge(60);

        savedProduct = SavingsProduct.builder()
                .id(1L)
                .productCode("SAV001")
                .productName("Premium Savings")
                .interestRatePercent(new BigDecimal("5.50"))
                .minimumOpeningBalanceAmount(new BigDecimal("1000.00"))
                .minimumMaintainingBalanceAmount(new BigDecimal("500.00"))
                .interestApplicationFrequencyCode(InterestApplicationFrequency.MONTHLY)
                .productStatus(ProductStatus.ACTIVE)
                .effectiveFromDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .minAge(18)
                .maxAge(60)
                .build();

        differentProduct = SavingsProduct.builder()
                .id(2L)
                .productCode("SAV002")
                .productName("Regular Savings")
                .interestRatePercent(new BigDecimal("4.00"))
                .minimumOpeningBalanceAmount(new BigDecimal("500.00"))
                .minimumMaintainingBalanceAmount(new BigDecimal("250.00"))
                .interestApplicationFrequencyCode(InterestApplicationFrequency.MONTHLY)
                .productStatus(ProductStatus.ACTIVE)
                .effectiveFromDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .minAge(18)
                .maxAge(60)
                .build();
    }

    @Nested
    @DisplayName("create() Tests")
    class CreateTests {

        @Test
        @DisplayName("TC1: Create product successfully")
        void create_ShouldReturnProductId_WhenValidDtoProvided() {
            when(repository.existsByProductName(anyString())).thenReturn(false);
            when(repository.save(any(SavingsProduct.class))).thenReturn(savedProduct);

            Long result = service.create(validDto);

            assertNotNull(result);
            assertEquals(1L, result);
            verify(repository, times(1)).existsByProductName("Premium Savings");
            verify(repository, times(1)).save(any(SavingsProduct.class));
        }

        @Test
        @DisplayName("TC2: Create product throws exception for duplicate name")
        void create_ShouldThrowException_WhenProductNameAlreadyExists() {
            when(repository.existsByProductName("Premium Savings")).thenReturn(true);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.create(validDto));

            assertEquals("Product name already exists", exception.getMessage());
            verify(repository, never()).save(any(SavingsProduct.class));
        }

        @Test
        @DisplayName("TC3: Create product throws exception for invalid age range")
        void create_ShouldThrowException_WhenMinAgeGreaterThanOrEqualToMaxAge() {
            validDto.setMinAge(60);
            validDto.setMaxAge(18);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.create(validDto));

            assertEquals("Minimum age must be less than maximum age", exception.getMessage());
            verify(repository, never()).save(any(SavingsProduct.class));
        }

        @Test
        @DisplayName("TC4: Create product throws exception for invalid balance")
        void create_ShouldThrowException_WhenMaintainingBalanceGreaterThanOpeningBalance() {
            validDto.setMinimumOpeningBalanceAmount(new BigDecimal("500.00"));
            validDto.setMinimumMaintainingBalanceAmount(new BigDecimal("1000.00"));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.create(validDto));

            assertEquals("Maintaining balance cannot be greater than opening balance", 
                    exception.getMessage());
            verify(repository, never()).save(any(SavingsProduct.class));
        }
    }

    @Nested
    @DisplayName("getById() Tests")
    class GetByIdTests {

        @Test
        @DisplayName("TC5: Get product by ID successfully")
        void getById_ShouldReturnProduct_WhenIdExists() {
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));

            SavingsProduct result = service.getById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Premium Savings", result.getProductName());
            verify(repository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("TC6: Get product by ID throws exception when not found")
        void getById_ShouldThrowResourceNotFoundException_WhenIdDoesNotExist() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.getById(99L));

            assertEquals("Product not found with id: 99", exception.getMessage());
            verify(repository, times(1)).findById(99L);
        }
    }

    @Nested
    @DisplayName("getAll() Tests")
    class GetAllTests {

        @Test
        @DisplayName("TC7: Get all products returns paginated results")
        void getAll_ShouldReturnPageOfProducts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SavingsProduct> expectedPage = new PageImpl<>(java.util.List.of(savedProduct));
            
            when(repository.findAll(pageable)).thenReturn(expectedPage);

            Page<SavingsProduct> result = service.getAll(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Premium Savings", result.getContent().get(0).getProductName());
            verify(repository, times(1)).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("update() Tests")
    class UpdateTests {

        @Test
        @DisplayName("TC8: Update product successfully - no code/name change")
        void update_Success_NoCodeNameChange() {
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));
            when(repository.save(any(SavingsProduct.class))).thenReturn(savedProduct);

            SavingsProduct result = service.update(1L, validDto);

            assertNotNull(result);
            assertEquals("Premium Savings", result.getProductName());
            verify(repository).save(any(SavingsProduct.class));
        }

        @Test
        @DisplayName("TC9: Update product - change product code to unique value")
        void update_ChangeProductCode_Unique() {
            validDto.setProductCode("NEW001");
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));
            when(repository.existsByProductCodeAndIdNot("NEW001", 1L)).thenReturn(false);
            when(repository.save(any(SavingsProduct.class))).thenReturn(savedProduct);

            SavingsProduct result = service.update(1L, validDto);

            assertNotNull(result);
            verify(repository).existsByProductCodeAndIdNot("NEW001", 1L);
            verify(repository).save(any(SavingsProduct.class));
        }

        @Test
        @DisplayName("TC10: Update product - duplicate product code throws exception")
        void update_DuplicateProductCode_ThrowsException() {
            validDto.setProductCode("DUPLICATE");
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));
            when(repository.existsByProductCodeAndIdNot("DUPLICATE", 1L)).thenReturn(true);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.update(1L, validDto));

            assertEquals("Product code 'DUPLICATE' is already in use by another product.", 
                    exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC11: Update product - change product name to unique value")
        void update_ChangeProductName_Unique() {
            validDto.setProductName("New Name");
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));
            when(repository.existsByProductName("New Name")).thenReturn(false);
            when(repository.save(any(SavingsProduct.class))).thenReturn(savedProduct);

            SavingsProduct result = service.update(1L, validDto);

            assertNotNull(result);
            verify(repository).existsByProductName("New Name");
            verify(repository).save(any(SavingsProduct.class));
        }

        @Test
        @DisplayName("TC12: Update product - duplicate product name throws exception")
        void update_DuplicateProductName_ThrowsException() {
            validDto.setProductName("Existing Name");
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));
            when(repository.existsByProductName("Existing Name")).thenReturn(true);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.update(1L, validDto));

            assertEquals("Product name already exists", exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC13: Update product - same product code (no change)")
        void update_SameProductCode_NoCheck() {
            validDto.setProductCode("SAV001");
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));
            when(repository.save(any(SavingsProduct.class))).thenReturn(savedProduct);

            SavingsProduct result = service.update(1L, validDto);

            assertNotNull(result);
            verify(repository, never()).existsByProductCodeAndIdNot(any(), any());
        }

        @Test
        @DisplayName("TC14: Update product - same product name (no change)")
        void update_SameProductName_NoCheck() {
            validDto.setProductName("Premium Savings");
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));
            when(repository.save(any(SavingsProduct.class))).thenReturn(savedProduct);

            SavingsProduct result = service.update(1L, validDto);

            assertNotNull(result);
            verify(repository, never()).existsByProductName(any());
        }

        @Test
        @DisplayName("TC15: Update product - null product code (skip check)")
        void update_NullProductCode_SkipCheck() {
            validDto.setProductCode(null);
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));
            when(repository.save(any(SavingsProduct.class))).thenReturn(savedProduct);

            SavingsProduct result = service.update(1L, validDto);

            assertNotNull(result);
            verify(repository, never()).existsByProductCodeAndIdNot(any(), any());
        }

        @Test
        @DisplayName("TC16: Update product - null product name (skip check)")
        void update_NullProductName_SkipCheck() {
            validDto.setProductName(null);
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));
            when(repository.save(any(SavingsProduct.class))).thenReturn(savedProduct);

            SavingsProduct result = service.update(1L, validDto);

            assertNotNull(result);
            verify(repository, never()).existsByProductName(any());
        }

        @Test
        @DisplayName("TC17: Update product - invalid age range throws exception")
        void update_InvalidAgeRange_ThrowsException() {
            validDto.setMinAge(60);
            validDto.setMaxAge(30);
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.update(1L, validDto));

            assertEquals("Minimum age must be less than maximum age", exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC18: Update product - invalid balance throws exception")
        void update_InvalidBalance_ThrowsException() {
            validDto.setMinimumOpeningBalanceAmount(new BigDecimal("500.00"));
            validDto.setMinimumMaintainingBalanceAmount(new BigDecimal("1000.00"));
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.update(1L, validDto));

            assertEquals("Maintaining balance cannot be greater than opening balance", 
                    exception.getMessage());
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("TC19: Update product - product not found throws exception")
        void update_ProductNotFound_ThrowsException() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.update(99L, validDto));

            assertEquals("Product not found with id: 99", exception.getMessage());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete() Tests")
    class DeleteTests {

        @Test
        @DisplayName("TC20: Delete product successfully")
        void delete_ShouldRemoveProduct_WhenIdExists() {
            when(repository.findById(1L)).thenReturn(Optional.of(savedProduct));
            doNothing().when(repository).delete(savedProduct);

            service.delete(1L);

            verify(repository, times(1)).findById(1L);
            verify(repository, times(1)).delete(savedProduct);
        }

        @Test
        @DisplayName("TC21: Delete product - not found throws exception")
        void delete_ProductNotFound_ThrowsException() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.delete(99L));

            assertEquals("Product not found with id: 99", exception.getMessage());
            verify(repository, never()).delete(any());
        }
    }
}