package in.bank.service;

import in.bank.dto.CompleteProfileRequestDTO;
import in.bank.dto.CustomerProfileResponseDTO;
import in.bank.dto.UpdateProfileRequestDTO;
import in.bank.entity.*;
import in.bank.exception.AccessDeniedException;
import in.bank.exception.DuplicateResourceException;
import in.bank.exception.ResourceNotFoundException;
import in.bank.repository.CustomerAddressRepository;
import in.bank.repository.CustomerProfileRepository;
import in.bank.repository.UserRepository;
import in.bank.config.CorsConfig;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerProfileService Tests")
class CustomerProfileServiceTest {

    @Mock private CustomerProfileRepository customerProfileRepository;
    @Mock private CustomerAddressRepository customerAddressRepository;
    @Mock private UserRepository userRepository;
    @Mock private CorsConfig corsConfig;

    @InjectMocks
    private CustomerProfileServiceImpl customerProfileService;

    private AppUser customer;
    private AppUser adminUser;
    private CustomerProfile profile;
    private CustomerAddress activeAddress;
    private CustomerAddress inactiveAddress;

    @BeforeEach
    void setUp() {
        customer = AppUser.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("+919876543210")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        adminUser = AppUser.builder()
                .id(2L)
                .firstName("Super")
                .lastName("Admin")
                .email("admin@bank.com")
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        activeAddress = CustomerAddress.builder()
                .id(1L)
                .addressType(AddressType.PERMANENT)
                .addressLine("123 MG Road")
                .city("Pune")
                .state("Maharashtra")
                .postalCode("411001")
                .country("India")
                .isActive(true)
                .build();

        inactiveAddress = CustomerAddress.builder()
                .id(2L)
                .addressType(AddressType.PERMANENT)
                .addressLine("Old Street")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .isActive(false)
                .build();

        profile = CustomerProfile.builder()
                .id(1L)
                .customer(customer)
                .username("john_doe")
                .dateOfBirth(LocalDate.of(1990, 5, 20))
                .gender(GenderType.MALE)
                .addresses(new ArrayList<>(List.of(activeAddress, inactiveAddress)))
                .build();
        
        activeAddress.setCustomerProfile(profile);
        inactiveAddress.setCustomerProfile(profile);
    }

