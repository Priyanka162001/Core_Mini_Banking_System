package in.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.bank.config.TestSecurityConfig;
import in.bank.dto.CompleteProfileRequestDTO;
import in.bank.dto.CustomerProfileResponseDTO;
import in.bank.dto.UpdateProfileRequestDTO;
import in.bank.entity.AddressType;
import in.bank.entity.GenderType;
import in.bank.exception.DuplicateResourceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.service.CustomerProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = CustomerProfileController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = in.bank.config.JwtFilter.class
    )
)
@Import(TestSecurityConfig.class)  // ✅ Import test security config
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("CustomerProfileController Tests")
class CustomerProfileControllerTest {

    @Autowired 
    private MockMvc mockMvc;
    
    @Autowired 
    private ObjectMapper objectMapper;

    @MockBean 
    private CustomerProfileService customerProfileService;

    private CompleteProfileRequestDTO completeRequest;
    private CustomerProfileResponseDTO profileResponse;

    @BeforeEach
    void setUp() {
        CompleteProfileRequestDTO.AddressRequestDTO address =
                new CompleteProfileRequestDTO.AddressRequestDTO();
        address.setAddressType(AddressType.PERMANENT);
        address.setAddressLine("123 MG Road");
        address.setCity("Pune");
        address.setState("Maharashtra");
        address.setPostalCode("411001");
        address.setCountry("India");
        address.setIsActive(true);

        completeRequest = new CompleteProfileRequestDTO();
        completeRequest.setUsername("john_doe");
        completeRequest.setDateOfBirth(LocalDate.of(1990, 5, 20));
        completeRequest.setGender(GenderType.MALE);
        completeRequest.setAddresses(List.of(address));

        CustomerProfileResponseDTO.ContactInfo contact =
                new CustomerProfileResponseDTO.ContactInfo();
        contact.setEmail("john@example.com");
        contact.setPhoneNumber("+919876543210");
        contact.setEmailVerified(true);

        profileResponse = new CustomerProfileResponseDTO();
        profileResponse.setCustomerProfileId(1L);
        profileResponse.setCustomerId(1L);
        profileResponse.setFirstName("John");
        profileResponse.setLastName("Doe");
        profileResponse.setUsername("john_doe");
        profileResponse.setGender("MALE");
        profileResponse.setContact(contact);
        profileResponse.setAddresses(List.of());
    }

    // =================== 1. COMPLETE PROFILE - SUCCESS ===================
    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC1: Complete profile - Success")
    void completeProfile_Success_Returns200() throws Exception {
        doNothing().when(customerProfileService)
                .completeProfile(anyLong(), any());

        mockMvc.perform(post("/api/v1/customers/1/profile")
                .with(csrf())  // ✅ Add CSRF
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile completed successfully"));

        verify(customerProfileService).completeProfile(eq(1L), any());
    }

    // =================== 2. COMPLETE PROFILE - DUPLICATE → 409 ===================
    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC2: Complete profile - Duplicate returns 409")
    void completeProfile_AlreadyExists_Returns409() throws Exception {
        doThrow(new DuplicateResourceException("Profile already exists"))
                .when(customerProfileService).completeProfile(anyLong(), any());

        mockMvc.perform(post("/api/v1/customers/1/profile")
                .with(csrf())  // ✅ Add CSRF
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isConflict());
    }

    // =================== 3. COMPLETE PROFILE - NO TOKEN → 401/403 ===================
    @Test
    @DisplayName("TC3: Complete profile - No token returns 401/403")
    void completeProfile_NoToken_Returns401Or403() throws Exception {
        mockMvc.perform(post("/api/v1/customers/1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403,
                            "Expected 401 or 403 but got: " + status);
                });
    }

