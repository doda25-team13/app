package frontend.ctrl;

import java.net.URI;
import java.net.URISyntaxException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Controller;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import frontend.data.Sms;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/sms")
public class FrontendController {

    private String modelHost;

    private RestTemplateBuilder rest;
    private final Counter requestCounter;
    private final Timer latencyHistogram;

    public FrontendController(RestTemplateBuilder rest, Environment env, MeterRegistry registry) {
        this.rest = rest;
        this.modelHost = env.getProperty("MODEL_HOST");
        assertModelHost();
        // 1. Counter: Tracks total requests
        this.requestCounter = Counter.builder("app_requests_total")
                .description("Total number of requests to the app")
                .register(registry);

        // 2. Histogram (Timer in Micrometer): Tracks latency distribution
        this.latencyHistogram = Timer.builder("app_request_latency_seconds")
                .description("Request latency in seconds")
                .publishPercentileHistogram() // Important for heatmap visualization in Grafana
                .register(registry);
    }

    private void assertModelHost() {
        if (modelHost == null || modelHost.strip().isEmpty()) {
            System.err.println("ERROR: ENV variable MODEL_HOST is null or empty");
            System.exit(1);
        }
        modelHost = modelHost.strip();
        if (modelHost.indexOf("://") == -1) {
            var m = "ERROR: ENV variable MODEL_HOST is missing protocol, like \"http://...\" (was: \"%s\")\n";
            System.err.printf(m, modelHost);
            System.exit(1);
        } else {
            System.out.printf("Working with MODEL_HOST=\"%s\"\n", modelHost);
        }
    }

    @GetMapping("")
    public String redirectToSlash(HttpServletRequest request) {
        // relative REST requests in JS will end up on / and not on /sms
        return "redirect:" + request.getRequestURI() + "/";
    }

    @GetMapping("/")
    public String index(Model m, HttpServletRequest request) {
        // Record Latency (Histogram)
        HttpSession session = request.getSession(true);
        Timer.Sample sample = Timer.start();
        try {
            requestCounter.increment();
            m.addAttribute("hostname", modelHost);
            return "sms/index";
        } finally {
            // Stop Timer and record duration
            sample.stop(latencyHistogram);
        }
    }

    @PostMapping({ "", "/" })
    @ResponseBody
    public Sms predict(@RequestBody Sms sms) {
        System.out.printf("Requesting prediction for \"%s\" ...\n", sms.sms);
        sms.result = getPrediction(sms);
        System.out.printf("Prediction: %s\n", sms.result);
        return sms;
    }

    private String getPrediction(Sms sms) {
        try {
            var url = new URI(modelHost + "/predict");
            var c = rest.build().postForEntity(url, sms, Sms.class);
            return c.getBody().result.trim();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}