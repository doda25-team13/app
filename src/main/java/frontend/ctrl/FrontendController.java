package frontend.ctrl;

import java.net.URI;
import java.net.URISyntaxException;

import frontend.metrics.CustomMetricsRegistry;
import jakarta.servlet.http.HttpServletRequest;
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

@Controller
@RequestMapping(path = "/sms")
public class FrontendController {
    private final CustomMetricsRegistry metricsRegistry;

    private String modelHost;

    private RestTemplateBuilder rest;

    public FrontendController(RestTemplateBuilder rest, Environment env, CustomMetricsRegistry registry) {
        this.rest = rest;
        this.modelHost = env.getProperty("MODEL_HOST");
        this.metricsRegistry = registry;
        assertModelHost();
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
        m.addAttribute("hostname", modelHost);
        return "sms/index";
    }

    @PostMapping({ "", "/" })
    @ResponseBody
    public Sms predict(@RequestBody Sms sms) {
        System.out.printf("Requesting prediction for \"%s\" ...\n", sms.sms);
        try {
            // Gauge: Set length of current SMS
            metricsRegistry.incrementSmsRequests();
            int length = sms.sms.length();

            metricsRegistry.setLastSmsLength(length);
            metricsRegistry.recordSmsLength(length);
        } catch (Exception metricsError) {
            System.err.println("Failed to record metrics: " + metricsError.getMessage());
        }
        sms.result = getPrediction(sms);
        System.out.printf("Prediction: %s\n", sms.result);
        return sms;
    }

    private String getPrediction(Sms sms) {
        long startTime = System.nanoTime();
        try {
            var url = new URI(modelHost + "/predict");
            var c = rest.build().postForEntity(url, sms, Sms.class);
            return c.getBody().result.trim();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            long durationNano = System.nanoTime() - startTime;
            double durationSeconds = durationNano / 1_000_000_000.0;

            metricsRegistry.recordRequestDuration(durationSeconds);
        }
    }
}