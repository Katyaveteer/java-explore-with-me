package ru.practicum.statsserver.service;

import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;

import java.util.List;


public interface StatsService {
    void saveHit(EndpointHit dto);

    List<ViewStats> getStats(String start, String end, List<String> uris, boolean unique);

}