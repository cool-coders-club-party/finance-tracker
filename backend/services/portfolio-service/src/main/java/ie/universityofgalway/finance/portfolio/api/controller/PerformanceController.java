package ie.universityofgalway.finance.portfolio.api.controller;

import ie.universityofgalway.finance.portfolio.api.dto.HoldingPerformanceResponse;
import ie.universityofgalway.finance.portfolio.application.service.PerformanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
public class PerformanceController {

    private static final String CURRENT_USER = "user-1"; // until real auth exists

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping("/{id}/performance")
    public List<HoldingPerformanceResponse> performance(@PathVariable Long id) {
        return performanceService.getPerformance(id, CURRENT_USER).stream()
                .map(HoldingPerformanceResponse::from)
                .toList();
    }
}
