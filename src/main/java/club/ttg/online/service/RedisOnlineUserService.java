package club.ttg.online.service;

import club.ttg.online.OnlineProperties;
import club.ttg.online.OnlineType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RedisOnlineUserService implements OnlineUserService
{
    /**
     * Набор тех, кто сейчас в игровом мире. Ключи в нём те же, что в наборах гостей и
     * зарегистрированных, поэтому один человек остаётся одним человеком, а не превращается в двоих.
     */
    private static final String IN_WORLD_BUCKET = "in-world";

    private final StringRedisTemplate redisTemplate;
    private final OnlineProperties properties;

    @Override
    public void heartbeat(
            OnlineType type,
            String siteId,
            String key,
            String previousGuestKey,
            boolean inWorld,
            Instant now
    )
    {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(now, "now");

        String redisKey = redisKey(siteId, bucket(type));
        String inWorldKey = redisKey(siteId, IN_WORLD_BUCKET);
        double score = (double) now.toEpochMilli();

        // обновляем lastSeen
        redisTemplate.opsForZSet().add(redisKey, key, score);

        // Из мира выходят молча, поэтому «уже не в мире» - это отсутствие признака в сигнале.
        // Без явного удаления посетитель числился бы играющим до конца окна учёта.
        if (inWorld)
        {
            redisTemplate.opsForZSet().add(inWorldKey, key, score);
        }
        else
        {
            redisTemplate.opsForZSet().remove(inWorldKey, key);
        }

        if (type == OnlineType.REGISTERED && StringUtils.hasText(previousGuestKey))
        {
            redisTemplate.opsForZSet().remove(redisKey(siteId, bucket(OnlineType.GUEST)), previousGuestKey);
            redisTemplate.opsForZSet().remove(inWorldKey, previousGuestKey);
        }

        // лёгкая чистка (по дефолтному окну) - чтобы ключи не пухли, даже если stats не вызывают
        Duration cleanupWindow = Duration.ofMinutes(properties.getDefaultWindowMinutes());
        Instant threshold = now.minus(cleanupWindow);

        cleanup(redisKey, threshold);
        cleanup(inWorldKey, threshold);
    }

    @Override
    public OnlineCount getCount(String siteId, Duration window, Instant now)
    {
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(now, "now");

        String guestsKey = redisKey(siteId, bucket(OnlineType.GUEST));
        String registeredKey = redisKey(siteId, bucket(OnlineType.REGISTERED));
        String inWorldKey = redisKey(siteId, IN_WORLD_BUCKET);

        Instant threshold = now.minus(window);

        cleanup(guestsKey, threshold);
        cleanup(registeredKey, threshold);
        cleanup(inWorldKey, threshold);

        long guests = countWindow(guestsKey, threshold, now);
        long registered = countWindow(registeredKey, threshold, now);
        long players = countWindow(inWorldKey, threshold, now);

        return new OnlineCount(guests, registered, players);
    }

    @Override
    public OnlineCount getTotalCount(Duration window, Instant now)
    {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(now, "now");

        return properties.getAllowedSites().stream()
                .map(siteId -> getCount(siteId, window, now))
                .reduce(
                        new OnlineCount(0, 0, 0),
                        (acc, count) -> new OnlineCount(
                                acc.guests() + count.guests(),
                                acc.registered() + count.registered(),
                                acc.players() + count.players()
                        )
                );
    }

    @Override
    public void cleanupExpired(Instant now)
    {
        Objects.requireNonNull(now, "now");

        Duration cleanupWindow = Duration.ofMinutes(properties.getDefaultWindowMinutes());
        Instant threshold = now.minus(cleanupWindow);

        properties.getAllowedSites().forEach(siteId -> {
            cleanup(redisKey(siteId, bucket(OnlineType.GUEST)), threshold);
            cleanup(redisKey(siteId, bucket(OnlineType.REGISTERED)), threshold);
            cleanup(redisKey(siteId, IN_WORLD_BUCKET), threshold);
        });
    }

    private long countWindow(String key, Instant fromInclusive, Instant toInclusive)
    {
        Instant activeFrom = fromInclusive.plusMillis(1);
        Long result = redisTemplate.opsForZSet().count(
                key,
                (double) activeFrom.toEpochMilli(),
                (double) toInclusive.toEpochMilli()
        );

        return (result == null) ? 0L : result;
    }

    private void cleanup(String key, Instant thresholdExclusive)
    {
        redisTemplate.opsForZSet().removeRangeByScore(key, 0d, (double) thresholdExclusive.toEpochMilli());
    }

    private String bucket(OnlineType type)
    {
        return type.name().toLowerCase();
    }

    private String redisKey(String siteId, String bucket)
    {
        String prefix = properties.getRedis().getKeyPrefix();
        String normalizedSiteId = siteId.trim().toLowerCase();

        return prefix + normalizedSiteId + ":" + bucket;
    }
}
