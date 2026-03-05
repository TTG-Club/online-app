package club.ttg.online.service;

import club.ttg.online.OnlineProperties;
import club.ttg.online.OnlineType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RedisOnlineUserService implements OnlineUserService
{
    private final StringRedisTemplate redisTemplate;
    private final OnlineProperties properties;

    @Override
    public void heartbeat(OnlineType type, String siteId, String key, Instant now)
    {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(now, "now");

        String redisKey = redisKey(siteId, type);
        double score = (double) now.toEpochMilli();

        // обновляем lastSeen
        redisTemplate.opsForZSet().add(redisKey, key, score);

        // лёгкая чистка (по дефолтному окну) - чтобы ключи не пухли, даже если stats не вызывают
        Duration cleanupWindow = Duration.ofMinutes(properties.getDefaultWindowMinutes());
        cleanup(redisKey, now.minus(cleanupWindow));
    }

    @Override
    public OnlineCount getCount(String siteId, Duration window, Instant now)
    {
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(now, "now");

        String guestsKey = redisKey(siteId, OnlineType.GUEST);
        String registeredKey = redisKey(siteId, OnlineType.REGISTERED);

        Instant threshold = now.minus(window);

        cleanup(guestsKey, threshold);
        cleanup(registeredKey, threshold);

        long guests = countWindow(guestsKey, threshold, now);
        long registered = countWindow(registeredKey, threshold, now);

        return new OnlineCount(guests, registered);
    }

    private long countWindow(String key, Instant fromInclusive, Instant toInclusive)
    {
        Long result = redisTemplate.opsForZSet().count(
                key,
                (double) fromInclusive.toEpochMilli(),
                (double) toInclusive.toEpochMilli()
        );

        return (result == null) ? 0L : result;
    }

    private void cleanup(String key, Instant thresholdExclusive)
    {
        redisTemplate.opsForZSet().removeRangeByScore(key, 0d, (double) thresholdExclusive.toEpochMilli());
    }

    private String redisKey(String siteId, OnlineType type)
    {
        String prefix = properties.getRedis().getKeyPrefix();
        String normalizedSiteId = siteId.trim().toLowerCase();

        return prefix + normalizedSiteId + ":" + type.name().toLowerCase();
    }
}