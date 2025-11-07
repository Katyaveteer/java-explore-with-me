package ru.practicum.statsclient;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StatsClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestTemplate restTemplate;

    @Value("${stats-server.url:http://localhost:9090}")
    private String statsServerUrl;

    public void hit(String uri, String ip) {
        String url = statsServerUrl + "/hit";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("app", "ewm-main-service");
        parameters.put("uri", uri);
        parameters.put("ip", ip);
        parameters.put("timestamp", LocalDateTime.now().format(FORMATTER));

        restTemplate.postForEntity(url, parameters, Object.class);
    }

    public Long getViews(Long eventId) {
        String url = statsServerUrl + "/stats?start={start}&end={end}&uris={uris}&unique={unique}";

        String start = LocalDateTime.now().minusYears(100).format(FORMATTER);
        String end = LocalDateTime.now().plusYears(100).format(FORMATTER);
        String uri = "/events/" + eventId;

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", start);
        parameters.put("end", end);
        parameters.put("uris", uri);
        parameters.put("unique", false);

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class, parameters);

        List<Map<String, Object>> data = response.getBody();
        if (data != null && !data.isEmpty()) {
            Object hits = data.get(0).get("hits");
            return Long.valueOf(String.valueOf(hits));
        }

        return 0L;
    }
}

