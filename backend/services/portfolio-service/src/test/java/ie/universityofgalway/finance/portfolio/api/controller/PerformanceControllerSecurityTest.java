package ie.universityofgalway.finance.portfolio.api.controller;

import ie.universityofgalway.finance.portfolio.application.service.PerformanceService;
import ie.universityofgalway.finance.portfolio.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@WebMvcTest(PerformanceController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=test-only-secret-that-is-at-least-32-bytes-long")
public class PerformanceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerformanceService performanceService;

    @Test
    void performance_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/portfolios/5/performance"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void performance_withGarbageBearerToken_returns401() throws Exception {
        mockMvc.perform(get("/api/portfolios/5/performance")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void performance_usesTokenSubjectAsUserId() throws Exception {
        when(performanceService.getPerformance(5L, "alice-id")).thenReturn(List.of());

        mockMvc.perform(get("/api/portfolios/5/performance")
                .with(jwt().jwt(j -> j.subject("alice-id"))))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));

        verify(performanceService).getPerformance(5L, "alice-id");
        verify(performanceService, never()).getPerformance(5L, "user-1");
    }

    @Test
    void performance_differentUsersReachServiceWithTheirOwnId() throws Exception {
        mockMvc.perform(get("/api/portfolios/5/performance")
                .with(jwt().jwt(j -> j.subject("bob-id"))))
                .andExpect(status().isOk());

        verify(performanceService).getPerformance(5L, "bob-id");
        verify(performanceService, never()).getPerformance(5L, "alice-id");
    }
}
