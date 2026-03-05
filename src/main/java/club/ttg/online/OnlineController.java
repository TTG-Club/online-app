package club.ttg.online;

import club.ttg.online.service.OnlineUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/online")
@Validated
public class OnlineController
{
    private final OnlineUserService service;
    private final OnlineProperties properties;

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@Valid @RequestBody HeartbeatRequest request)
    {
        String siteId = normalizeSiteId(request.siteId());

        if (!properties.getAllowedSites().contains(siteId))
        {
            return ResponseEntity.notFound().build();
        }

        Instant now = Instant.now();
        service.heartbeat(request.type(), siteId, request.key(), now);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public OnlineStatsResponse stats(@RequestParam(required = false) Long windowMinutes)
    {
        Duration window = resolveWindow(windowMinutes);
        Instant now = Instant.now();

        List<OnlineSiteStats> sites = properties.getAllowedSites().stream()
                .map(siteId -> buildSiteStats(siteId, window, now))
                .sorted(Comparator.comparing(OnlineSiteStats::siteId))
                .toList();

        OnlineTotals totals = sites.stream()
                .reduce(
                        new OnlineTotals(0, 0),
                        (acc, s) -> new OnlineTotals(acc.guests() + s.guests(), acc.registered() + s.registered()),
                        (a, b) -> new OnlineTotals(a.guests() + b.guests(), a.registered() + b.registered())
                );

        return new OnlineStatsResponse(
                window.toMinutes(),
                new OnlineTotalsWithTotal(totals.guests(), totals.registered(), totals.total()),
                sites
        );
    }

    @GetMapping("/stats/{siteId}")
    public OnlineSiteStatsResponse siteStats(
            @PathVariable String siteId,
            @RequestParam(required = false) Long windowMinutes
    )
    {
        String normalized = normalizeSiteId(siteId);

        if (!properties.getAllowedSites().contains(normalized))
        {
            return new OnlineSiteStatsResponse(
                    resolveWindow(windowMinutes).toMinutes(),
                    normalized,
                    0,
                    0,
                    0
            );
        }

        Duration window = resolveWindow(windowMinutes);
        Instant now = Instant.now();

        OnlineSiteStats stats = buildSiteStats(normalized, window, now);

        return new OnlineSiteStatsResponse(
                window.toMinutes(),
                stats.siteId(),
                stats.guests(),
                stats.registered(),
                stats.total()
        );
    }

    private OnlineSiteStats buildSiteStats(String siteId, Duration window, Instant now)
    {
        OnlineUserService.OnlineCount count = service.getCount(siteId, window, now);

        return new OnlineSiteStats(
                siteId,
                count.guests(),
                count.registered(),
                count.total()
        );
    }

    private Duration resolveWindow(Long requestedMinutes)
    {
        long minutes = (requestedMinutes == null)
                ? properties.getDefaultWindowMinutes()
                : requestedMinutes;

        long clamped = Math.max(properties.getMinWindowMinutes(), Math.min(minutes, properties.getMaxWindowMinutes()));

        return Duration.ofMinutes(clamped);
    }

    private static String normalizeSiteId(String siteId)
    {
        return (siteId == null) ? "" : siteId.trim().toLowerCase();
    }

    // ===== DTOs =====

    public record HeartbeatRequest(
            @NotBlank
            @Size(max = 32)
            @Pattern(regexp = "^[a-z0-9][a-z0-9_-]*$")
            String siteId,

            @NotBlank
            @Size(max = 128)
            String key,

            OnlineType type
    )
    {
        public HeartbeatRequest
        {
            if (type == null)
            {
                throw new IllegalArgumentException("type is required");
            }
        }
    }

    public record OnlineStatsResponse(
            long windowMinutes,
            OnlineTotalsWithTotal total,
            List<OnlineSiteStats> sites
    )
    {
    }

    public record OnlineSiteStatsResponse(
            long windowMinutes,
            String siteId,
            long guests,
            long registered,
            long total
    )
    {
    }

    public record OnlineSiteStats(
            String siteId,
            long guests,
            long registered,
            long total
    )
    {
    }

    public record OnlineTotals(long guests, long registered)
    {
        public long total()
        {
            return guests + registered;
        }
    }

    public record OnlineTotalsWithTotal(long guests, long registered, long total)
    {
    }
}