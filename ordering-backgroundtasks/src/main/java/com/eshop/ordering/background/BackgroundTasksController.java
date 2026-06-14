package com.eshop.ordering.background;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class BackgroundTasksController {
    private final AtomicReference<OffsetDateTime> lastRun = new AtomicReference<>();

    @Scheduled(fixedDelayString = "${eshop.ordering.check-update-time:30000}")
    public void checkGracePeriod() {
        lastRun.set(OffsetDateTime.now());
    }

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of("service", "ordering-backgroundtasks", "lastRun", String.valueOf(lastRun.get()));
    }
}
