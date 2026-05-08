package club.ttg.online.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "online.redis.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OnlineCleanupScheduler
{
    private final OnlineUserService service;

    @Scheduled(fixedDelayString = "#{@onlineProperties.redis.cleanup.fixedDelay.toMillis()}")
    public void cleanupExpiredUsers()
    {
        service.cleanupExpired(Instant.now());
    }
}
