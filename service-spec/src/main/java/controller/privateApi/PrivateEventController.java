package controller.privateApi;

import dto.event.EventFullDto;
import dto.event.NewEventDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import service.privateService.PrivateEventService;

public class PrivateEventController {

    private PrivateEventService privateEventService;

    @PostMapping
    public EventFullDto addEvent(@PathVariable Long userId,
                                 @Valid @RequestBody NewEventDto dto) {
        return privateEventService.createEvent(userId, dto);
    }
}
