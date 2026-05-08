package club.ttg.online;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig
{
    public static final String ONLINE_TOKEN_SCHEME = "onlineToken";

    @Bean
    public OpenAPI onlineOpenApi(OnlineProperties properties)
    {
        String tokenHeader = properties.getSecurity().getHeader();

        return new OpenAPI()
                .info(new Info()
                        .title("TTG Online Service API")
                        .description("API for heartbeat tracking and online user statistics.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(ONLINE_TOKEN_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(tokenHeader)))
                .addSecurityItem(new SecurityRequirement().addList(ONLINE_TOKEN_SCHEME));
    }
}
