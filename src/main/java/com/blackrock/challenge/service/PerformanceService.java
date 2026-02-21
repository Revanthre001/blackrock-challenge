package com.blackrock.challenge.service;

import com.blackrock.challenge.dto.PerformanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;

/**
 * Service for reporting live JVM performance metrics.
 *
 * <p>Tracks:
 * - Uptime: time since application startup (using start instant captured at construction)
 * - Memory: JVM heap memory used (in MB)
 * - Threads: live thread count (JVM ThreadMXBean — actual OS threads, not virtual threads)
 *
 * <p>This service is instantiated once per application context. The startTime
 * is recorded at Spring context startup, ensuring accurate uptime tracking.
 */
@Service
@Slf4j
public class PerformanceService {

    private final Instant startTime;
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;

    public PerformanceService() {
        // Capture startup time on Spring bean initialization
        this.startTime = Instant.now();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        log.info("PerformanceService initialized. Application start time: {}", startTime);
    }

    /**
     * Generates a current performance snapshot.
     *
     * @return performance metrics DTO
     */
    public PerformanceResponse getPerformance() {
        String uptime = formatUptime();
        String memory = formatMemory();
        int threads = threadMXBean.getThreadCount();

        log.debug("Performance snapshot — uptime: {}, memory: {}, threads: {}", uptime, memory, threads);

        return PerformanceResponse.builder()
                .time(uptime)
                .memory(memory)
                .threads(threads)
                .build();
    }

    /**
     * Formats elapsed time since startup as "HH:mm:ss.SSS".
     */
    private String formatUptime() {
        Duration elapsed = Duration.between(startTime, Instant.now());
        long totalSeconds = elapsed.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long millis = elapsed.toMillisPart();

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    /**
     * Returns JVM used heap memory in MB formatted to 2 decimal places.
     */
    private String formatMemory() {
        long usedBytes = memoryMXBean.getHeapMemoryUsage().getUsed();
        double usedMb = usedBytes / (1024.0 * 1024.0);
        return String.format("%.2f MB", usedMb);
    }
}
