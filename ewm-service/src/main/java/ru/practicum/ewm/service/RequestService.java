package ru.practicum.ewm.service;


import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.enums.EventState;
import ru.practicum.ewm.enums.RequestStatus;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestService {
    private final RequestRepository requestRepository;
    private final EventService eventService;
    private final UserService userService;
    private final RequestMapper requestMapper;
    private final EventRepository eventRepository;

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userService.getUserById(userId);
        return requestRepository.findByRequesterId(userId).stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User user = userService.getUserById(userId);
        Event event = eventService.getUserEvent(userId, eventId).getInitiator().getId().equals(userId) ?
                null : eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event == null || event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot add request to his own event");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        if (requestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ConflictException("Request already exists");
        }

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        ParticipationRequest savedRequest = requestRepository.save(request);
        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        userService.getUserById(userId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Request with id=" + requestId + " was not found");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);
        return requestMapper.toParticipationRequestDto(updatedRequest);
    }

    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        userService.getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        return requestRepository.findByEventId(event.getId()).stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        userService.getUserById(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        List<ParticipationRequest> requests = requestRepository.findByIdIn(updateRequest.getRequestIds());

        if (requests.stream().anyMatch(r -> r.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Request must have status PENDING");
        }

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit() &&
                updateRequest.getStatus() == RequestStatus.CONFIRMED) {
            throw new ConflictException("The participant limit has been reached");
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                if (event.getParticipantLimit() == 0 || confirmedRequests < event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests++;
                    confirmed.add(requestMapper.toParticipationRequestDto(request));
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(requestMapper.toParticipationRequestDto(request));
                }
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(requestMapper.toParticipationRequestDto(request));
            }
        }

        requestRepository.saveAll(requests);

        // Отклонение всех оставшихся заявок при достижении лимита
        if (updateRequest.getStatus() == RequestStatus.CONFIRMED &&
                event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            List<ParticipationRequest> pendingRequests = requestRepository
                    .findByEventIdAndStatus(eventId, RequestStatus.PENDING);
            for (ParticipationRequest pendingRequest : pendingRequests) {
                pendingRequest.setStatus(RequestStatus.REJECTED);
                rejected.add(requestMapper.toParticipationRequestDto(pendingRequest));
            }
            requestRepository.saveAll(pendingRequests);
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }
}
