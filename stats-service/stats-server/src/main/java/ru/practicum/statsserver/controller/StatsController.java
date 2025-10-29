package ru.practicum.statsserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.statsserver.service.StatsService;
import ru.practicum.statsdto.EndpointHit;
import ru.practicum.statsdto.ViewStats;


import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsController {
    private final StatsService statsService;

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void hit(@Valid @RequestBody EndpointHit endpointHit) {
        log.info("Получен запрос на сохранение информации о посещении: {}", endpointHit);
        statsService.saveHit(endpointHit);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(required = false, defaultValue = "false") Boolean unique) {

        log.info("Получен запрос на получение статистики с {} по {}, uris: {}, unique: {}",
                start, end, uris, unique);

        return statsService.getStats(start, end, uris, unique);
    }
}