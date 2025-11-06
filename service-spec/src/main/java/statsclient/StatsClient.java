package statsclient;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsClient {

    private final RestTemplate restTemplate;
    private final String statsServerUrl;

    public StatsClient(RestTemplateBuilder builder, org.springframework.core.env.Environment env) {
        this.restTemplate = builder.build();
        this.statsServerUrl = env.getProperty("stats-service.url", "http://localhost:9090");
    }

    public void hit(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHit hit = EndpointHit.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(String.valueOf(timestamp))
                .build();
        restTemplate.postForObject(statsServerUrl + "/hit", hit, Object.class);
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (uris == null || uris.isEmpty()) return List.of();

        String startStr = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String endStr = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String uriStr = uris.stream()
                .map(u -> "uris=" + u)
                .collect(Collectors.joining("&"));

        String url = String.format(
                "%s/stats?start=%s&end=%s&unique=%s&%s",
                statsServerUrl, startStr, endStr, unique, uriStr
        );

        ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(url, ViewStats[].class);
        return Arrays.asList(response.getBody());
    }
}