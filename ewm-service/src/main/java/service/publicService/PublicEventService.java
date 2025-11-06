package service.publicService;

import dto.category.CategoryDto;
import dto.event.EventFullDto;
import dto.event.EventShortDto;
import dto.event.LocationDto;
import dto.user.UserShortDto;
import enums.EventState;
import enums.RequestStatus;
import exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import model.Event;
import org.springframework.stereotype.Service;
import repository.EventRepository;
import repository.RequestRepository;
import ru.practicum.statsdto.ViewStats;
import statsclient.StatsClient;
import utils.DateTimeUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicEventService {

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    public List<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid,
                                         LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                         Boolean onlyAvailable, String sort, int from, int size) {
        LocalDateTime now = LocalDateTime.now();
        if (rangeStart == null) rangeStart = now;
        if (rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new IllegalArgumentException("rangeEnd must be after rangeStart");
        }

        List<Event> events = eventRepository.findPublishedWithFilters(
                        text, categories, paid, rangeStart, rangeEnd
                ).stream()
                .filter(e -> !onlyAvailable || isAvailable(e))
                .collect(Collectors.toList());

        // Пагинация
        int start = Math.min(from, events.size());
        int end = Math.min(from + size, events.size());
        events = events.subList(start, end);

        // Получаем просмотры
        Map<Long, Long> views = getViews(events);
        // Получаем confirmedRequests
        Map<Long, Long> confirmed = getConfirmedRequests(events);

        return events.stream()
                .map(e -> toShortDto(e, views.get(e.getId()), confirmed.get(e.getId())))
                .sorted(getComparator(sort))
                .collect(Collectors.toList());
    }

    public EventFullDto getEvent(Long id) {
        Event event = eventRepository.findById(id)
                .filter(e -> e.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        Long views = getViews(List.of(event)).get(id);
        Long confirmed = getConfirmedRequests(List.of(event)).get(id);

        return toFullDto(event, views, confirmed);
    }

    private boolean isAvailable(Event e) {
        if (e.getParticipantLimit() == 0) return true;
        long confirmed = requestRepository.countByEventIdAndStatus(e.getId(), RequestStatus.CONFIRMED);
        return confirmed < e.getParticipantLimit();
    }

    private Map<Long, Long> getViews(List<Event> events) {
        if (events.isEmpty()) return new HashMap<>();
        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());
        List<ViewStats> stats = statsClient.getStats(
                LocalDateTime.of(1970, 1, 1, 0, 0, 0),
                LocalDateTime.now().plusYears(1),
                uris,
                true
        );
        Map<String, Long> statsMap = stats.stream()
                .collect(Collectors.toMap(ViewStats::getUri, ViewStats::getHits));
        return events.stream()
                .collect(Collectors.toMap(Event::getId, e -> statsMap.getOrDefault("/events/" + e.getId(), 0L)));
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) return new HashMap<>();
        Map<Long, Long> map = new HashMap<>();
        for (Event e : events) {
            long count = requestRepository.countByEventIdAndStatus(e.getId(), RequestStatus.CONFIRMED);
            map.put(e.getId(), count);
        }
        return map;
    }

    private Comparator<EventShortDto> getComparator(String sort) {
        if ("VIEWS".equals(sort)) {
            return Comparator.comparing(EventShortDto::getViews).reversed();
        }
        return Comparator.comparing(EventShortDto::getEventDate);
    }


    public EventShortDto toShortDto(Event e, Long views, Long confirmed) {
        return EventShortDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .annotation(e.getAnnotation())
                .paid(e.getPaid())
                .eventDate(DateTimeUtils.format(e.getEventDate()))
                .initiator(UserShortDto.builder().id(e.getInitiator().getId()).name(e.getInitiator().getName()).build())
                .category(CategoryDto.builder().id(e.getCategory().getId()).name(e.getCategory().getName()).build())
                .confirmedRequests(confirmed)
                .views(views)
                .build();
    }

    public EventShortDto toShortDto(Event e) {
        return toShortDto(e, 0L, 0L);
    }

    public EventFullDto toFullDto(Event e, Long views, Long confirmed) {
        return EventFullDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .annotation(e.getAnnotation())
                .description(e.getDescription())
                .paid(e.getPaid())
                .eventDate(DateTimeUtils.format(e.getEventDate()))
                .createdOn(DateTimeUtils.format(e.getCreatedOn()))
                .publishedOn(DateTimeUtils.format(e.getPublishedOn()))
                .participantLimit(e.getParticipantLimit())
                .requestModeration(e.getRequestModeration())
                .state(e.getState().name())
                .initiator(UserShortDto.builder().id(e.getInitiator().getId()).name(e.getInitiator().getName()).build())
                .category(CategoryDto.builder().id(e.getCategory().getId()).name(e.getCategory().getName()).build())
                .location(LocationDto.builder().lat(e.getLocation().getLat()).lon(e.getLocation().getLon()).build())
                .confirmedRequests(confirmed)
                .views(views)
                .build();
    }
}
