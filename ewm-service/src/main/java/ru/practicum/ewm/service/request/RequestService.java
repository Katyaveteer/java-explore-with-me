package ru.practicum.ewm.service.request;

import ru.practicum.ewm.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {


    // private: requests
    // Получение инфо о заявках текущего пользователя на участие в чужих событиях
    List<ParticipationRequestDto> getRequestsByCurrentUser(Long userId);

    // Добавление запроса от текущего пользователя на участие в событии
    ParticipationRequestDto addRequest(Long userId, Long eventId);

    // Отмена своего запроса на участие в событии
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);


}
