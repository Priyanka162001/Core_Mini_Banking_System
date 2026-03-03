package in.bank.controllertest;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import in.bank.entity.SavingsProduct;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bank.dto.SavingsProductRequestDTO;
import in.bank.entity.InterestApplicationFrequency;
import in.bank.entity.ProductStatus;
import in.bank.security.JwtService;
import in.bank.service.SavingsProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SavingsProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private SavingsProductService service; //  MOCK SERVICE

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setup() {

        // Admin token
        adminToken = jwtService.generateToken(
                User.withUsername("admin")
                        .password("Admin@123")
                        .roles("ADMIN")
                        .build()
        );

        // Normal USER token (no ADMIN role)
        userToken = jwtService.generateToken(
                User.withUsername("user")
                        .password("User@123")
                        .roles("USER")
                        .build()
        );
    }

    private SavingsProductRequestDTO createValidDTO() {
        SavingsProductRequestDTO dto = new SavingsProductRequestDTO();
        dto.setProductName("Test Savings");
        dto.setInterestRatePercent(BigDecimal.valueOf(4.5));
        dto.setMinimumOpeningBalanceAmount(BigDecimal.valueOf(1000));
        dto.setMinimumMaintainingBalanceAmount(BigDecimal.valueOf(500));
        dto.setInterestApplicationFrequencyCode(InterestApplicationFrequency.MONTHLY);
        dto.setProductStatus(ProductStatus.ACTIVE);
        dto.setEffectiveFromDate(LocalDate.now().plusDays(1));
        dto.setExpiryDate(LocalDate.now().plusYears(1));
        return dto;
    }

    // ✅ 1. Happy Path - Create product with ADMIN
    @Test
    void createSavingsProduct_success() throws Exception {

        SavingsProductRequestDTO dto = createValidDTO();

        when(service.create(any())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/savings-products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savingsProductId").value(1));
    }

    // ✅ 2. Unauthorized - No token
    @Test
    void createSavingsProduct_withoutToken_shouldReturn403() throws Exception {

        SavingsProductRequestDTO dto = createValidDTO();

        mockMvc.perform(post("/api/v1/savings-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ✅ 3. Forbidden - USER role instead of ADMIN
    @Test
    @WithMockUser(username="user", roles={"USER"})
    void createSavingsProduct_withUserRole_shouldReturn403() throws Exception {
        SavingsProductRequestDTO dto = createValidDTO();

        mockMvc.perform(post("/api/v1/savings-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden()); // now it will be 403
    }


    // ✅ 4. Validation Error - Missing product name
    @Test
    @WithMockUser(username="admin", roles={"ADMIN"})
    void createSavingsProduct_invalidRequest_shouldReturn400() throws Exception {
        SavingsProductRequestDTO dto = createValidDTO();
        dto.setProductName(""); // invalid

        mockMvc.perform(post("/api/v1/savings-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
    // ✅ 5. Not Found - Get non existing product
    @Test
    void getSavingsProduct_notFound_shouldReturn404() throws Exception {

        when(service.getById(9999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/savings-products/9999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}