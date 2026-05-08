package club.ttg.online;

import club.ttg.online.service.OnlineUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnlineControllerTest
{
    private final OnlineUserService service = mock(OnlineUserService.class);
    private final OnlineProperties properties = new OnlineProperties();
    private final OnlineController controller = new OnlineController(service, properties);

    OnlineControllerTest()
    {
        properties.setAllowedSites(List.of("new"));
    }

    @Test
    void heartbeatReturnsTotalForSiteDefaultWindow()
    {
        OnlineController.HeartbeatRequest request = new OnlineController.HeartbeatRequest(
                "new",
                "visitor-123",
                null,
                OnlineType.GUEST
        );
        when(service.getCount(eq("new"), eq(Duration.ofMinutes(30)), any(Instant.class)))
                .thenReturn(new OnlineUserService.OnlineCount(2, 3));

        ResponseEntity<OnlineController.HeartbeatResponse> response = controller.heartbeat(request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().total());
        verify(service).heartbeat(eq(OnlineType.GUEST), eq("new"), eq("visitor-123"), eq(null), any(Instant.class));
    }
}
