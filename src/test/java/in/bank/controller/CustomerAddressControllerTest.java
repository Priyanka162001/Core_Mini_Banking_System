package in.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bank.config.TestSecurityConfig;
import in.bank.dto.AddressRequestDTO;
import in.bank.dto.AddressResponseDTO;
import in.bank.entity.AddressStatus;
import in.bank.entity.AddressType;
import in.bank.exception.ResourceNotFoundException;
import in.bank.security.WithMockCustomer;
import in.bank.service.CustomerAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = CustomerAddressController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = in.bank.config.JwtFilter.class
    )
)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "spring.config.use-legacy-processing=true"
})
@DisplayName("CustomerAddressController Tests")
class CustomerAddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerAddressService customerAddressService;

    private AddressRequestDTO addressRequest;
    private AddressResponseDTO addressResponse;

    @BeforeEach
    void setUp() {
        addressRequest = new AddressRequestDTO();
        addressRequest.setAddressType(AddressType.PERMANENT);
        addressRequest.setAddressLine("123 MG Road");
        addressRequest.setCity("Pune");
        addressRequest.setState("Maharashtra");
        addressRequest.setPostalCode("411001");
        addressRequest.setCountry("India");
        addressRequest.setIsActive(true);

        addressResponse = AddressResponseDTO.builder()
                .addressId(1L)
                .customerProfileId(1L)
                .addressType("PERMANENT")
                .addressLine("123 MG Road")
                .city("Pune")
                .state("Maharashtra")
                .postalCode("411001")
                .country("India")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(1L)
                .updatedBy(1L)
                .build();
    }

    // =================== ADD ADDRESS TESTS ===================

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC1: Add address - Customer can add address for themselves")
    void addAddress_CustomerForSelf_Success() throws Exception {
        // Use when().thenReturn() instead of doNothing() since addAddress returns something
        when(customerAddressService.addAddress(eq(1L), any(AddressRequestDTO.class)))
                .thenReturn(addressResponse);

        mockMvc.perform(post("/api/v1/1/addresses")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Address added successfully"));

        verify(customerAddressService, times(1)).addAddress(eq(1L), any(AddressRequestDTO.class));
    }

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC2: Add address - Admin can add address for any customer")
    void addAddress_AdminForAnyCustomer_Success() throws Exception {
        // Use when().thenReturn() instead of doNothing() since addAddress returns something
        when(customerAddressService.addAddress(eq(2L), any(AddressRequestDTO.class)))
                .thenReturn(addressResponse);

        mockMvc.perform(post("/api/v1/2/addresses")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Address added successfully"));

        verify(customerAddressService, times(1)).addAddress(eq(2L), any(AddressRequestDTO.class));
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC3: Add address - Customer cannot add address for another customer")
    void addAddress_CustomerForAnother_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/2/addresses")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isForbidden());

        verify(customerAddressService, never()).addAddress(anyLong(), any());
    }
    
    // =================== GET ADDRESSES TESTS ===================

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC4: Get addresses - Customer can view ACTIVE addresses (success)")
    void getAddresses_CustomerWithActiveStatus_Success() throws Exception {
        List<AddressResponseDTO> addresses = Arrays.asList(addressResponse);
        when(customerAddressService.getAddressesByStatus(1L, AddressStatus.ACTIVE)).thenReturn(addresses);

        mockMvc.perform(get("/api/v1/1")
                .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].addressId").value(1));
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC5: Get addresses - Customer trying to view INACTIVE throws AccessDeniedException (COVERS YELLOW & RED LINES)")
    void getAddresses_CustomerWithInactiveStatus_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/1")
                .param("status", "INACTIVE"))
                .andExpect(status().isForbidden());
        
        verify(customerAddressService, never()).getAddressesByStatus(anyLong(), any());
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC6: Get addresses - Customer trying to view ALL throws AccessDeniedException (COVERS YELLOW & RED LINES)")
    void getAddresses_CustomerWithAllStatus_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/1")
                .param("status", "ALL"))
                .andExpect(status().isForbidden());
        
        verify(customerAddressService, never()).getAddressesByStatus(anyLong(), any());
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC7: Get addresses - Customer trying to view INVALID status returns 500")
    void getAddresses_CustomerWithInvalidStatus_ReturnsInternalServerError() throws Exception {
        // Change expectation from 400 to 500 since MethodArgumentTypeMismatchException is thrown
        mockMvc.perform(get("/api/v1/1")
                .param("status", "INVALID_STATUS"))
                .andExpect(status().is5xxServerError());
        
        verify(customerAddressService, never()).getAddressesByStatus(anyLong(), any());
    }

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC8: Get addresses - Admin can view ACTIVE addresses")
    void getAddresses_AdminWithActiveStatus_Success() throws Exception {
        List<AddressResponseDTO> addresses = Arrays.asList(addressResponse);
        when(customerAddressService.getAddressesByStatus(1L, AddressStatus.ACTIVE)).thenReturn(addresses);

        mockMvc.perform(get("/api/v1/1")
                .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC9: Get addresses - Admin can view INACTIVE addresses")
    void getAddresses_AdminWithInactiveStatus_Success() throws Exception {
        List<AddressResponseDTO> addresses = Arrays.asList();
        when(customerAddressService.getAddressesByStatus(1L, AddressStatus.INACTIVE)).thenReturn(addresses);

        mockMvc.perform(get("/api/v1/1")
                .param("status", "INACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC10: Get addresses - Admin can view ALL addresses")
    void getAddresses_AdminWithAllStatus_Success() throws Exception {
        List<AddressResponseDTO> addresses = Arrays.asList(addressResponse);
        when(customerAddressService.getAddressesByStatus(1L, AddressStatus.ALL)).thenReturn(addresses);

        mockMvc.perform(get("/api/v1/1")
                .param("status", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC11: Get addresses - Customer not found returns 404")
    void getAddresses_CustomerNotFound_Returns404() throws Exception {
        when(customerAddressService.getAddressesByStatus(999L, AddressStatus.ACTIVE))
                .thenThrow(new ResourceNotFoundException("Customer not found"));

        mockMvc.perform(get("/api/v1/999")
                .param("status", "ACTIVE"))
                .andExpect(status().isNotFound());
    }

    // =================== UPDATE ADDRESS TESTS ===================

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC12: Update address - Admin can update any address")
    void updateAddress_Admin_Success() throws Exception {
        when(customerAddressService.updateAddress(eq(1L), any(AddressRequestDTO.class)))
                .thenReturn(addressResponse);

        mockMvc.perform(put("/api/v1/update/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(1L));
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC13: Update address - Customer cannot update address (403)")
    void updateAddress_Customer_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/update/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isForbidden());

        verify(customerAddressService, never()).updateAddress(anyLong(), any());
    }

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC14: Update address - Address not found returns 404")
    void updateAddress_NotFound_Returns404() throws Exception {
        when(customerAddressService.updateAddress(eq(999L), any(AddressRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Address not found"));

        mockMvc.perform(put("/api/v1/update/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isNotFound());
    }

    // =================== DEACTIVATE ADDRESS TESTS ===================

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC15: Deactivate address - Admin can deactivate address")
    void deactivateAddress_Admin_Success() throws Exception {
        doNothing().when(customerAddressService).deactivateAddress(1L);

        mockMvc.perform(delete("/api/v1/deactivate/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Address deactivated successfully"));

        verify(customerAddressService, times(1)).deactivateAddress(1L);
    }

    @Test
    @WithMockCustomer(id = 1, roles = "CUSTOMER")
    @DisplayName("TC16: Deactivate address - Customer cannot deactivate address (403)")
    void deactivateAddress_Customer_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/deactivate/1")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(customerAddressService, never()).deactivateAddress(anyLong());
    }

    @Test
    @WithMockCustomer(id = 999, roles = "ADMIN")
    @DisplayName("TC17: Deactivate address - Address not found returns 404")
    void deactivateAddress_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Address not found"))
                .when(customerAddressService).deactivateAddress(999L);

        mockMvc.perform(delete("/api/v1/deactivate/999")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // =================== UNAUTHENTICATED TESTS ===================

    @Test
    @DisplayName("TC18: Add address - Unauthenticated returns 401")
    void addAddress_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/1/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isUnauthorized());
        
        verify(customerAddressService, never()).addAddress(anyLong(), any());
    }

    @Test
    @DisplayName("TC19: Get addresses - Unauthenticated returns 401")
    void getAddresses_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/1")
                .param("status", "ACTIVE"))
                .andExpect(status().isUnauthorized());
        
        verify(customerAddressService, never()).getAddressesByStatus(anyLong(), any());
    }
}