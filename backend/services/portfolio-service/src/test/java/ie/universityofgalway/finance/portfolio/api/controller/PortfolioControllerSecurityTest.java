package ie.universityofgalway.finance.portfolio.api.controller;

import ie.universityofgalway.finance.portfolio.application.service.PortfolioService;
import ie.universityofgalway.finance.portfolio.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PortfolioController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=test-only-secret-that-is-at-least-32-bytes-long")
class PortfolioControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioService portfolioService;

    @Test
    void listPortfolios_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/portfolios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void createPortfolio_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sneaky\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void listPortfolios_withGarbageBearerToken_returns401() throws Exception {
        // No jwt() shortcut here, so the real JwtDecoder from SecurityConfig runs.
        mockMvc.perform(get("/api/portfolios")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

    }

    @Test
    void listPortfolios_usesTokenSubjectAsUserId() throws Exception {
        when(portfolioService.getPortfolios("alice-id")).thenReturn(List.of());

        mockMvc.perform(get("/api/portfolios")
                .with(jwt().jwt(j -> j.subject("alice-id"))))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(portfolioService).getPortfolios("alice-id");
        verify(portfolioService, never()).getPortfolios("user-1");
    }

    @Test
    void getHoldings_usesTokenSubjectAsUserId() throws Exception {
        when(portfolioService.getHoldings(5L, "bob-id")).thenReturn(List.of());

        mockMvc.perform(get("/api/portfolios/5/holdings")
                .with(jwt().jwt(j -> j.subject("bob-id"))))
                .andExpect(status().isOk());

        verify(portfolioService).getHoldings(5L, "bob-id");
    }
}