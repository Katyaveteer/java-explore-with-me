package ru.practicum.statsclient;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class StatsClient {
    private final RestTemplate rest;
    private final String statsBaseUrl;

    public StatsClient(RestTemplate rest, String statsBaseUrl) {
        this.rest = rest;
        this.statsBaseUrl = statsBaseUrl; // example: "http://localhost:9090"
    }

    public void postHit(EndpointHit hit) {
        HttpEntity<EndpointHit> request = new HttpEntity<>(hit);
        rest.postForLocation(statsBaseUrl + "/hit", request);
    }

    public List<ViewStats> getStats(String start, String end, List<String> uris, boolean unique) {
        StringBuilder url = new StringBuilder(statsBaseUrl + "/stats?start=" + encode(start) + "&end=" + encode(end) + "&unique=" + unique);
        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                url.append("&uris=").append(encode(uri));
            }
        }
        ResponseEntity<ViewStats[]> resp = rest.getForEntity(URI.create(url.toString()), ViewStats[].class);
        return Arrays.asList(Objects.requireNonNull(resp.getBody()));
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}

