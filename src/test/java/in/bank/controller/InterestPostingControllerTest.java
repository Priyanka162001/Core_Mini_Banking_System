package in.bank.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.bank.config.JwtFilter;
import in.bank.config.TestSecurityConfig;
import in.bank.dto.InterestPostingResponseDTO;
import in.bank.security.JwtService;
import in.bank.service.InterestPostingService;
import in.bank.service.InterestPostingService.JobSummary;

import jakarta.servlet.FilterChain;

@EnableMethodSecurity
@WebMvcTest(InterestPostingController.class)
@Import({InterestPostingController.class, TestSecurityConfig.class})
@DisplayName("Interest Posting Controller Tests")
class InterestPostingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InterestPostingService interestPostingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtFilter jwtFilter;

    private InterestPostingResponseDTO sampleResponseDTO;
    private JobSummary sampleJobSummary;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(
                invocation.getArgument(0),
                invocation.getArgument(1)
            );
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());

        sampleResponseDTO = InterestPostingResponseDTO.builder()
                .id(1L)
                .accountId(100L)
                .interestAmount(new BigDecimal("125.50"))
                .balanceBefore(new BigDecimal("5000.00"))
                .balanceAfter(new BigDecimal("5125.50"))
                .annualInterestRate(new BigDecimal("4.5"))
                .postingMonth(1)
                .postingYear(2024)
                .status("POSTED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(1L)
                .updatedBy(1L)
                .build();

        sampleJobSummary = new JobSummary(1, 2024, 150, 145, 3, 2);

        when(jwtService.extractUsername(anyString())).thenReturn("testuser");
        when(jwtService.validateToken(anyString(), any())).thenReturn(true);
    }

    @Nested
    @DisplayName("POST /api/v1/interest/post - Run Interest Posting Job")
    class PostInterestTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("TC1: Successfully post interest for a period - all accounts successful")
        void testPostInterest_AllSuccess() throws Exception {
            JobSummary summary = new JobSummary(1, 2024, 100, 100, 0, 0);
            when(interestPostingService.postInterestForPeriod(1, 2024)).thenReturn(summary);

            mockMvc.perform(post("/api/v1/interest/post")
                    .param("month", "1")
                    .param("year", "2024")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("✅ Interest posted successfully for 1/2024"))
                    .andExpect(jsonPath("$.month").value(1))
                    .andExpect(jsonPath("$.year").value(2024))
                    .andExpect(jsonPath("$.totalAccounts").value(100))
                    .andExpect(jsonPath("$.posted").value(100))
                    .andExpect(jsonPath("$.skipped").value(0))
                    .andExpect(jsonPath("$.failed").value(0));

            verify(interestPostingService, times(1)).postInterestForPeriod(1, 2024);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("TC2: Interest already posted for all accounts - all skipped")
        void testPostInterest_AllSkipped() throws Exception {
            JobSummary summary = new JobSummary(1, 2024, 100, 0, 100, 0);
            when(interestPostingService.postInterestForPeriod(1, 2024)).thenReturn(summary);

            mockMvc.perform(post("/api/v1/interest/post")
                    .param("month", "1")
                    .param("year", "2024")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("⏭ Interest already posted for 1/2024 — no accounts updated"))
                    .andExpect(jsonPath("$.posted").value(0))
                    .andExpect(jsonPath("$.skipped").value(100));

            verify(interestPostingService, times(1)).postInterestForPeriod(1, 2024);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("TC3: Interest posting failed for all accounts - covers yellow line condition (failed > 0 && posted == 0)")
        void testPostInterest_AllFailed() throws Exception {
            // This covers: else if (summary.failed() > 0 && summary.posted() == 0)
            JobSummary summary = new JobSummary(1, 2024, 100, 0, 0, 100);
            when(interestPostingService.postInterestForPeriod(1, 2024)).thenReturn(summary);

            mockMvc.perform(post("/api/v1/interest/post")
                    .param("month", "1")
                    .param("year", "2024")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("❌ Interest posting failed for all accounts for 1/2024"))
                    .andExpect(jsonPath("$.failed").value(100))
                    .andExpect(jsonPath("$.posted").value(0))
                    .andExpect(jsonPath("$.skipped").value(0));

            verify(interestPostingService, times(1)).postInterestForPeriod(1, 2024);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("TC4: Partial success - some posted, some failed")
        void testPostInterest_PartialSuccess() throws Exception {
            JobSummary summary = new JobSummary(1, 2024, 150, 100, 20, 30);
            when(interestPostingService.postInterestForPeriod(1, 2024)).thenReturn(summary);

            mockMvc.perform(post("/api/v1/interest/post")
                    .param("month", "1")
                    .param("year", "2024")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("⚠️ Interest posting partially completed for 1/2024"))
                    .andExpect(jsonPath("$.posted").value(100))
                    .andExpect(jsonPath("$.skipped").value(20))
                    .andExpect(jsonPath("$.failed").value(30));

            verify(interestPostingService, times(1)).postInterestForPeriod(1, 2024);
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("TC5: Access denied for non-ADMIN user")
        void testPostInterest_AccessDenied() throws Exception {
            mockMvc.perform(post("/api/v1/interest/post")
                    .param("month", "1")
                    .param("year", "2024")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());

            verify(interestPostingService, never()).postInterestForPeriod(anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/interest/history/account/{accountId}")
    class GetAccountHistoryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("TC6: Get interest history for specific account - returns list with all fields")
        void testGetAccountHistory_Success() throws Exception {
            List<InterestPostingResponseDTO> historyList = Arrays.asList(
                    sampleResponseDTO,
                    InterestPostingResponseDTO.builder()
                            .id(2L)
                            .accountId(100L)
                            .interestAmount(new BigDecimal("130.75"))
                            .balanceBefore(new BigDecimal("5125.50"))
                            .balanceAfter(new BigDecimal("5256.25"))
                            .annualInterestRate(new BigDecimal("4.5"))
                            .postingMonth(2)
                            .postingYear(2024)
                            .status("POSTED")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .createdBy(1L)
                            .updatedBy(1L)
                            .build()
            );
            
            when(interestPostingService.getHistoryForAccount(100L)).thenReturn(historyList);

            mockMvc.perform(get("/api/v1/interest/history/account/100")
                    .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].accountId").value(100))
                    .andExpect(jsonPath("$[0].interestAmount").value(125.50))
                    .andExpect(jsonPath("$[0].postingMonth").value(1))
                    .andExpect(jsonPath("$[0].postingYear").value(2024))
                    .andExpect(jsonPath("$[0].balanceBefore").value(5000.00))
                    .andExpect(jsonPath("$[0].balanceAfter").value(5125.50))
                    .andExpect(jsonPath("$[0].status").value("POSTED"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].postingMonth").value(2));

            verify(interestPostingService, times(1)).getHistoryForAccount(100L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("TC7: Get interest history - empty list returned")
        void testGetAccountHistory_EmptyList() throws Exception {
            when(interestPostingService.getHistoryForAccount(999L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/interest/history/account/999")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(interestPostingService, times(1)).getHistoryForAccount(999L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/interest/history/period")
    class GetPeriodHistoryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("TC8: Get all interest postings for a period")
        void testGetPeriodHistory_Success() throws Exception {
            List<InterestPostingResponseDTO> periodHistory = Arrays.asList(
                    sampleResponseDTO,
                    InterestPostingResponseDTO.builder()
                            .id(3L)
                            .accountId(200L)
                            .interestAmount(new BigDecimal("200.00"))
                            .balanceBefore(new BigDecimal("10000.00"))
                            .balanceAfter(new BigDecimal("10200.00"))
                            .annualInterestRate(new BigDecimal("4.5"))
                            .postingMonth(1)
                            .postingYear(2024)
                            .status("POSTED")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .createdBy(1L)
                            .updatedBy(1L)
                            .build()
            );
            when(interestPostingService.getHistoryForPeriod(1, 2024)).thenReturn(periodHistory);

            mockMvc.perform(get("/api/v1/interest/history/period")
                    .param("month", "1")
                    .param("year", "2024")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].accountId").value(100))
                    .andExpect(jsonPath("$[1].accountId").value(200))
                    .andExpect(jsonPath("$[1].interestAmount").value(200.00));

            verify(interestPostingService, times(1)).getHistoryForPeriod(1, 2024);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/interest/my-history/{accountId}")
    class GetMyHistoryTests {

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("TC9: Customer can view their own interest history")
        void testGetMyHistory_Success() throws Exception {
            List<InterestPostingResponseDTO> historyList = Arrays.asList(sampleResponseDTO);
            when(interestPostingService.getHistoryForAccount(100L)).thenReturn(historyList);

            mockMvc.perform(get("/api/v1/interest/my-history/100")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].accountId").value(100))
                    .andExpect(jsonPath("$[0].postingMonth").value(1))
                    .andExpect(jsonPath("$[0].postingYear").value(2024));

            verify(interestPostingService, times(1)).getHistoryForAccount(100L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("TC10: ADMIN cannot access USER endpoint - returns forbidden")
        void testGetMyHistory_AdminNotAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/interest/my-history/100")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());

            verify(interestPostingService, never()).getHistoryForAccount(anyLong());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("TC11: Customer can access their own account history (alternative account)")
        void testGetMyHistory_OwnAccountAlternative() throws Exception {
            List<InterestPostingResponseDTO> historyList = Arrays.asList(sampleResponseDTO);
            when(interestPostingService.getHistoryForAccount(100L)).thenReturn(historyList);

            mockMvc.perform(get("/api/v1/interest/my-history/100")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(interestPostingService, times(1)).getHistoryForAccount(100L);
        }
    }
}