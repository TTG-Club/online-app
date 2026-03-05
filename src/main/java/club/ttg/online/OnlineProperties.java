package club.ttg.online;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "online")
public class OnlineProperties
{
    private List<String> allowedSites = new ArrayList<>();

    private long defaultWindowMinutes = 30;
    private long minWindowMinutes = 1;
    private long maxWindowMinutes = 1440;

    private Redis redis = new Redis();
    private Security security = new Security();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Redis
    {
        private String keyPrefix = "online:";
    }

    @Getter
    @Setter
    public static class Security
    {
        private boolean enabled = true;
        private String header = "X-Online-Token";
        private String token = "change-me";
    }

    @Getter
    @Setter
    public static class Cors
    {
        private boolean enabled = false;
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = new ArrayList<>();
        private List<String> allowedHeaders = new ArrayList<>();
        private boolean allowCredentials = false;
    }
}