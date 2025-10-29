package ru.practicum.statsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.statsserver.model.EndpointHit;
import ru.practicum.statsdto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    @Query("SELECT new ru.practicum.statsdto.ViewStats(h.app, h.uri, COUNT(h.ip)) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN ?1 AND ?2 " +
            "AND (COALESCE(?3, NULL) IS NULL OR h.uri IN ?3) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.ip) DESC")
    List<ViewStats> findStats(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("SELECT new ru.practicum.statsdto.ViewStats(h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN ?1 AND ?2 " +
            "AND (COALESCE(?3, NULL) IS NULL OR h.uri IN ?3) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStats> findUniqueStats(LocalDateTime start, LocalDateTime end, List<String> uris);
}
