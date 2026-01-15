package frontend.metrics;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CustomMetricsRegistry {

    // 1. Counter: "sms_requests_total"
    private final AtomicLong smsRequestsTotal = new AtomicLong(0);

    // 2. Gauge: "last_sms_length_characters"
    private final AtomicInteger lastSmsLength = new AtomicInteger(0);

    // 3. Histogram: "request_duration"
    // Buckets: 0.01, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 1.0, 2.0
    private final double[] durationBucketBoundaries = {0.01, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 1.0, 2.0};
    private final AtomicLong[] durationBucketCounts = new AtomicLong[durationBucketBoundaries.length + 1]; // +1 for +Inf
    private double totalDurationSum = 0.0;
    private final AtomicLong durationCount = new AtomicLong(0);

    // 4. Histogram: "sms_length"
    // Buckets: 5.0, 10.0, 20.0, 30.0, 40.0, 50.0, 100.0, 200.0, 500.0
    private final double[] smsLengthBucketBoundaries = {5.0, 10.0, 20.0, 30.0, 40.0, 50.0, 100.0, 200.0, 500.0};
    private final AtomicLong[] smsLengthBucketCounts = new AtomicLong[smsLengthBucketBoundaries.length + 1];
    private double totalSmsLengthSum = 0.0;
    private final AtomicLong smsLengthCount = new AtomicLong(0);


    public CustomMetricsRegistry() {
        // Initialize bucket counters
        for (int i = 0; i < durationBucketCounts.length; i++) {
            durationBucketCounts[i] = new AtomicLong(0);
        }
        for (int i = 0; i < smsLengthBucketCounts.length; i++) {
            smsLengthBucketCounts[i] = new AtomicLong(0);
        }
    }

    // --- Actions ---

    public void incrementSmsRequests() {
        smsRequestsTotal.incrementAndGet();
    }

    public void setLastSmsLength(int length) {
        lastSmsLength.set(length);
    }

    public void recordRequestDuration(double durationSeconds) {
        updateHistogram(durationSeconds, durationBucketBoundaries, durationBucketCounts, durationCount);
        addToDurationSum(durationSeconds);
    }

    public void recordSmsLength(double length) {
        updateHistogram(length, smsLengthBucketBoundaries, smsLengthBucketCounts, smsLengthCount);
        addToSmsLengthSum(length);
    }

    private void updateHistogram(double value, double[] boundaries, AtomicLong[] counts, AtomicLong totalCount) {
        totalCount.incrementAndGet();
        for (int i = 0; i < boundaries.length; i++) {
            if (value <= boundaries[i]) {
                counts[i].incrementAndGet();
            }
        }
        counts[counts.length - 1].incrementAndGet();
    }

    private synchronized void addToDurationSum(double value) {
        this.totalDurationSum += value;
    }

    private synchronized void addToSmsLengthSum(double value) {
        this.totalSmsLengthSum += value;
    }


    // --- Output Generator ---

    public String getPrometheusOutput() {
        StringBuilder sb = new StringBuilder();

        // 1. Counter: sms_requests_total
        appendCounter(sb, "sms_requests_total", "Total number of SMS prediction requests received", smsRequestsTotal);

        // 2. Gauge: last_sms_length_characters
        appendGauge(sb, "last_sms_length_characters", "Length of the last SMS in characters", lastSmsLength);

        // 3. Histogram: request_duration
        appendHistogram(sb, "request_duration", "Histogram of sms prediction request durations in seconds",
                durationBucketBoundaries, durationBucketCounts, totalDurationSum, durationCount);

        // 4. Histogram: sms_length
        appendHistogram(sb, "sms_length", "Histogram of SMS lengths in characters",
                smsLengthBucketBoundaries, smsLengthBucketCounts, totalSmsLengthSum, smsLengthCount);

        return sb.toString();
    }

    private void appendCounter(StringBuilder sb, String name, String help, AtomicLong value) {
        sb.append("# HELP ").append(name).append(" ").append(help).append("\n");
        sb.append("# TYPE ").append(name).append(" counter\n");
        sb.append(name).append(" ").append(value.get()).append("\n");
    }

    private void appendGauge(StringBuilder sb, String name, String help, AtomicInteger value) {
        sb.append("# HELP ").append(name).append(" ").append(help).append("\n");
        sb.append("# TYPE ").append(name).append(" gauge\n");
        sb.append(name).append(" ").append(value.get()).append("\n");
    }

    private void appendHistogram(StringBuilder sb, String name, String help, double[] boundaries, AtomicLong[] counts, double sum, AtomicLong count) {
        sb.append("# HELP ").append(name).append(" ").append(help).append("\n");
        sb.append("# TYPE ").append(name).append(" histogram\n");

        for (int i = 0; i < boundaries.length; i++) {
            sb.append(name).append("_bucket{le=\"").append(boundaries[i]).append("\"} ")
                    .append(counts[i].get()).append("\n");
        }
        sb.append(name).append("_bucket{le=\"+Inf\"} ").append(counts[counts.length - 1].get()).append("\n");
        sb.append(name).append("_sum ").append(sum).append("\n");
        sb.append(name).append("_count ").append(count.get()).append("\n");
    }
}