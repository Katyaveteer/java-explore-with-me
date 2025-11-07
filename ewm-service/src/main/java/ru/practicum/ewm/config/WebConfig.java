package ru.practicum.ewm.config;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.practicum.statsclient.StatsClient;

@Configuration
public class WebConfig {

    @Value("${stats-server.url:http://localhost:9090}")
    private String statsServerUrl;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    public StatsClient statsClient(ObjectMapper objectMapper) {
        return new StatsClient("ewm-main-service", statsServerUrl, objectMapper);
    }
}