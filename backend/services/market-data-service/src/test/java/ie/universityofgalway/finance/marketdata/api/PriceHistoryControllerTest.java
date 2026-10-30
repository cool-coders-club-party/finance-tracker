package ie.universityofgalway.finance.marketdata.api;

import ie.universityofgalway.finance.marketdata.application.PriceHistoryService;
import ie.universityofgalway.finance.marketdata.domain.PricePoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PriceHistoryControllerTest {

    @Test
    void returnsDailyPricePoints() throws Exception {
        PriceHistoryService service = mock(PriceHistoryService.class);
        PriceHistoryController controller = new PriceHistoryController(service);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // The service supplies two ordered daily closes.
        when(service.getHistory("AAPL")).thenReturn(Optional.of(List.of(
                new PricePoint(LocalDate.parse("2026-10-28"), new BigDecimal("343.83")),
                new PricePoint(LocalDate.parse("2026-10-29"), new BigDecimal("348.91"))
        )));

        mockMvc.perform(get("/api/quotes/AAPL/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[0].date").value("2026-10-28"))
                .andExpect(jsonPath("$.points[0].close").value(343.83))
                .andExpect(jsonPath("$.points[1].date").value("2026-10-29"))
                .andExpect(jsonPath("$.points[1].close").value(348.91));
    }
}