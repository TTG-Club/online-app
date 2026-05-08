package club.ttg.online.service;

import club.ttg.online.OnlineProperties;
import club.ttg.online.OnlineType;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;

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
}
