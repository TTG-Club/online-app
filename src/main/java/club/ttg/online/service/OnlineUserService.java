package club.ttg.online.service;

import club.ttg.online.OnlineType;

import java.time.Duration;
import java.time.Instant;

public interface OnlineUserService
{
    /**
     * @param inWorld посетитель прямо сейчас находится в игровом мире. Признак живёт отдельно от
     *                {@link OnlineType}: играть можно и с аккаунтом, и без него, поэтому одним типом
     *                эти две оси не выражаются.
     */
    void heartbeat(OnlineType type, String siteId, String key, String previousGuestKey, boolean inWorld, Instant now);

    OnlineCount getCount(String siteId, Duration window, Instant now);

    OnlineCount getTotalCount(Duration window, Instant now);

    void cleanupExpired(Instant now);

    /**
     * @param players сколько из тех же посетителей сейчас в мирах — подмножество гостей и
     *                зарегистрированных, а не отдельное слагаемое, поэтому в {@link #total()} не входит.
     */
    record OnlineCount(long guests, long registered, long players)
    {
        public long total()
        {
            return guests + registered;
        }
    }
}
