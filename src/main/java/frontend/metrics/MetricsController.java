package frontend.metrics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    private final CustomMetricsRegistry metricsRegistry;

    public MetricsController(CustomMetricsRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
    }

    // Expose metrics at /metrics (default Prometheus scraper path)
    @GetMapping(value = "/metrics", produces = "text/plain")
    public String metrics() {
        return metricsRegistry.getPrometheusOutput();
    }
}