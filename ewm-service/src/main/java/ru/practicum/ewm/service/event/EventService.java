package ru.practicum.ewm.service.event;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    // private
    // получение событий текущего пользователя
    List<EventShortDto> getEventsByInitiator(Long userId, Pageable pageable);

    // добавление нового события
    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    // полная инфо о событии добавленное текущим пользователем
    EventFullDto getEventByInitiator(Long userId, Long eventId);

    // изменения события добавленного текущим пользователем
    EventFullDto updateEventByInitiator(Long userId,
                                        Long eventId,
                                        UpdateEventUserRequest updateEventUserRequest);

    // admin
    // поиск событий
    List<EventFullDto> getEventsByAdmin(AdminEventFilter filter);

    // редактирование данных события и его статуса
    EventFullDto updateEventByAdmin(Long eventId,
                                    UpdateEventAdminRequest updateEventAdminRequest);

    // public
    // получение событий с возможностью фильтрации
    List<EventShortDto> getEventList(PublicEventFilter filter);

    // получение подробной инфо о событии по его id
    EventFullDto getEvent(Long eventId, String userIp, String requestUri);

    // private: events
    // Получение инфо о запросах на участие в событии текущего пользователя
    List<ParticipationRequestDto> getRequestsByCurrentUserOfCurrentEvent(Long userId, Long eventId);

    // Изменение статуса (подтверждена, отменена) заявок на участие в событии текущего пользователя
    EventRequestStatusUpdateResult updateRequest(Long userId,
                                                 Long eventId,
                                                 EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest);

}