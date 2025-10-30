package ru.practicum.statsclient;



import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class StatsClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "http://localhost:9090";

    public void hit(EndpointHit hit) {
        restTemplate.postForEntity(baseUrl + "/hit", hit, Void.class);
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        StringBuilder url = new StringBuilder(baseUrl + "/stats?start=" + encode(start) + "&end=" + encode(end));
        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) url.append("&uris=").append(uri);
        }
        url.append("&unique=").append(unique);
        ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(url.toString(), ViewStats[].class);
        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

    private String encode(LocalDateTime dt) {
        return java.net.URLEncoder.encode(dt.toString().replace("T", " "), java.nio.charset.StandardCharsets.UTF_8);
    }
}
