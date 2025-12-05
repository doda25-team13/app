package frontend.ctrl;

import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SessionMetricsListener implements HttpSessionListener {

    private final AtomicInteger activeSessions = new AtomicInteger(0);

    public SessionMetricsListener(MeterRegistry registry) {
        Gauge.builder("app_active_sessions", activeSessions, AtomicInteger::get)
                .description("Number of active HTTP sessions")
                .register(registry);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        activeSessions.incrementAndGet();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        activeSessions.decrementAndGet();
    }
}

