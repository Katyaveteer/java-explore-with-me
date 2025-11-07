package ru.practicum.ewm.service.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.enums.EventState;
import ru.practicum.ewm.enums.RequestStatus;
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
    private RequestStatus RequestStatusUpdate;


    @Override
    public List<ParticipationRequestDto> getRequestsByCurrentUserOfCurrentEvent(Long userId, Long eventId) {
        List<ParticipationRequestDto> participationRequestDtoList = new ArrayList<>();

        if (!userRepository.existsById(userId) && !eventRepository.existsById(eventId)) {
            return participationRequestDtoList;
        }

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User does not exist " + userId);
        }
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event does not exist " + eventId);
        }

        List<Request> requestList;
        if (userId.equals(eventRepository.findById(eventId).get().getInitiator().getId())) {
            requestList = requestRepository.findAllByEvent_InitiatorIdAndEvent_Id(userId, eventId);
        } else {
            throw new ValidationException("User with such id is initiator of event with id" + userId + eventId);
        }

        for (Request request : requestList) {
            participationRequestDtoList.add(requestMapper.toParticipationRequestDto(request));
        }
        return participationRequestDtoList;
    }


    @Override
    public EventRequestStatusUpdateResult updateRequest(Long userId, Long eventId,
                                                        EventRequestStatusUpdateRequest eventRequest) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User does not exist " + userId);
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event does not exist " + eventId));

        if (event.getParticipantLimit() == 0 && !event.getRequestModeration()) {
            throw new ValidationException("Moderation is not required " + eventId);
        }

        Long confirmedRequest = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        if (confirmedRequest >= event.getParticipantLimit()) {
            throw new AlreadyExistsException("Participation limit exceed " + eventId);
        }


        List<Long> requestIdList = eventRequest.getRequestIds();

        RequestStatus status = eventRequest.getStatus();

        List<Request> requestList = requestRepository.findAllByIdIn(requestIdList);
        if (requestList.isEmpty()) {
            throw new NotFoundException("Requests does not exist ");
        }

        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();

        List<Request> updatedRequests = new ArrayList<>();


        for (Request currentRequest : requestList) {
            if (status == RequestStatus.CONFIRMED && currentRequest.getStatus().equals(RequestStatus.PENDING)) {
                if (confirmedRequest >= event.getParticipantLimit()) {

                    currentRequest.setStatus(RequestStatus.REJECTED);
                    updatedRequests.add(currentRequest);
                    rejectedRequests.add(currentRequest);
                }
                currentRequest.setStatus(RequestStatus.CONFIRMED);
                updatedRequests.add(currentRequest);
                confirmedRequest++;
                confirmedRequests.add(currentRequest);
            }
            if (status == RequestStatus.REJECTED && currentRequest.getStatus().equals(RequestStatus.PENDING)) {

                currentRequest.setStatus(RequestStatus.REJECTED);
                updatedRequests.add(currentRequest);
                rejectedRequests.add(currentRequest);
            }
        }


        requestRepository.saveAll(updatedRequests);
        eventRepository.save(event);


        List<ParticipationRequestDto> confirmedRequestsDto =
                confirmedRequests.stream().map(requestMapper::toParticipationRequestDto).collect(Collectors.toList());
        List<ParticipationRequestDto> rejectedRequestsDto =
                rejectedRequests.stream().map(requestMapper::toParticipationRequestDto).collect(Collectors.toList());

        EventRequestStatusUpdateResult updateResult = new EventRequestStatusUpdateResult();
        updateResult.setConfirmedRequests(confirmedRequestsDto);
        updateResult.setRejectedRequests(rejectedRequestsDto);


        return updateResult;
    }


    @Override
    public List<ParticipationRequestDto> getRequestsByCurrentUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User does not exist");
        }
        List<Request> requestList = requestRepository.findAllByRequesterIdAndNotInitiator(userId);
        List<ParticipationRequestDto> participationRequestDtoList = new ArrayList<>();
        for (Request request : requestList) {
            participationRequestDtoList.add(requestMapper.toParticipationRequestDto(request));
        }
        return participationRequestDtoList;
    }


    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {


        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User does not exist " + userId));


        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event does not exist " + eventId));


        Request request = new Request(LocalDateTime.now(), event, requester, RequestStatus.PENDING);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new AlreadyExistsException("Request already exist: userId {}, eventId {} " + userId + eventId);
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new AlreadyExistsException("Initiator could not be requester " + userId);
        }
        if (!(event.getState().equals(EventState.PUBLISHED))) {
            throw new AlreadyExistsException("Event has not published yet");
        }

        Long confirmedRequest = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Long limit = Long.valueOf(event.getParticipantLimit());


        if (limit != 0) {
            if (limit.equals(confirmedRequest)) {
                throw new AlreadyExistsException("Max confirmed requests was reached: " + limit);
            }
        } else {
            request.setStatus(RequestStatus.CONFIRMED);
        }


        if (!event.getRequestModeration()) {
            request.setStatus(RequestStatus.CONFIRMED);
        }

        requestRepository.save(request);
        return requestMapper.toParticipationRequestDto(request);
    }


    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request with id and/or requester id does not exist" + requestId + userId));
        request.setStatus(RequestStatus.CANCELED);
        requestRepository.save(request);
        return requestMapper.toParticipationRequestDto(request);
    }
}
