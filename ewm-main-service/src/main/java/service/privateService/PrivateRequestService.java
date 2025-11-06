package service.privateService;

import dto.request.EventRequestStatusUpdateRequest;
import dto.request.EventRequestStatusUpdateResult;
import dto.request.ParticipationRequestDto;
import enums.EventState;
import enums.RequestStatus;
import exception.ConflictException;
import exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import model.Event;
import model.ParticipationRequest;
import model.User;
import org.springframework.stereotype.Service;
import repository.EventRepository;
import repository.RequestRepository;
import repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivateRequestService {

    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        return requestRepository.findByRequesterId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot request to own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot request to unpublished event");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists");
        }
        if (event.getParticipantLimit() > 0) {
            long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmed >= event.getParticipantLimit()) {
                throw new ConflictException("Participant limit reached");
            }
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(user)
                .status(event.getRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED)
                .created(LocalDateTime.now())
                .build();

        request = requestRepository.save(request);
        return toDto(request);
    }

    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Request not found");
        }
        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);
        return toDto(request);
    }

    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event not found");
        }
        return requestRepository.findByEventId(eventId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest update) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event not found");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(update.getRequestIds());
        List<ParticipationRequest> confirmed = new java.util.ArrayList<>();
        List<ParticipationRequest> rejected = new java.util.ArrayList<>();

        for (ParticipationRequest r : requests) {
            if (r.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }

            if ("CONFIRMED".equals(update.getStatus())) {
                if (event.getParticipantLimit() > 0) {
                    long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
                    if (confirmedCount >= event.getParticipantLimit()) {
                        throw new ConflictException("The participant limit has been reached");
                    }
                }
                r.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(r);
            } else {
                r.setStatus(RequestStatus.REJECTED);
                rejected.add(r);
            }
        }

        // Если лимит достигнут — отклоняем все остальные PENDING
        if (event.getParticipantLimit() > 0) {
            long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedCount >= event.getParticipantLimit()) {
                List<ParticipationRequest> pending = requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
                for (ParticipationRequest p : pending) {
                    p.setStatus(RequestStatus.REJECTED);
                    rejected.add(p);
                }
                requestRepository.saveAll(pending);
            }
        }

        requestRepository.saveAll(requests);
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed.stream().map(this::toDto).collect(Collectors.toList()))
                .rejectedRequests(rejected.stream().map(this::toDto).collect(Collectors.toList()))
                .build();
    }

    private ParticipationRequestDto toDto(ParticipationRequest r) {
        return ParticipationRequestDto.builder()
                .id(r.getId())
                .event(r.getEvent().getId())
                .requester(r.getRequester().getId())
                .created(r.getCreated().toString())
                .status(r.getStatus().name())
                .build();
    }
}
