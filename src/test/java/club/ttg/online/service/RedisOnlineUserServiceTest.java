package club.ttg.online.service;

import club.ttg.online.OnlineProperties;
import club.ttg.online.OnlineType;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class RedisOnlineUserServiceTest
{
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ZSetOperations<String, String> zSetOperations = mock(ZSetOperations.class);
    private final OnlineProperties properties = new OnlineProperties();
    private final RedisOnlineUserService service = new RedisOnlineUserService(redisTemplate, properties);

    RedisOnlineUserServiceTest()
    {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void registeredHeartbeatRemovesPreviousGuestKey()
    {
        Instant now = Instant.parse("2026-05-08T08:00:00Z");

        service.heartbeat(OnlineType.REGISTERED, "New", "user-42", "visitor-123", now);

        verify(zSetOperations).add("online:new:registered", "user-42", 1778227200000d);
        verify(zSetOperations).remove("online:new:guest", "visitor-123");
    }

    @Test
    void guestHeartbeatDoesNotRemovePreviousGuestKey()
    {
        Instant now = Instant.parse("2026-05-08T08:00:00Z");

        service.heartbeat(OnlineType.GUEST, "new", "visitor-123", "visitor-123", now);

        verify(zSetOperations).add("online:new:guest", "visitor-123", 1778227200000d);
        verify(zSetOperations, never()).remove("online:new:guest", "visitor-123");
    }

    @Test
    void getCountCleansExpiredUsersAndCountsOnlyUsersAfterWindow()
    {
        Instant now = Instant.parse("2026-05-08T08:00:00Z");
        Instant threshold = now.minus(Duration.ofMinutes(30));

        when(zSetOperations.count("online:new:guest", 1778225400001d, 1778227200000d))
                .thenReturn(2L);
        when(zSetOperations.count("online:new:registered", 1778225400001d, 1778227200000d))
                .thenReturn(3L);

        OnlineUserService.OnlineCount count = service.getCount("new", Duration.ofMinutes(30), now);

        assertEquals(2, count.guests());
        assertEquals(3, count.registered());
        assertEquals(5, count.total());
        verify(zSetOperations).removeRangeByScore("online:new:guest", 0d, (double) threshold.toEpochMilli());
        verify(zSetOperations).removeRangeByScore("online:new:registered", 0d, (double) threshold.toEpochMilli());
    }

    @Test
    void cleanupExpiredCleansEveryAllowedSiteAndType()
    {
        Instant now = Instant.parse("2026-05-08T08:00:00Z");
        Instant threshold = now.minus(Duration.ofMinutes(30));
        properties.setAllowedSites(List.of("new", "5e14"));

        service.cleanupExpired(now);

        verify(zSetOperations).removeRangeByScore("online:new:guest", 0d, (double) threshold.toEpochMilli());
        verify(zSetOperations).removeRangeByScore("online:new:registered", 0d, (double) threshold.toEpochMilli());
        verify(zSetOperations).removeRangeByScore("online:5e14:guest", 0d, (double) threshold.toEpochMilli());
        verify(zSetOperations).removeRangeByScore("online:5e14:registered", 0d, (double) threshold.toEpochMilli());
    }
}
