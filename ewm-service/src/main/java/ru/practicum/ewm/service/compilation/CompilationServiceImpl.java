package ru.practicum.ewm.service.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.enums.RequestStatus;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.statsdto.ViewStatsRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final EventMapper eventMapper;

    // public
    // получение подборок событий
    @Override
    public List<CompilationDto> getCompilationList(Boolean pinned, Integer from, Integer size) {

        int safeFrom = (from != null) ? from : 0;
        int safeSize = (size != null) ? size : 10;

        Pageable pageable = PageRequest.of(safeFrom / safeSize, safeSize);

        Page<Compilation> compilationPage;

        if (pinned != null) {
            compilationPage = compilationRepository.findAllByPinnedOrderByIdDesc(pinned, pageable);
        } else {
            compilationPage = compilationRepository.findAll(pageable);
        }

        List<Compilation> compilationList = compilationPage.getContent();

        // 1. Собираем все события из всех компиляций
        List<EventShortDto> allEvents = compilationList.stream()
                .flatMap(c -> c.getEvents().stream())
                .map(eventMapper::toEventShortDto)
                .toList();

        List<Long> eventIds = allEvents.stream()
                .map(EventShortDto::getId)
                .toList();

        // 2. Получаем confirmedRequests одним запросом
        Map<Long, Long> confirmedCounts = requestRepository.countConfirmedByEventIds(eventIds);

        // 3. Получаем views одним запросом к statsClient
        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        ViewStatsRequest viewStatsRequest = new ViewStatsRequest(
                LocalDateTime.now().minusYears(100),
                LocalDateTime.now(),
                uris,
                true
        );

        List<ViewStats> viewStatsList = statsClient.getStats(viewStatsRequest);

        Map<Long, Long> viewsMap = viewStatsList.stream()
                .collect(Collectors.toMap(
                        v -> Long.parseLong(v.getUri().split("/")[2]),
                        ViewStats::getHits
                ));

        // 4. Создаем Map для мгновенного доступа к событиям по id
        Map<Long, EventShortDto> eventMap = allEvents.stream()
                .peek(e -> {
                    e.setConfirmedRequests(confirmedCounts.getOrDefault(e.getId(), 0L));
                    e.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                })
                .collect(Collectors.toMap(EventShortDto::getId, e -> e));

        // 5. Собираем компиляции с обновлёнными событиями
        return compilationList.stream()
                .map(compilation -> {
                    CompilationDto dto = compilationMapper.toCompilationDto(compilation);
                    List<EventShortDto> updatedEvents = compilation.getEvents().stream()
                            .map(e -> eventMap.get(e.getId()))
                            .toList();
                    dto.setEvents(updatedEvents);
                    return dto;
                })
                .toList();
    }


    // получение подборки событие по его id
    @Override
    public CompilationDto getCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборки не существует с Id: " + compilationId));

        // переводим в ДТО и сохраняем ConfirmedRequestsAndViews
        CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);
        return addConfirmedRequestsAndViews(compilationDto);
    }

    // admin
    // добавление новой подборки
    @Override
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        // если в подборке уже есть какие-то события, то их нужно сохранить
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            // получаем список id событий в этой подборке
            Set<Long> eventIdList = newCompilationDto.getEvents();

            // выгружаем все события
            Set<Event> events = eventRepository.findAllByIdIn(eventIdList);
            Compilation compilation = compilationMapper.toCompilation(newCompilationDto);

            // сохраняем все события в подборке и сохраняем в репозитории
            compilation.setEvents(events);
            compilationRepository.save(compilation);

            // переводим в ДТО и сохраняем ConfirmedRequestsAndViews
            CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);
            return addConfirmedRequestsAndViews(compilationDto);
        }

        // новая подборка без событий
        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);
        if (compilation.getEvents() == null) {
            compilation.setEvents(new HashSet<>());
        }
        compilationRepository.save(compilation);
        return compilationMapper.toCompilationDto(compilation);
    }


    // удаление подборки
    @Override
    public void deleteCompilation(Long compilationId) {
        compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("Подборки не существует с Id: " + compilationId));
        compilationRepository.deleteById(compilationId);
    }

    // обновить информацию о подборке
    @Override
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest updateCompilationRequest) {
        // выгружаем подборку из БД
        Compilation compilation = compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("Подборки не существует с Id: " + compilationId));

        // если в присланной подборке есть события, то сохраняем их в выгруженной подборке
        if (updateCompilationRequest.getEvents() != null && !updateCompilationRequest.getEvents().isEmpty()) {
            Set<Long> eventIdList = updateCompilationRequest.getEvents();
            Set<Event> events = eventRepository.findAllByIdIn(eventIdList);
            compilation.setEvents(events);
        }
        // обновляем закреплено на главной странице или нет
        if (updateCompilationRequest.getPinned() != null) {
            compilation.setPinned(updateCompilationRequest.getPinned());
        }
        // обновляем название/заголовок
        if (updateCompilationRequest.getTitle() != null) {
            compilation.setTitle(updateCompilationRequest.getTitle());
        }
        // сохраняем в БД
        compilationRepository.save(compilation);

        CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);

        return addConfirmedRequestsAndViews(compilationDto);
    }

    private CompilationDto addConfirmedRequestsAndViews(CompilationDto compilationDto) {
        for (EventShortDto eventDto : compilationDto.getEvents()) {
            // Добавить сonfirmedRequests к каждому событию
            eventDto.setConfirmedRequests(
                    requestRepository.countByEventIdAndStatus(eventDto.getId(), RequestStatus.CONFIRMED));

            // Добавить views к каждому событию
            List<String> uris = new ArrayList<>();

            // создаем uri для обращения к базе данных статистики
            uris.add("/events/" + eventDto.getId());
            ViewStatsRequest viewStatsRequest = new ViewStatsRequest(
                    LocalDateTime.now().minusYears(100),
                    LocalDateTime.now(),
                    uris,
                    true);
            List<ViewStats> viewStatsList = statsClient.getStats(viewStatsRequest);
            if (viewStatsList.isEmpty()) {
                eventDto.setViews(0L);
            } else {
                eventDto.setViews(viewStatsList.getFirst().getHits());
            }
        }
        return compilationDto;
    }
}