    private void mockSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        SecurityContextHolder.setContext(securityContext);
    }

    private CompleteProfileRequestDTO buildCompleteRequest(boolean withAddress) {
        CompleteProfileRequestDTO request = new CompleteProfileRequestDTO();
        request.setUsername("john_doe");
        request.setDateOfBirth(LocalDate.of(1990, 5, 20));
        request.setGender(GenderType.MALE);

        if (withAddress) {
            CompleteProfileRequestDTO.AddressRequestDTO address =
                    new CompleteProfileRequestDTO.AddressRequestDTO();
            address.setAddressType(AddressType.PERMANENT);
            address.setAddressLine("123 MG Road");
            address.setCity("Pune");
            address.setState("Maharashtra");
            address.setPostalCode("411001");
            address.setCountry("India");
            address.setIsActive(true);
            request.setAddresses(List.of(address));
        } else {
            request.setAddresses(null);
        }
        return request;
    }

    @Nested
    @DisplayName("completeProfile() Tests")
    class CompleteProfileTests {

        @Test
        @DisplayName("TC1: Complete profile with address - success")
        void completeProfile_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.empty());
            when(customerProfileRepository.save(any())).thenReturn(profile);

            assertDoesNotThrow(() ->
                    customerProfileService.completeProfile(1L, buildCompleteRequest(true)));

            verify(customerProfileRepository).save(any(CustomerProfile.class));
            verify(customerAddressRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("TC2: Complete profile without address - success")
        void completeProfile_NoAddress_SavesProfileWithoutAddress() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.empty());
            when(customerProfileRepository.save(any())).thenReturn(profile);

            assertDoesNotThrow(() ->
                    customerProfileService.completeProfile(1L, buildCompleteRequest(false)));

            verify(customerProfileRepository).save(any(CustomerProfile.class));
            verify(customerAddressRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("TC3: Complete profile with address where isActive is null - defaults to true - covers yellow line")
        void completeProfile_AddressWithNullIsActive_DefaultsToTrue() {
            CompleteProfileRequestDTO request = buildCompleteRequest(true);
            request.getAddresses().get(0).setIsActive(null);
            
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.empty());
            when(customerProfileRepository.save(any())).thenReturn(profile);
            
            assertDoesNotThrow(() ->
                    customerProfileService.completeProfile(1L, request));
            
            verify(customerAddressRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("TC4: Complete profile - duplicate profile throws exception")
        void completeProfile_AlreadyExists_ThrowsDuplicateException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));

            assertThrows(DuplicateResourceException.class,
                    () -> customerProfileService.completeProfile(1L,
                            buildCompleteRequest(true)));

            verify(customerProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC5: Complete profile - user not found")
        void completeProfile_UserNotFound_ThrowsResourceNotFoundException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> customerProfileService.completeProfile(99L,
                            buildCompleteRequest(true)));
        }
    }

    @Nested
    @DisplayName("getCustomerProfile() Tests")
    class GetCustomerProfileTests {

        @Test
        @DisplayName("TC6: Get customer profile - customer views own profile")
        void getCustomerProfile_CustomerOwnProfile_Success() {
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));

            CustomerProfileResponseDTO result = 
                    customerProfileService.getCustomerProfile(1L, customer);

            assertNotNull(result);
            assertEquals("john_doe", result.getUsername());
        }

        @Test
        @DisplayName("TC7: Get customer profile - admin views any profile")
        void getCustomerProfile_AdminViewsAnyProfile_Success() {
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));

            CustomerProfileResponseDTO result = 
                    customerProfileService.getCustomerProfile(1L, adminUser);

            assertNotNull(result);
            assertEquals("john_doe", result.getUsername());
        }

        @Test
        @DisplayName("TC8: Get customer profile - customer views other profile throws exception")
        void getCustomerProfile_CustomerViewsOther_ThrowsAccessDeniedException() {
            when(customerProfileRepository.findByCustomer_Id(2L))
                    .thenReturn(Optional.of(profile));

            assertThrows(AccessDeniedException.class,
                    () -> customerProfileService.getCustomerProfile(2L, customer));
        }

        @Test
        @DisplayName("TC9: Get customer profile - profile not found throws exception")
        void getCustomerProfile_ProfileNotFound_ThrowsResourceNotFoundException() {
            when(customerProfileRepository.findByCustomer_Id(99L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> customerProfileService.getCustomerProfile(99L, adminUser));
        }
    }

    @Nested
    @DisplayName("getProfileDTO() Tests")
    class GetProfileDTOTests {

        @Test
        @DisplayName("TC10: Get profile DTO - customer views own profile")
        void getProfileDTO_CustomerViewsOwnProfile_Success() {
            mockSecurityContext("john@example.com");

            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(customer));
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));

            CustomerProfileResponseDTO result =
                    customerProfileService.getProfileDTO(1L);

            assertNotNull(result);
            assertEquals("john_doe", result.getUsername());
        }

        @Test
        @DisplayName("TC11: Get profile DTO - customer views other profile throws exception")
        void getProfileDTO_CustomerViewsOtherProfile_ThrowsAccessDeniedException() {
            mockSecurityContext("john@example.com");

            when(userRepository.findByEmail("john@example.com"))
                    .thenReturn(Optional.of(customer));

            assertThrows(AccessDeniedException.class,
                    () -> customerProfileService.getProfileDTO(2L));
        }

        @Test
        @DisplayName("TC12: Get profile DTO - admin views any profile")
        void getProfileDTO_AdminViewsAnyProfile_Success() {
            mockSecurityContext("admin@bank.com");

            when(userRepository.findByEmail("admin@bank.com"))
                    .thenReturn(Optional.of(adminUser));
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));

            CustomerProfileResponseDTO result =
                    customerProfileService.getProfileDTO(1L);

            assertNotNull(result);
            assertEquals("john_doe", result.getUsername());
        }

        @Test
        @DisplayName("TC13: Get profile DTO - logged-in user not found")
        void getProfileDTO_LoggedInUserNotFound_ThrowsException() {
            mockSecurityContext("unknown@example.com");

            when(userRepository.findByEmail("unknown@example.com"))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> customerProfileService.getProfileDTO(1L));
        }

        @Test
        @DisplayName("TC14: Get profile DTO - profile not found")
        void getProfileDTO_ProfileNotFound_ThrowsResourceNotFoundException() {
            mockSecurityContext("admin@bank.com");

            when(userRepository.findByEmail("admin@bank.com"))
                    .thenReturn(Optional.of(adminUser));
            when(customerProfileRepository.findByCustomer_Id(99L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> customerProfileService.getProfileDTO(99L));
        }
    }

    @Nested
    @DisplayName("updateProfile() Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("TC15: Update profile with new address - deactivates old addresses")
        void updateProfile_WithNewAddress_DeactivatesOld() {
            UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
            request.setUsername("john_updated");
            request.setGender(GenderType.MALE);
            request.setAddressType(AddressType.PERMANENT);
            request.setAddressLine("456 New Road");
            request.setCity("Mumbai");
            request.setState("Maharashtra");
            request.setPostalCode("400001");
            request.setCountry("India");

            List<CustomerAddress> existingAddresses = List.of(activeAddress);
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));
            when(customerAddressRepository
                    .findByCustomerProfile_IdAndAddressTypeAndIsActiveTrue(
                            profile.getId(), request.getAddressType()))
                    .thenReturn(existingAddresses);
            when(customerAddressRepository.saveAll(anyList())).thenReturn(existingAddresses);
            when(customerAddressRepository.save(any(CustomerAddress.class))).thenReturn(activeAddress);
            when(customerProfileRepository.save(any())).thenReturn(profile);

            assertDoesNotThrow(() ->
                    customerProfileService.updateProfile(1L, request));

            assertFalse(activeAddress.getIsActive());
            verify(customerAddressRepository).saveAll(existingAddresses);
            verify(customerAddressRepository).save(any(CustomerAddress.class));
        }

        @Test
        @DisplayName("TC16: Update profile without address - only updates basic fields - covers username and gender yellow lines")
        void updateProfile_WithoutAddress_OnlyUpdatesBasicFields() {
            UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
            request.setUsername("john_updated");
            request.setGender(GenderType.MALE);

            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));
            when(customerProfileRepository.save(any())).thenReturn(profile);

            assertDoesNotThrow(() ->
                    customerProfileService.updateProfile(1L, request));

            verify(customerAddressRepository, never()).findByCustomerProfile_IdAndAddressTypeAndIsActiveTrue(any(), any());
            verify(customerAddressRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC17: Update profile with null username - skips update")
        void updateProfile_NullUsername_SkipsUpdate() {
            UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
            request.setUsername(null);
            request.setGender(GenderType.MALE);
            request.setAddressType(AddressType.PERMANENT);
            request.setAddressLine("456 New Road");
            request.setCity("Mumbai");
            request.setState("Maharashtra");
            request.setPostalCode("400001");
            request.setCountry("India");

            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));
            when(customerAddressRepository
                    .findByCustomerProfile_IdAndAddressTypeAndIsActiveTrue(
                            profile.getId(), request.getAddressType()))
                    .thenReturn(List.of());
            when(customerAddressRepository.save(any(CustomerAddress.class))).thenReturn(activeAddress);
            when(customerProfileRepository.save(any())).thenReturn(profile);

            assertDoesNotThrow(() ->
                    customerProfileService.updateProfile(1L, request));
            
            verify(customerProfileRepository).save(profile);
        }

        @Test
        @DisplayName("TC18: Update profile with null gender - skips update")
        void updateProfile_NullGender_SkipsUpdate() {
            UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
            request.setUsername("john_updated");
            request.setGender(null);
            request.setAddressType(AddressType.PERMANENT);
            request.setAddressLine("456 New Road");
            request.setCity("Mumbai");
            request.setState("Maharashtra");
            request.setPostalCode("400001");
            request.setCountry("India");

            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));
            when(customerAddressRepository
                    .findByCustomerProfile_IdAndAddressTypeAndIsActiveTrue(
                            profile.getId(), request.getAddressType()))
                    .thenReturn(List.of());
            when(customerAddressRepository.save(any(CustomerAddress.class))).thenReturn(activeAddress);
            when(customerProfileRepository.save(any())).thenReturn(profile);

            assertDoesNotThrow(() ->
                    customerProfileService.updateProfile(1L, request));
        }

        @Test
        @DisplayName("TC19: Update profile with address but no existing duplicates")
        void updateProfile_WithNewAddress_NoExistingDuplicates() {
            UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
            request.setUsername("john_updated");
            request.setGender(GenderType.MALE);
            request.setAddressType(AddressType.PERMANENT);
            request.setAddressLine("456 New Road");
            request.setCity("Mumbai");
            request.setState("Maharashtra");
            request.setPostalCode("400001");
            request.setCountry("India");

            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));
            when(customerAddressRepository
                    .findByCustomerProfile_IdAndAddressTypeAndIsActiveTrue(
                            profile.getId(), request.getAddressType()))
                    .thenReturn(List.of());
            when(customerAddressRepository.save(any(CustomerAddress.class))).thenReturn(activeAddress);
            when(customerProfileRepository.save(any())).thenReturn(profile);

            assertDoesNotThrow(() ->
                    customerProfileService.updateProfile(1L, request));

            verify(customerAddressRepository, never()).saveAll(anyList());
            verify(customerAddressRepository).save(any(CustomerAddress.class));
        }

        @Test
        @DisplayName("TC20: Update profile - profile not found")
        void updateProfile_ProfileNotFound_ThrowsResourceNotFoundException() {
            when(customerProfileRepository.findByCustomer_Id(99L))
                    .thenReturn(Optional.empty());

            UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
            request.setUsername("john_updated");

            assertThrows(ResourceNotFoundException.class,
                    () -> customerProfileService.updateProfile(99L, request));
        }
    }

    @Nested
    @DisplayName("getProfile() Tests")
    class GetProfileTests {

        @Test
        @DisplayName("TC21: Get raw profile by customer ID - success")
        void getProfile_Success() {
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));

            CustomerProfile result = customerProfileService.getProfile(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("TC22: Get raw profile - not found throws exception")
        void getProfile_NotFound_ThrowsResourceNotFoundException() {
            when(customerProfileRepository.findByCustomer_Id(99L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> customerProfileService.getProfile(99L));
        }
    }

    @Nested
    @DisplayName("getAllProfiles() Tests")
    class GetAllProfilesTests {

        @Test
        @DisplayName("TC23: Get all raw profiles - success")
        void getAllProfiles_Success() {
            List<CustomerProfile> profiles = List.of(profile);
            when(customerProfileRepository.findAll()).thenReturn(profiles);

            List<CustomerProfile> result = customerProfileService.getAllProfiles();

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("TC24: Get all raw profiles - empty list")
        void getAllProfiles_EmptyList() {
            when(customerProfileRepository.findAll()).thenReturn(List.of());

            List<CustomerProfile> result = customerProfileService.getAllProfiles();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getAllProfileDTOs() Tests")
    class GetAllProfileDTOTests {

        @Test
        @DisplayName("TC25: Get all profile DTOs paginated - success")
        void getAllProfileDTOs_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<CustomerProfile> page = new PageImpl<>(List.of(profile));
            when(customerProfileRepository.findAll(pageable)).thenReturn(page);

            Page<CustomerProfileResponseDTO> result = 
                    customerProfileService.getAllProfileDTOs(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("TC26: Get all profile DTOs - empty page")
        void getAllProfileDTOs_EmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<CustomerProfile> emptyPage = new PageImpl<>(List.of());
            when(customerProfileRepository.findAll(pageable)).thenReturn(emptyPage);

            Page<CustomerProfileResponseDTO> result = 
                    customerProfileService.getAllProfileDTOs(pageable);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("mapToDTO() Tests - Covers all yellow lines")
    class MapToDTOTests {

        @Test
        @DisplayName("TC27: Map to DTO with null gender, role, status - covers ternary operators")
        void mapToDTO_WithNullValues_HandlesGracefully() {
            customer.setRole(null);
            customer.setStatus(null);
            profile.setGender(null);
            
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));

            CustomerProfileResponseDTO result = 
                    customerProfileService.getCustomerProfile(1L, adminUser);

            assertNotNull(result);
            assertNull(result.getGender());
            assertNull(result.getRole());
            assertNull(result.getStatus());
        }

        @Test
        @DisplayName("TC28: Map to DTO with null addresses - uses empty list")
        void mapToDTO_NullAddresses_UsesEmptyList() {
            profile.setAddresses(null);
            
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));

            CustomerProfileResponseDTO result = 
                    customerProfileService.getCustomerProfile(1L, adminUser);

            assertNotNull(result);
            assertNotNull(result.getAddresses());
            assertTrue(result.getAddresses().isEmpty());
        }

        @Test
        @DisplayName("TC29: Map to DTO with address having null addressType - handles gracefully")
        void mapToDTO_AddressWithNullType_HandlesGracefully() {
            activeAddress.setAddressType(null);
            
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));

            CustomerProfileResponseDTO result = 
                    customerProfileService.getCustomerProfile(1L, adminUser);

            assertNotNull(result);
            assertNotNull(result.getAddresses());
            assertEquals(1, result.getAddresses().size());
            assertNull(result.getAddresses().get(0).getAddressType());
        }

        @Test
        @DisplayName("TC30: Map to DTO with all values present")
        void mapToDTO_AllValuesPresent_Success() {
            when(customerProfileRepository.findByCustomer_Id(1L))
                    .thenReturn(Optional.of(profile));

            CustomerProfileResponseDTO result = 
                    customerProfileService.getCustomerProfile(1L, adminUser);

            assertNotNull(result);
            assertEquals("john_doe", result.getUsername());
            assertEquals("MALE", result.getGender());
            assertEquals("ROLE_CUSTOMER", result.getRole());  // Full enum name
            assertEquals("ACTIVE", result.getStatus());
            assertEquals(1, result.getAddresses().size());
            assertEquals("PERMANENT", result.getAddresses().get(0).getAddressType());
        }
    }
}