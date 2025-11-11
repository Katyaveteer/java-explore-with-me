package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.enums.*;
import ru.practicum.ewm.exception.AlreadyExistsException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.*;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.statsdto.ViewStats;
import ru.practicum.statsdto.ViewStatsRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;



@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final RequestMapper requestMapper;

    // private
    // получение событий текущего пользователя
    @Override
    public List<EventShortDto> getEventsByInitiator(Long userId, Pageable pageable) {

        // 1. Получаем все события инициатора
        List<Event> eventList = eventRepository.findAllByInitiatorId(userId, pageable).toList();

        if (eventList.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Преобразуем события в DTO
        List<EventShortDto> eventShortDtoList = eventList.stream()
                .map(eventMapper::toEventShortDto)
                .toList();

        List<Long> eventIds = eventShortDtoList.stream()
                .map(EventShortDto::getId)
                .toList();

        // 3. Получаем confirmedRequests одним запросом
        Map<Long, Long> confirmedCounts = requestRepository.countConfirmedByEventIds(eventIds);

        // 4. Получаем просмотры одним запросом к statsClient
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

        // 5. Применяем confirmedRequests и views к DTO
        eventShortDtoList.forEach(e -> {
            e.setConfirmedRequests(confirmedCounts.getOrDefault(e.getId(), 0L));
            e.setViews(viewsMap.getOrDefault(e.getId(), 0L));
        });

        return eventShortDtoList;
    }


    // добавление нового события
    @Override
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не существует " + userId));

        if (newEventDto.getEventDate().minusHours(2).isBefore(LocalDateTime.now())) {
            throw new ValidationException("дата и время на которые намечено событие не может быть раньше, " +
                    "чем через два часа от текущего момента");
        }

        // формируем event для сохранения в БД
        Event eventToSave = eventMapper.toEvent(newEventDto);
        eventToSave.setState(EventState.PENDING);
        eventToSave.setCreatedOn(LocalDateTime.now());

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория не существует"));
        eventToSave.setCategory(category);
        eventToSave.setInitiator(user);
        eventRepository.save(eventToSave);

        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(eventToSave));
    }

    // полная инфо о событии добавленное текущим пользователем
    @Override
    public EventFullDto getEventByInitiator(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие не существует"));
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(event));
    }

    // изменения события добавленного текущим пользователем
    @Override
    public EventFullDto updateEventByInitiator(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не существует " + eventId));
        if (eventToUpdate.getState().equals(EventState.CANCELED) || eventToUpdate.getState().equals(EventState.PENDING)) {
            if (updateEventUserRequest.getEventDate() != null
                    && updateEventUserRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Дата и время на которые намечено событие не может быть раньше, " +
                        "чем через два часа от текущего момента ");
            }
            if (StateActionUser.SEND_TO_REVIEW == updateEventUserRequest.getStateAction()) {
                eventToUpdate.setState(EventState.PENDING);
            }
            if (StateActionUser.CANCEL_REVIEW == updateEventUserRequest.getStateAction()) {
                eventToUpdate.setState(EventState.CANCELED);
            }
        } else {
            throw new AlreadyExistsException("Состояние события должно быть  CANCELED или находится в PENDING" + eventToUpdate.getState());
        }

        updateEventEntity(updateEventUserRequest, eventToUpdate);
        eventRepository.save(eventToUpdate);
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(eventToUpdate));
    }

    // admin
    // поиск событий
    @Override
    public List<EventFullDto> getEventsByAdmin(AdminEventFilter filter) {

        // дефолты
        int from = filter.getFrom() != null ? filter.getFrom() : 0;
        int size = filter.getSize() != null ? filter.getSize() : 10;

        List<Event> events = handleFilters(filter, from, size);

        return events.stream()
                .map(eventMapper::toEventFullDto)
                .map(this::addConfirmedRequestsAndViews)
                .toList();
    }

    private List<Event> handleFilters(AdminEventFilter filter, int from, int size) {

        boolean noFilters =
                filter.getStates() == null &&
                        filter.getRangeStart() == null &&
                        filter.getRangeEnd() == null &&
                        filter.getUsers() == null &&
                        filter.getCategories() == null;

        PageRequest pageRequest = PageRequest.of(from / size, size);

        if (noFilters) {
            return eventRepository.findAll(pageRequest).toList();
        }

        LocalDateTime start = (filter.getRangeStart() != null && !filter.getRangeStart().isEmpty())
                ? LocalDateTime.parse(filter.getRangeStart(), DTF)
                : LocalDateTime.MIN;

        LocalDateTime end = (filter.getRangeEnd() != null && !filter.getRangeEnd().isEmpty())
                ? LocalDateTime.parse(filter.getRangeEnd(), DTF)
                : LocalDateTime.MAX;

        return eventRepository.findEvents(
                filter.getUsers(),
                filter.getStates(),
                filter.getCategories(),
                start,
                end,
                pageRequest
        ).toList();
    }



    // редактирование данных события и его статуса
    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не существует " + eventId));

        if (updateEventAdminRequest.getEventDate() != null) {
            if (updateEventAdminRequest.getEventDate().minusHours(1).isBefore(LocalDateTime.now())) {
                throw new ValidationException("Дата начала события должна быть не ранее чем за час от даты публикации");
            }
        }
        if (updateEventAdminRequest.getStateAction() != null) {
            if (updateEventAdminRequest.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
                if (eventToUpdate.getState().equals(EventState.PENDING)) {
                    eventToUpdate.setState(EventState.PUBLISHED);
                    eventToUpdate.setPublishedOn(LocalDateTime.now());
                } else {
                    throw new AlreadyExistsException("Событие должно находиться в PENDING чтобы его можно было PUBLISHED" +
                            updateEventAdminRequest.getStateAction());
                }
            }
            if (updateEventAdminRequest.getStateAction() == StateActionAdmin.REJECT_EVENT) {
                if (eventToUpdate.getState().equals(EventState.PUBLISHED)) {
                    throw new AlreadyExistsException("Событие должно находиться в PENDING чтобы его можно было отклонить  " +
                            updateEventAdminRequest.getStateAction());
                }
                eventToUpdate.setState(EventState.CANCELED);
            }
        }
        updateEventEntity(updateEventAdminRequest, eventToUpdate);

        eventRepository.save(eventToUpdate);
        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(eventToUpdate));
    }

    // public
    // получение событий с возможностью фильтрации
    @Override
    public List<EventShortDto> getEventList(PublicEventFilter filter) {

        statsClient.hit(filter.getUserIp(), filter.getRequestUri());

        LocalDateTime start;
        LocalDateTime end;

        String startStr = filter.getRangeStart();
        String endStr = filter.getRangeEnd();

        boolean hasStart = startStr != null && !startStr.trim().isEmpty();
        boolean hasEnd = endStr != null && !endStr.trim().isEmpty();

        if (hasStart && hasEnd) {
            start = LocalDateTime.parse(startStr, DTF);
            end = LocalDateTime.parse(endStr, DTF);

            if (start.isAfter(end)) {
                throw new ValidationException("Неправильные даты");
            }

        } else {
            start = hasStart
                    ? LocalDateTime.parse(startStr, DTF)
                    : LocalDateTime.now();

            end = hasEnd
                    ? LocalDateTime.parse(endStr, DTF)
                    : LocalDateTime.now().plusYears(10);
        }

        PageRequest pageRequest = PageRequest.of(filter.getFrom() / filter.getSize(), filter.getSize());

        List<Event> eventList = eventRepository
                .searchPublishedEvents(
                        filter.getText(),
                        filter.getCategories(),
                        filter.getPaid(),
                        start,
                        end,
                        pageRequest)
                .getContent();

        if (eventList.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventShortDto> result = eventList.stream()
                .map(eventMapper::toEventShortDto)
                .map(this::addShortConfirmedRequestsAndViews)
                .collect(Collectors.toList());

        if (filter.getSort() != null) {
            switch (SortValue.valueOf(filter.getSort())) {
                case EVENT_DATE -> result.sort(Comparator.comparing(EventShortDto::getEventDate));
                case VIEWS -> result.sort(Comparator.comparing(EventShortDto::getViews));
                default -> throw new ValidationException("Параметр sort недопустим");
            }
        }

        return result;
    }


    // получение подробной инфо о событии по его id
    @Override
    public EventFullDto getEvent(Long eventId, String userIp, String requestUri) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не существует " + eventId));

        statsClient.hit(userIp, requestUri);

        return addConfirmedRequestsAndViews(eventMapper.toEventFullDto(event));
    }

    private void updateEventEntity(UpdateEventUserRequest event, Event eventToUpdate) {
        eventToUpdate.setAnnotation(Objects.requireNonNullElse(event.getAnnotation(), eventToUpdate.getAnnotation()));
        eventToUpdate.setCategory(event.getCategory() == null
                ? eventToUpdate.getCategory()
                : categoryRepository.findById(event.getCategory()).orElseThrow(() -> new NotFoundException("Категория не найдена")));
        eventToUpdate.setDescription(Objects.requireNonNullElse(event.getDescription(), eventToUpdate.getDescription()));
        eventToUpdate.setEventDate(Objects.requireNonNullElse(event.getEventDate(), eventToUpdate.getEventDate()));
        eventToUpdate.setLocation(event.getLocation() == null
                ? eventToUpdate.getLocation()
                : locationRepository.findByLatAndLon(event.getLocation().getLat(), event.getLocation().getLon())
                .orElse(new Location(null, event.getLocation().getLat(), event.getLocation().getLon())));
        eventToUpdate.setPaid(Objects.requireNonNullElse(event.getPaid(), eventToUpdate.getPaid()));
        eventToUpdate.setParticipantLimit(Objects.requireNonNullElse(event.getParticipantLimit(), eventToUpdate.getParticipantLimit()));
        eventToUpdate.setRequestModeration(Objects.requireNonNullElse(event.getRequestModeration(), eventToUpdate.getRequestModeration()));
        eventToUpdate.setTitle(Objects.requireNonNullElse(event.getTitle(), eventToUpdate.getTitle()));
    }

    private void updateEventEntity(UpdateEventAdminRequest event, Event eventToUpdate) {
        eventToUpdate.setAnnotation(Objects.requireNonNullElse(event.getAnnotation(), eventToUpdate.getAnnotation()));
        eventToUpdate.setCategory(event.getCategory() == null
                ? eventToUpdate.getCategory()
                : categoryRepository.findById(event.getCategory()).orElseThrow(() -> new NotFoundException("Категория не найдена")));
        eventToUpdate.setDescription(Objects.requireNonNullElse(event.getDescription(), eventToUpdate.getDescription()));
        eventToUpdate.setEventDate(Objects.requireNonNullElse(event.getEventDate(), eventToUpdate.getEventDate()));
        eventToUpdate.setLocation(event.getLocation() == null
                ? eventToUpdate.getLocation()
                : locationRepository.findByLatAndLon(event.getLocation().getLat(), event.getLocation().getLon())
                .orElse(new Location(null, event.getLocation().getLat(), event.getLocation().getLon())));
        eventToUpdate.setPaid(Objects.requireNonNullElse(event.getPaid(), eventToUpdate.getPaid()));
        eventToUpdate.setParticipantLimit(Objects.requireNonNullElse(event.getParticipantLimit(), eventToUpdate.getParticipantLimit()));
        eventToUpdate.setRequestModeration(Objects.requireNonNullElse(event.getRequestModeration(), eventToUpdate.getRequestModeration()));
        eventToUpdate.setTitle(Objects.requireNonNullElse(event.getTitle(), eventToUpdate.getTitle()));
    }

    private EventFullDto addConfirmedRequestsAndViews(EventFullDto eventFullDto) {

        // 1. Подтверждённые запросы
        eventFullDto.setConfirmedRequests(
                requestRepository.countByEventIdAndStatus(eventFullDto.getId(), RequestStatus.CONFIRMED));

        // 2. Просмотры — с даты публикации события
        LocalDateTime start = eventFullDto.getPublishedOn() != null
                ? eventFullDto.getPublishedOn()
                : LocalDateTime.now().minusYears(100); // fallback, если дата публикации отсутствует

        List<String> uris = List.of("/events/" + eventFullDto.getId());

        ViewStatsRequest viewStatsRequest = new ViewStatsRequest(
                start,
                LocalDateTime.now(),
                uris,
                true
        );

        List<ViewStats> viewStatsList = statsClient.getStats(viewStatsRequest);

        eventFullDto.setViews(viewStatsList.isEmpty()
                ? 0L
                : viewStatsList.getFirst().getHits());

        return eventFullDto;
    }


    private EventShortDto addShortConfirmedRequestsAndViews(EventShortDto eventShortDto) {
        // Добавить сonfirmedRequests к каждому событию
        eventShortDto.setConfirmedRequests(
                requestRepository.countByEventIdAndStatus(eventShortDto.getId(), RequestStatus.CONFIRMED));

        // Добавить views к каждому событию
        List<String> uris = new ArrayList<>();

        // создаем uri для обращения к базе данных статистики
        uris.add("/events/" + eventShortDto.getId());
        ViewStatsRequest viewStatsRequest = new ViewStatsRequest(
                LocalDateTime.now().minusYears(100),
                LocalDateTime.now(),
                uris,
                true);
        List<ViewStats> viewStatsList = statsClient.getStats(viewStatsRequest);
        if (viewStatsList.isEmpty()) {
            log.info("addShortConfirmedRequestsAndViews: просмотров нет у этого эндпоинта " +
                    viewStatsRequest.getUris());
            eventShortDto.setViews(0L);
        } else {
            eventShortDto.setViews(viewStatsList.getFirst().getHits());
        }
        return eventShortDto;
    }

    // private
    // Получение инфо о запросах на участие в событии текущего пользователя
    @Override
    public List<ParticipationRequestDto> getRequestsByCurrentUserOfCurrentEvent(Long userId, Long eventId) {

        // проверяем пользователя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не существует: " + userId));

        // проверяем событие
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не существует: " + eventId));

        // проверяем — является ли user инициатором
        if (!event.getInitiator().getId().equals(user.getId())) {
            throw new ValidationException("Пользователь " + userId + " не является инициатором события " + eventId);
        }

        // получаем запросы
        List<Request> requestList = requestRepository
                .findAllByEvent_InitiatorIdAndEvent_Id(userId, eventId);

        return requestList.stream()
                .map(requestMapper::toParticipationRequestDto)
                .toList();
    }

    // Изменение статуса (подтверждена, отменена) заявок на участие в событии текущего пользователя
    @Override
    public EventRequestStatusUpdateResult updateRequest(Long userId, Long eventId,
                                                        EventRequestStatusUpdateRequest eventRequest) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не существует " + userId);
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не существует " + eventId));

        if (event.getParticipantLimit() == 0 && !event.getRequestModeration()) {
            throw new ValidationException("Модерация не требуется " + eventId);
        }

        Long confirmedRequest = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        if (confirmedRequest >= event.getParticipantLimit()) {
            throw new AlreadyExistsException("Превышение лимита участия " + eventId);
        }

        // получаем список всех запросов статус которых нужно обновить
        List<Long> requestIdList = eventRequest.getRequestIds();
        // получаем статус события, который нужно проставить у всех событий
        RequestStatusUpdate status = eventRequest.getStatus();

        List<Request> requestList = requestRepository.findAllByIdIn(requestIdList);
        if (requestList.isEmpty()) {
            throw new NotFoundException("Запросов не существует ");
        }

        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();

        List<Request> updatedRequests = new ArrayList<>();

        // перебираем все запросы
        for (Request currentRequest : requestList) {
            if (status == RequestStatusUpdate.CONFIRMED && currentRequest.getStatus().equals(RequestStatus.PENDING)) {
                if (currentRequest.getStatus().equals(RequestStatus.CONFIRMED)) {
                    throw new AlreadyExistsException("Запрос уже был подтвержден");
                }
                if (confirmedRequest >= event.getParticipantLimit()) {
                    // всем отказываем когда превышен лимит
                    currentRequest.setStatus(RequestStatus.REJECTED);
                    updatedRequests.add(currentRequest);
                    rejectedRequests.add(currentRequest);
                }
                currentRequest.setStatus(RequestStatus.CONFIRMED);
                updatedRequests.add(currentRequest);
                confirmedRequest++;
                confirmedRequests.add(currentRequest);
            }
            if (status == RequestStatusUpdate.REJECTED && currentRequest.getStatus().equals(RequestStatus.PENDING)) {
                // отказываем когда событие отменилось
                currentRequest.setStatus(RequestStatus.REJECTED);
                updatedRequests.add(currentRequest);
                rejectedRequests.add(currentRequest);
            }
        }

        // сохранили все запросы с новыми статусами в БД
        requestRepository.saveAll(updatedRequests);
        eventRepository.save(event);

        // переводим в ДТО и на выход
        List<ParticipationRequestDto> confirmedRequestsDto =
                confirmedRequests.stream().map(requestMapper::toParticipationRequestDto).collect(Collectors.toList());
        List<ParticipationRequestDto> rejectedRequestsDto =
                rejectedRequests.stream().map(requestMapper::toParticipationRequestDto).collect(Collectors.toList());

        EventRequestStatusUpdateResult updateResult = new EventRequestStatusUpdateResult();
        updateResult.setConfirmedRequests(confirmedRequestsDto);
        updateResult.setRejectedRequests(rejectedRequestsDto);


        return updateResult;
    }


}