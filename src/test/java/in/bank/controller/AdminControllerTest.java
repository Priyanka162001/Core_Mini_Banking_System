package in.bank.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import in.bank.config.TestSecurityConfig;
import in.bank.entity.AppUser;
import in.bank.entity.UserRole;
import in.bank.entity.UserStatus;
import in.bank.repository.UserRepository;

@WebMvcTest(
    controllers = AdminController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = in.bank.config.JwtFilter.class
    )
)
@Import(TestSecurityConfig.class)  // ✅ Import test security config
@AutoConfigureMockMvc(addFilters = true)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    private AppUser adminUser;
    private AppUser customerUser;

    @BeforeEach
    void setUp() {
        adminUser = AppUser.builder()
                .id(1L)
                .firstName("Super")
                .lastName("Admin")
                .email("admin@bank.com")
                .role(UserRole.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        customerUser = AppUser.builder()
                .id(2L)
                .firstName("John")
                .lastName("Doe")
                .email("customer@bank.com")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ================= ASCENDING ORDER TESTS =================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC1: Get all users - ascending order by id (default)")
    void getAllUsers_WithAscendingOrder_ShouldReturnUsers() throws Exception {
        Page<AppUser> userPage = new PageImpl<>(Arrays.asList(adminUser, customerUser), 
                PageRequest.of(0, 20, Sort.by("id").ascending()), 2);
        
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/v1/admin/users")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "id")
                .param("direction", "asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC2: Get all users - ascending order by email")
    void getAllUsers_WithAscendingOrderByEmail_ShouldReturnUsers() throws Exception {
        Page<AppUser> userPage = new PageImpl<>(Arrays.asList(adminUser, customerUser), 
                PageRequest.of(0, 20, Sort.by("email").ascending()), 2);
        
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/v1/admin/users")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "email")
                .param("direction", "asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ================= DESCENDING ORDER TESTS =================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC3: Get all users - descending order by id")
    void getAllUsers_WithDescendingOrder_ShouldReturnUsers() throws Exception {
        Page<AppUser> userPage = new PageImpl<>(Arrays.asList(customerUser, adminUser), 
                PageRequest.of(0, 20, Sort.by("id").descending()), 2);
        
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/v1/admin/users")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "id")
                .param("direction", "desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC4: Get all users - descending order by email")
    void getAllUsers_WithDescendingOrderByEmail_ShouldReturnUsers() throws Exception {
        Page<AppUser> userPage = new PageImpl<>(Arrays.asList(customerUser, adminUser), 
                PageRequest.of(0, 20, Sort.by("email").descending()), 2);
        
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/v1/admin/users")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "email")
                .param("direction", "DESC")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ================= PAGINATION TESTS =================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC5: Get all users - custom page and size")
    void getAllUsers_WithCustomPageAndSize_ShouldReturnUsers() throws Exception {
        Page<AppUser> userPage = new PageImpl<>(Arrays.asList(adminUser), 
                PageRequest.of(1, 10, Sort.by("id").ascending()), 2);
        
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/v1/admin/users")
                .param("page", "1")
                .param("size", "10")
                .param("sortBy", "id")
                .param("direction", "asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC6: Get all users - default parameters")
    void getAllUsers_WithDefaultParameters_ShouldReturnUsers() throws Exception {
        Page<AppUser> userPage = new PageImpl<>(Arrays.asList(adminUser), 
                PageRequest.of(0, 20, Sort.by("id").ascending()), 1);
        
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ================= EMPTY RESULTS TESTS =================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC7: Get all users - empty page when no users exist")
    void getAllUsers_ShouldReturnEmptyPage_WhenNoUsersExist() throws Exception {
        Page<AppUser> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 20), 0);
        
        when(userRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ================= ACCESS DENIED TESTS =================

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC8: Get all users - forbidden for CUSTOMER role")
    void getAllUsers_ShouldReturnForbidden_WhenCustomerAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("TC9: Get all users - unauthorized without authentication")
    void getAllUsers_ShouldReturnUnauthorized_WhenNoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        
        verify(userRepository, never()).findAll(any(Pageable.class));
    }
}