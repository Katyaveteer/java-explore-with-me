package ru.practicum.ewm.mapper;


import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.LocationDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EventMapper {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    @Autowired
    public EventMapper(UserMapper userMapper, CategoryMapper categoryMapper) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
    }

    public EventFullDto toEventFullDto(Event event) {
        if (event == null) {
            return null;
        }

        EventFullDto eventFullDto = new EventFullDto();
        eventFullDto.setId(event.getId());
        eventFullDto.setAnnotation(event.getAnnotation());
        eventFullDto.setCategory(categoryMapper.toCategoryDto(event.getCategory()));
        eventFullDto.setConfirmedRequests(event.getConfirmedRequests() != null ? event.getConfirmedRequests() : 0L);
        eventFullDto.setCreatedOn(event.getCreatedOn());
        eventFullDto.setDescription(event.getDescription());
        eventFullDto.setEventDate(event.getEventDate());
        eventFullDto.setInitiator(userMapper.toUserShortDto(event.getInitiator()));

        if (event.getLocation() != null) {
            LocationDto locationDto = new LocationDto();
            locationDto.setLat(event.getLocation().getLat());
            locationDto.setLon(event.getLocation().getLon());
            eventFullDto.setLocation(locationDto);
        }

        eventFullDto.setPaid(event.getPaid());
        eventFullDto.setParticipantLimit(event.getParticipantLimit() != null ? event.getParticipantLimit() : 0);
        eventFullDto.setPublishedOn(event.getPublishedOn());
        eventFullDto.setRequestModeration(event.getRequestModeration() != null ? event.getRequestModeration() : true);
        eventFullDto.setState(event.getState());
        eventFullDto.setTitle(event.getTitle());
        eventFullDto.setViews(event.getViews() != null ? event.getViews() : 0L);

        return eventFullDto;
    }

    public EventShortDto toEventShortDto(Event event) {
        if (event == null) {
            return null;
        }

        EventShortDto eventShortDto = new EventShortDto();
        eventShortDto.setId(event.getId());
        eventShortDto.setAnnotation(event.getAnnotation());
        eventShortDto.setCategory(categoryMapper.toCategoryDto(event.getCategory()));
        eventShortDto.setConfirmedRequests(event.getConfirmedRequests() != null ? event.getConfirmedRequests() : 0L);
        eventShortDto.setEventDate(event.getEventDate());
        eventShortDto.setInitiator(userMapper.toUserShortDto(event.getInitiator()));
        eventShortDto.setPaid(event.getPaid());
        eventShortDto.setTitle(event.getTitle());
        eventShortDto.setViews(event.getViews() != null ? event.getViews() : 0L);

        return eventShortDto;
    }

    public Event toEvent(NewEventDto newEventDto) {
        if (newEventDto == null) {
            return null;
        }

        Event event = new Event();
        event.setAnnotation(newEventDto.getAnnotation());
        event.setDescription(newEventDto.getDescription());
        event.setEventDate(newEventDto.getEventDate());

        if (newEventDto.getLocation() != null) {
            Location location = new Location();
            location.setLat(newEventDto.getLocation().getLat());
            location.setLon(newEventDto.getLocation().getLon());
            event.setLocation(location);
        }

        event.setPaid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false);
        event.setParticipantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0);
        event.setRequestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true);
        event.setTitle(newEventDto.getTitle());
        event.setCreatedOn(LocalDateTime.now());
        event.setConfirmedRequests(0);

        return event;
    }
}
