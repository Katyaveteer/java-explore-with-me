package ru.practicum.ewm.service.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.enums.EventState;
import ru.practicum.ewm.enums.RequestStatus;
import ru.practicum.ewm.enums.RequestStatusUpdate;
import ru.practicum.ewm.exception.AlreadyExistsException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // private
    // Получение инфо о запросах на участие в событии текущего пользователя
    @Override
    public List<ParticipationRequestDto> getRequestsByCurrentUserOfCurrentEvent(Long userId, Long eventId) {
        List<ParticipationRequestDto> participationRequestDtoList = new ArrayList<>();

        if (!userRepository.existsById(userId) && !eventRepository.existsById(eventId)) {
            return participationRequestDtoList;
        }

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь не существует " + userId);
        }
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие не существует " + eventId);
        }

        List<Request> requestList;
        if (userId.equals(eventRepository.findById(eventId).get().getInitiator().getId())) {
            requestList = requestRepository.findAllByEvent_InitiatorIdAndEvent_Id(userId, eventId);
        } else {
            throw new ValidationException("Пользователь с таким id является инициатором события с id" + userId + eventId);
        }

        for (Request request : requestList) {
            participationRequestDtoList.add(requestMapper.toParticipationRequestDto(request));
        }
        return participationRequestDtoList;
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