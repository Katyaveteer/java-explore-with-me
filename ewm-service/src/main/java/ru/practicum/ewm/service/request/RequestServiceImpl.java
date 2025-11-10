package ru.practicum.ewm.service.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.enums.EventState;
import ru.practicum.ewm.enums.RequestStatus;
import ru.practicum.ewm.exception.AlreadyExistsException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Request;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;


    // Получение инфо о заявках текущего пользователя на участие в чужих событиях
    @Override
    public List<ParticipationRequestDto> getRequestsByCurrentUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не существует");
        }
        List<Request> requestList = requestRepository.findAllByRequesterIdAndNotInitiator(userId);
        List<ParticipationRequestDto> participationRequestDtoList = new ArrayList<>();
        for (Request request : requestList) {
            participationRequestDtoList.add(requestMapper.toParticipationRequestDto(request));
        }
        return participationRequestDtoList;
    }

    // Добавление запроса от текущего пользователя на участие в событии
    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {

        // выгружаем данные пользователя, кто отправиляет запрос
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не существует " + userId));

        // выгружаем данные события
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не существует " + eventId));

        // создаем запрос
        Request request = new Request(LocalDateTime.now(), event, requester, RequestStatus.PENDING);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new AlreadyExistsException("Запрос уже существует: userId {}, eventId {} " + userId + eventId);
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new AlreadyExistsException("Инициатор не мог быть запрашивающим лицом " + userId);
        }
        if (!(event.getState().equals(EventState.PUBLISHED))) {
            throw new AlreadyExistsException("Событие еще не опубликовано");
        }

        Long confirmedRequest = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Long limit = event.getParticipantLimit();

        // если есть ограничение, то проверяем. Если ограничения нет, то автоматически подтверждаем запрос
        if (limit != 0) {
            if (limit.equals(confirmedRequest)) {
                throw new AlreadyExistsException("Получено максимальное количество подтвержденных запросов: " + limit);
            }
        } else {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        // если модерация не нужна, то автоматом подтверждаем запрос и увеличиваем счетчик
        if (!event.getRequestModeration()) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        requestRepository.save(request);
        return requestMapper.toParticipationRequestDto(request);
    }

    // Отмена своего запроса на участие в событии
    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с идентификатором и/или идентификатором отправителя запроса не существует" + requestId + userId));
        request.setStatus(RequestStatus.CANCELED);
        requestRepository.save(request);
        return requestMapper.toParticipationRequestDto(request);
    }
}