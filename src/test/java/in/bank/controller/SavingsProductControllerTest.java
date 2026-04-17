package in.bank.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.bank.dto.SavingsProductRequestDTO;
import in.bank.entity.InterestApplicationFrequency;
import in.bank.entity.ProductStatus;
import in.bank.entity.SavingsProduct;
import in.bank.exception.ResourceNotFoundException;
import in.bank.service.SavingsProductService;

@SpringBootTest
@AutoConfigureMockMvc
class SavingsProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SavingsProductService service;

    private SavingsProductRequestDTO validDto;
    private SavingsProduct savedProduct;

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
    }

    // ================= CREATE PRODUCT TESTS =================
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_ShouldReturnCreated_WhenAdminAndValidRequest() throws Exception {
        when(service.create(any(SavingsProductRequestDTO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/savings-products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savingsProductId").value(1L));

        verify(service, times(1)).create(any(SavingsProductRequestDTO.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createProduct_ShouldReturnForbidden_WhenCustomer() throws Exception {
        mockMvc.perform(post("/api/v1/savings-products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isForbidden());

        verify(service, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        validDto.setProductName("");

        mockMvc.perform(post("/api/v1/savings-products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isBadRequest());
    }

    // ================= GET PRODUCT BY ID TESTS =================

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProductById_ShouldReturnProduct_WhenIdExists() throws Exception {
        when(service.getById(1L)).thenReturn(savedProduct);

        mockMvc.perform(get("/api/v1/savings-products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.productName").value("Premium Savings"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getProductById_ShouldReturnProduct_WhenCustomerAndIdExists() throws Exception {
        when(service.getById(1L)).thenReturn(savedProduct);

        mockMvc.perform(get("/api/v1/savings-products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getProductById_ShouldReturnNotFound_WhenIdDoesNotExist() throws Exception {
        when(service.getById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/savings-products/{id}", 99L))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getProductById_ShouldReturnNotFound_WhenCustomerAndIdDoesNotExist() throws Exception {
        when(service.getById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/savings-products/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    // ================= GET ALL PRODUCTS TESTS =================

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAllProducts_ShouldReturnPaginatedResults_WithAscendingSort() throws Exception {
        Page<SavingsProduct> page = new PageImpl<>(List.of(savedProduct), PageRequest.of(0, 10), 1);
        when(service.getAll(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/savings-products")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Premium Savings"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAllProducts_ShouldReturnPaginatedResults_WithDescendingSort() throws Exception {
        Page<SavingsProduct> page = new PageImpl<>(List.of(savedProduct), PageRequest.of(0, 10), 1);
        when(service.getAll(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/savings-products")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAllProducts_ShouldReturnEmptyPage_WhenNoProducts() throws Exception {
        Page<SavingsProduct> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(service.getAll(any(PageRequest.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/savings-products")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "id")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ================= UPDATE PRODUCT TESTS =================

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_ShouldReturnOk_WhenAdminAndValidRequest() throws Exception {
        // Change doNothing() to when().thenReturn() since update method returns something
        when(service.update(eq(1L), any(SavingsProductRequestDTO.class)))
                .thenReturn(savedProduct); // or .thenReturn(null) if it returns void-like

        mockMvc.perform(put("/api/v1/savings-products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Savings product updated successfully"));

        verify(service, times(1)).update(eq(1L), any(SavingsProductRequestDTO.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateProduct_ShouldReturnForbidden_WhenCustomer() throws Exception {
        mockMvc.perform(put("/api/v1/savings-products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isForbidden());

        verify(service, never()).update(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        validDto.setProductName("");

        mockMvc.perform(put("/api/v1/savings-products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isBadRequest());

        verify(service, never()).update(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        // For void method that throws exception, use doThrow()
        doThrow(new ResourceNotFoundException("Product not found with id: 99"))
                .when(service).update(eq(99L), any(SavingsProductRequestDTO.class));

        mockMvc.perform(put("/api/v1/savings-products/{id}", 99L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isNotFound());
    }

    // ================= DELETE PRODUCT TESTS =================

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_ShouldReturnOk_WhenAdminAndIdExists() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/v1/savings-products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Savings product deleted successfully"));
        
        verify(service, times(1)).delete(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteProduct_ShouldReturnForbidden_WhenCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/savings-products/{id}", 1L))
                .andExpect(status().isForbidden());

        verify(service, never()).delete(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found with id: 99"))
                .when(service).delete(99L);

        mockMvc.perform(delete("/api/v1/savings-products/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    // ================= UNAUTHENTICATED TESTS =================

    @Test
    void createProduct_ShouldReturnForbidden_WhenNoAuth() throws Exception {
        mockMvc.perform(post("/api/v1/savings-products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProductById_ShouldReturnForbidden_WhenNoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/savings-products/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllProducts_ShouldReturnForbidden_WhenNoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/savings-products"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProduct_ShouldReturnForbidden_WhenNoAuth() throws Exception {
        mockMvc.perform(put("/api/v1/savings-products/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProduct_ShouldReturnForbidden_WhenNoAuth() throws Exception {
        mockMvc.perform(delete("/api/v1/savings-products/{id}", 1L))
                .andExpect(status().isForbidden());
    }
}