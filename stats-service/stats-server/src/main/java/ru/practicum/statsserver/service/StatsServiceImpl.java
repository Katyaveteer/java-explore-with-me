package ru.practicum.statsserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.statsserver.model.EndpointHit;
import ru.practicum.statsserver.repository.StatsRepository;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    public void saveHit(ru.practicum.statsdto.EndpointHit endpointHitDto) {
        EndpointHit endpointHit = EndpointHit.builder()
                .app(endpointHitDto.getApp())
                .uri(endpointHitDto.getUri())
                .ip(endpointHitDto.getIp())
                .timestamp(endpointHitDto.getTimestamp())
                .build();

        statsRepository.save(endpointHit);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (unique != null && unique) {
            return statsRepository.findUniqueStats(start, end, uris);
        } else {
            return statsRepository.findStats(start, end, uris);
        }
    }
}