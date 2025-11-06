package service.privateService;

import dto.event.EventFullDto;
import dto.event.EventShortDto;
import dto.event.NewEventDto;
import dto.event.UpdateEventUserRequest;
import enums.EventState;
import exception.ConflictException;
import exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import model.Category;
import model.Event;
import model.Location;
import model.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import repository.EventRepository;
import repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivateEventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        PageRequest page = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, page);
        return events.stream().map(this::toShortDto).collect(Collectors.toList());
    }

    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate().replace(" ", "T"));
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + dto.getEventDate());
        }

        Event event = Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .location(Location.builder()
                        .lat(dto.getLocation().getLat())
                        .lon(dto.getLocation().getLon())
                        .build())

                .eventDate(eventDate)
                .paid(dto.getPaid())
                .participantLimit(dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration())
                .state(EventState.PENDING)
                .initiator(user)
                .category(Category.builder().id(dto.getCategory()).build())
                .build();

        event = eventRepository.save(event);
        return toFullDto(event);
    }

    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event not found");
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (dto.getEventDate() != null) {
            LocalDateTime newDate = LocalDateTime.parse(dto.getEventDate().replace(" ", "T"));
            if (newDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ConflictException("Event date must be at least 2 hours in the future");
            }
            event.setEventDate(newDate);
        }

        // Обновляем остальные поля, если не null
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getLocation() != null) {
            event.getLocation().setLat(dto.getLocation().getLat());
            event.getLocation().setLon(dto.getLocation().getLon());
        }
        if (dto.getCategory() != null) {
            event.setCategory(Category.builder().id(dto.getCategory()).build());
        }

        if ("SEND_TO_REVIEW".equals(dto.getStateAction())) {
            event.setState(EventState.PENDING);
        } else if ("CANCEL_REVIEW".equals(dto.getStateAction())) {
            event.setState(EventState.CANCELED);
        }

        event = eventRepository.save(event);
        return toFullDto(event);
    }

    // Маппинг — упрощённый
    private EventShortDto toShortDto(Event e) { /* ... */
        return null;
    }

    private EventFullDto toFullDto(Event e) { /* ... */
        return null;
    }
}
