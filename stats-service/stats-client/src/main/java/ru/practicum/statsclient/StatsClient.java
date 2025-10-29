package ru.practicum.statsclient;

import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StatsClient {
    private final RestTemplate rest;
    private final String serverUrl;

    public StatsClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.rest = new RestTemplate();
    }

    public void hit(EndpointHit endpointHit) {
        makeAndSendRequest(HttpMethod.POST, "/hit", null, endpointHit);
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    @Nullable List<String> uris, @Nullable Boolean unique) {
        Map<String, Object> parameters = new HashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        parameters.put("start", start.format(formatter));
        parameters.put("end", end.format(formatter));

        StringBuilder path = new StringBuilder("/stats?start={start}&end={end}");

        if (uris != null && !uris.isEmpty()) {
            parameters.put("uris", String.join(",", uris));
            path.append("&uris={uris}");
        }

        if (unique != null) {
            parameters.put("unique", unique);
            path.append("&unique={unique}");
        }

        ResponseEntity<ViewStats[]> response = makeAndSendRequest(HttpMethod.GET, path.toString(),
                parameters, null);

        return response != null ? Arrays.asList(Objects.requireNonNull(response.getBody())) : Collections.emptyList();
    }

    private <T> ResponseEntity<T> makeAndSendRequest(HttpMethod method, String path,
                                                     @Nullable Map<String, Object> parameters,
                                                     @Nullable Object body) {
        HttpEntity<Object> requestEntity = new HttpEntity<>(body, defaultHeaders());

        ResponseEntity<T> response;
        try {
            if (parameters != null) {
                response = rest.exchange(serverUrl + path, method, requestEntity, (Class<T>) Object.class, parameters);
            } else {
                response = rest.exchange(serverUrl + path, method, requestEntity, (Class<T>) Object.class);
            }
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }

        return response;
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}