package ru.practicum.statsserver.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.statsserver.exception.ValidationException;
import ru.practicum.statsserver.model.EndpointHitEntity;
import ru.practicum.statsserver.repository.StatsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository repository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public void saveHit(EndpointHit hitDto) {
        EndpointHitEntity entity = new EndpointHitEntity();
        entity.setApp(hitDto.getApp());
        entity.setUri(hitDto.getUri());
        entity.setIp(hitDto.getIp());
        entity.setTimestamp(LocalDateTime.parse(hitDto.getTimestamp(), formatter));
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<ViewStats> getStats(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime s = LocalDateTime.parse(start, formatter);
        LocalDateTime e = LocalDateTime.parse(end, formatter);

        if (s.isAfter(e)) {
            throw new ValidationException("Дата начала должна быть раньше даты окончания");
        }

        // Если uris пустой или null — не фильтруем по URI
        List<String> filteredUris = (uris != null && !uris.isEmpty()) ? uris : null;

        if (unique) {
            return repository.getUniqueStats(s, e, filteredUris);
        } else {
            return repository.getAllStats(s, e, filteredUris);
        }
    }

}
