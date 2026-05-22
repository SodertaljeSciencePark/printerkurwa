package nti.te4.printerkurwa.Configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper toolsObjectMapper() {
        return new ObjectMapper();
    }
}