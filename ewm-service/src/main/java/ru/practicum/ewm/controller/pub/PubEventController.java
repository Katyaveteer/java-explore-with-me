package ru.practicum.ewm.controller.pub;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.PublicEventFilter;
import ru.practicum.ewm.service.event.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Slf4j
public class PubEventController {
    private final EventService eventService;

    // получение событий с возможностью фильтрации
    @GetMapping
    public List<EventShortDto> getEventList(@ModelAttribute PublicEventFilter filter,
                                            HttpServletRequest request) {

        filter.setUserIp(request.getRemoteAddr());
        filter.setRequestUri(request.getRequestURI());

        log.info("PubEventController / getEventList: {}", filter);

        return eventService.getEventList(filter);
    }


    // получение подробной инфо о событии по его id
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        log.info("PubEventController / getEvent: получение подробной инфо о событии по его id " +
                id + request.getRemoteAddr() + request.getRequestURI());
        return eventService.getEvent(id, request.getRemoteAddr(), request.getRequestURI());
    }
}