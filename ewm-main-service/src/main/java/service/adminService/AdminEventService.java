package service.adminService;

import dto.event.EventFullDto;
import dto.event.UpdateEventAdminRequest;
import enums.EventState;
import exception.ConflictException;
import exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import model.Category;
import model.Event;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import repository.EventRepository;
import service.publicService.PublicEventService;
import utils.DateTimeUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;
    private final PublicEventService publicEventService;

    public List<EventFullDto> getEvents(List<Long> users, List<String> states, List<Long> categories,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        List<EventState> eventStates = (states == null) ? null :
                states.stream().map(EventState::valueOf).collect(Collectors.toList());
        PageRequest page = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findWithFilters(users, eventStates, categories, rangeStart, rangeEnd, page);
        return events.stream()
                .map(e -> publicEventService.toFullDto(e, 0L, 0L))
                .collect(Collectors.toList());
    }

    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (dto.getEventDate() != null) {
            LocalDateTime newDate = DateTimeUtils.parse(dto.getEventDate());
            event.setEventDate(newDate);
        }

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

        if ("PUBLISH_EVENT".equals(dto.getStateAction())) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if ("REJECT_EVENT".equals(dto.getStateAction())) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject published event");
            }
            event.setState(EventState.CANCELED);
        }

        event = eventRepository.save(event);
        return publicEventService.toFullDto(event, 0L, 0L);
    }
}
