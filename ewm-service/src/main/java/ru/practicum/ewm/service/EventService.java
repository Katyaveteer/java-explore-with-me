package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.enums.EventState;
import ru.practicum.ewm.enums.StateActionAdmin;
import ru.practicum.ewm.enums.StateActionUser;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.statsclient.StatsClient;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        userService.getUserById(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        return events.stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. " +
                    "Value: " + newEventDto.getEventDate());
        }

        User user = userService.getUserById(userId);
        Category category = categoryService.getCategoryEntity(newEventDto.getCategory());

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(savedEvent);
    }

    public EventFullDto getUserEvent(Long userId, Long eventId) {
        userService.getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return eventMapper.toEventFullDto(event);
    }

    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        userService.getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Field: eventDate. Error: должно содержать дату, которая еще не наступила.");
        }

        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() == StateActionUser.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (updateRequest.getStateAction() == StateActionUser.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }

        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<EventState> eventStates = null;

        if (states != null) {
            eventStates = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
        }

        List<Event> events = eventRepository.findEventsByAdmin(users, eventStates, categories,
                rangeStart, rangeEnd, pageable);

        return events.stream()
                .map(eventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Cannot publish the event because it's not in the right state");
        }

        if (updateRequest.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: " +
                        event.getState());
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (updateRequest.getStateAction() == StateActionAdmin.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject the event because it's not in the right state: " +
                        event.getState());
            }
            event.setState(EventState.CANCELED);
        }

        updateEventFields(event, updateRequest);
        Event updatedEvent = eventRepository.save(event);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Integer from, Integer size,
                                               HttpServletRequest request) {
        Pageable pageable;
        if ("VIEWS".equals(sort)) {
            pageable = PageRequest.of(from / size, size, Sort.by("views").descending());
        } else {
            pageable = PageRequest.of(from / size, size, Sort.by("eventDate").descending());
        }

        if (rangeStart == null && rangeEnd == null) {
            rangeStart = LocalDateTime.now();
        }

        List<Event> events = eventRepository.findEventsPublic(text, categories, paid, rangeStart,
                rangeEnd, onlyAvailable, pageable);

        // Сохранение статистики
        statsClient.hit("/events", request.getRemoteAddr());

        return events.stream()
                .map(eventMapper::toEventShortDto)
                .collect(Collectors.toList());
    }

    public EventFullDto getEventPublic(Long id, HttpServletRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + id + " was not found");
        }

        // Сохранение статистики
        statsClient.hit("/events/" + id, request.getRemoteAddr());

        // Получение количества просмотров из сервиса статистики
        Long views = statsClient.getViews(event.getId());
        event.setViews(views);

        return eventMapper.toEventFullDto(event);
    }

    private void updateEventFields(Event event, Object updateRequest) {
        if (updateRequest instanceof UpdateEventUserRequest) {
            UpdateEventUserRequest userRequest = (UpdateEventUserRequest) updateRequest;
            updateCommonFields(event, userRequest);
        } else if (updateRequest instanceof UpdateEventAdminRequest) {
            UpdateEventAdminRequest adminRequest = (UpdateEventAdminRequest) updateRequest;
            updateCommonFields(event, adminRequest);
        }
    }

    private void updateCommonFields(Event event, UpdateEventUserRequest userRequest) {
        if (userRequest.getAnnotation() != null) {
            event.setAnnotation(userRequest.getAnnotation());
        }
        if (userRequest.getCategory() != null) {
            Category category = categoryService.getCategoryEntity(userRequest.getCategory());
            event.setCategory(category);
        }
        if (userRequest.getDescription() != null) {
            event.setDescription(userRequest.getDescription());
        }
        if (userRequest.getEventDate() != null) {
            event.setEventDate(userRequest.getEventDate());
        }
        if (userRequest.getLocation() != null) {
            Location location = new Location();
            location.setLat(userRequest.getLocation().getLat());
            location.setLon(userRequest.getLocation().getLon());
            event.setLocation(location);
        }
        if (userRequest.getPaid() != null) {
            event.setPaid(userRequest.getPaid());
        }
        if (userRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(userRequest.getParticipantLimit());
        }
        if (userRequest.getRequestModeration() != null) {
            event.setRequestModeration(userRequest.getRequestModeration());
        }
        if (userRequest.getTitle() != null) {
            event.setTitle(userRequest.getTitle());
        }
    }

    private void updateCommonFields(Event event, UpdateEventAdminRequest adminRequest) {
        if (adminRequest.getAnnotation() != null) {
            event.setAnnotation(adminRequest.getAnnotation());
        }
        if (adminRequest.getCategory() != null) {
            Category category = categoryService.getCategoryEntity(adminRequest.getCategory());
            event.setCategory(category);
        }
        if (adminRequest.getDescription() != null) {
            event.setDescription(adminRequest.getDescription());
        }
        if (adminRequest.getEventDate() != null) {
            event.setEventDate(adminRequest.getEventDate());
        }
        if (adminRequest.getLocation() != null) {
            Location location = new Location();
            location.setLat(adminRequest.getLocation().getLat());
            location.setLon(adminRequest.getLocation().getLon());
            event.setLocation(location);
        }
        if (adminRequest.getPaid() != null) {
            event.setPaid(adminRequest.getPaid());
        }
        if (adminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(adminRequest.getParticipantLimit());
        }
        if (adminRequest.getRequestModeration() != null) {
            event.setRequestModeration(adminRequest.getRequestModeration());
        }
        if (adminRequest.getTitle() != null) {
            event.setTitle(adminRequest.getTitle());
        }
    }
}