    // =================== 4. COMPLETE PROFILE - ADMIN ROLE → 403 ===================
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC4: Complete profile - Admin role returns 403")
    void completeProfile_AdminRole_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/customers/1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isForbidden());
    }

    // =================== 5. GET PROFILE - CUSTOMER SUCCESS → 200 ===================
    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC5: Get profile - Customer success returns 200")
    void getProfile_CustomerSuccess_Returns200() throws Exception {
        when(customerProfileService.getProfileDTO(1L))
                .thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1L))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.gender").value("MALE"));
    }

    // =================== 6. GET PROFILE - ACCESS DENIED → 403 ===================
    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC6: Get profile - Access denied returns 403")
    void getProfile_AccessDenied_Returns403() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException(
                "You are not authorized to view this profile"))
                .when(customerProfileService).getProfileDTO(2L);

        mockMvc.perform(get("/api/v1/customer/2"))
                .andExpect(status().isForbidden());
    }

    // =================== 7. GET PROFILE - NOT FOUND → 404 ===================
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC7: Get profile - Not found returns 404")
    void getProfile_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Profile not found"))
                .when(customerProfileService).getProfileDTO(99L);

        mockMvc.perform(get("/api/v1/customer/99"))
                .andExpect(status().isNotFound());
    }

    // =================== 8. UPDATE PROFILE - SUCCESS → 200 ===================
    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC8: Update profile - Success returns 200")
    void updateProfile_Success_Returns200() throws Exception {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
        request.setUsername("john_updated");
        request.setGender(GenderType.MALE);

        doNothing().when(customerProfileService)
                .updateProfile(anyLong(), any());

        mockMvc.perform(patch("/api/v1/customers/1/profile")
                .with(csrf())  // ✅ Add CSRF
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Profile updated successfully"));
    }

    // =================== 9. UPDATE PROFILE - NO TOKEN → 401/403 ===================
    @Test
    @DisplayName("TC9: Update profile - No token returns 401/403")
    void updateProfile_NoToken_Returns401Or403() throws Exception {
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
        request.setUsername("john_updated");

        mockMvc.perform(patch("/api/v1/customers/1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403,
                            "Expected 401 or 403 but got: " + status);
                });
    }

    // =================== 10. GET ALL PROFILES - ADMIN SUCCESS (ASCENDING) ===================
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC10: Get all profiles - Admin success with ascending order (covers asc branch)")
    void getAllProfiles_AdminSuccess_Ascending_Returns200() throws Exception {
        PageImpl<CustomerProfileResponseDTO> page =
                new PageImpl<>(List.of(profileResponse));

        when(customerProfileService.getAllProfileDTOs(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "id")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("john_doe"))
                .andExpect(jsonPath("$.content[0].customerId").value(1L));
    }

    // =================== 11. GET ALL PROFILES - ADMIN SUCCESS (DESCENDING) ===================
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC11: Get all profiles - Admin success with descending order (covers desc branch)")
    void getAllProfiles_AdminSuccess_Descending_Returns200() throws Exception {
        PageImpl<CustomerProfileResponseDTO> page =
                new PageImpl<>(List.of(profileResponse));

        when(customerProfileService.getAllProfileDTOs(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "id")
                .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("john_doe"))
                .andExpect(jsonPath("$.content[0].customerId").value(1L));
    }

    // =================== 12. GET ALL PROFILES - ADMIN WITH CUSTOM SORT ===================
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC12: Get all profiles - Admin with custom sort by username ascending")
    void getAllProfiles_AdminWithCustomSortAsc_Returns200() throws Exception {
        PageImpl<CustomerProfileResponseDTO> page =
                new PageImpl<>(List.of(profileResponse));

        when(customerProfileService.getAllProfileDTOs(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "username")
                .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("john_doe"));
    }

    // =================== 13. GET ALL PROFILES - ADMIN WITH CUSTOM SORT DESCENDING ===================
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC13: Get all profiles - Admin with custom sort by username descending")
    void getAllProfiles_AdminWithCustomSortDesc_Returns200() throws Exception {
        PageImpl<CustomerProfileResponseDTO> page =
                new PageImpl<>(List.of(profileResponse));

        when(customerProfileService.getAllProfileDTOs(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "username")
                .param("direction", "DESC"))
                .andExpect(status().isOk());
    }

    // =================== 14. GET ALL PROFILES - ADMIN WITH DEFAULT PARAMETERS ===================
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC14: Get all profiles - Admin with default parameters")
    void getAllProfiles_AdminWithDefaultParams_Returns200() throws Exception {
        PageImpl<CustomerProfileResponseDTO> page =
                new PageImpl<>(List.of(profileResponse));

        when(customerProfileService.getAllProfileDTOs(any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1"))
                .andExpect(status().isOk());
    }

    // =================== 15. GET ALL PROFILES - FORBIDDEN FOR CUSTOMER ===================
    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC15: Get all profiles - Forbidden for CUSTOMER")
    void getAllProfiles_CustomerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1"))
                .andExpect(status().isForbidden());
        
        verify(customerProfileService, never()).getAllProfileDTOs(any(Pageable.class));
    }
}