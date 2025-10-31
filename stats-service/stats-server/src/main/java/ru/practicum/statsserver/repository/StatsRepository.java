package ru.practicum.statsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.statsserver.model.EndpointHitEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHitEntity, Long> {

    @Query("SELECT NEW ru.practicum.statsdto.ViewStats(h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM EndpointHitEntity h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR h.uri IN :uris) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStats> getUniqueStats(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   @Param("uris") List<String> uris);

    @Query("SELECT NEW ru.practicum.statsdto.ViewStats(h.app, h.uri, COUNT(h.id)) " +
            "FROM EndpointHitEntity h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR h.uri IN :uris) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.id) DESC")
    List<ViewStats> getAllStats(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end,
                                @Param("uris") List<String> uris);
}