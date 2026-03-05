package club.ttg.online.service;

import club.ttg.online.OnlineType;

import java.time.Duration;
import java.time.Instant;

public interface OnlineUserService
{
    void heartbeat(OnlineType type, String siteId, String key, Instant now);

    OnlineCount getCount(String siteId, Duration window, Instant now);

    record OnlineCount(long guests, long registered)
    {
        public long total()
        {
            return guests + registered;
        }
    }
}