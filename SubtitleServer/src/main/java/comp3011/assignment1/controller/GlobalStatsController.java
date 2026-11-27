package comp3011.assignment1.controller;

import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.dto.GlobalStatsResponse;
import comp3011.assignment1.service.GlobalStatsService;
import comp3011.assignment1.dto.UsageResponse;

/**
 * Exposes global speech-to-text usage statistics.
 */
@RestController
@RequestMapping("/api/v1/global")
public class GlobalStatsController {

    private final GlobalStatsService globalStatsService;

    /**
     * Creates the controller with its required statistics service.
     *
     * @param globalStatsService service maintaining global token usage
     */
    public GlobalStatsController(GlobalStatsService globalStatsService) {
        this.globalStatsService = globalStatsService;
    }

    /**
     * Returns cumulative token usage since server startup.
     *
     * @return current global token statistics
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalStatsResponse getGlobalStats() {
        return globalStatsService.getGlobalStats();
    }
    
    @GetMapping(
            value = "/usage",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UsageResponse getUsage() {
        return new UsageResponse(
                globalStatsService.getUsageSeconds());
    }
}