package club.ttg.online;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig
{
    @Bean
    public OpenAPI onlineOpenApi()
    {
        return new OpenAPI()
                .info(new Info()
                        .title("TTG Online Service API")
                        .description("API for heartbeat tracking and online user statistics.")
                        .version("v1"))
                .components(new Components());
    }
}
