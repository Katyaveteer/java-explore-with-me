package ru.practicum.statsserver.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.statsserver.model.EndpointHitEntity;
import ru.practicum.statsserver.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository repository;

    public void save(EndpointHit dto) {
        EndpointHitEntity entity = EndpointHitEntity.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
        repository.save(entity);
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        return unique ?
                repository.getUniqueStats(start, end, uris) :
                repository.getAllStats(start, end, uris);
    }
}
