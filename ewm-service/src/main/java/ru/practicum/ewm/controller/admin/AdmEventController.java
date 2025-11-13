package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.AdminEventFilter;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.service.event.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/events")
@RequiredArgsConstructor
@Slf4j
public class AdmEventController {
    private final EventService eventService;

    // Поиск событий
    @GetMapping
    public List<EventFullDto> getEventListByAdmin(@ModelAttribute AdminEventFilter filter) {
        log.info("AdmEventController / getEventListByAdmin: Поиск событий {}", filter);
        return eventService.getEventsByAdmin(filter);
    }


    // редактирование данных события и его статуса
    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long eventId,
                                    @Valid @RequestBody UpdateEventAdminRequest updateEventAdminRequest) {
        log.info("AdmEventController / updateEvent: редактирование данных события и его статуса " + updateEventAdminRequest);
        return eventService.updateEventByAdmin(eventId, updateEventAdminRequest);
    }
}