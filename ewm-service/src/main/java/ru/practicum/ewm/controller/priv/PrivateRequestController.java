package ru.practicum.ewm.controller.priv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.service.request.RequestService;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/requests")
@RequiredArgsConstructor
@Slf4j
public class PrivateRequestController {
    private final RequestService requestService;

    // Получение инфо о заявках текущего пользователя на участие в чужих событиях
    @GetMapping
    public List<ParticipationRequestDto> getRequestsByCurrentUser(@PathVariable Long userId) {
        log.info("PrivateRequestController / getRequestsByCurrentUser: " +
                "Получение инфо о заявках текущего пользователя на участие в чужих событиях " +
                userId);
        return requestService.getRequestsByCurrentUser(userId);
    }

    // Добавление запроса от текущего пользователя на участие в событии
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(@PathVariable Long userId,
                                              @RequestParam Long eventId) {
        log.info("PrivateRequestController / addRequest: " +
                "Добавление запроса от текущего пользователя на участие в событии " +
                userId + eventId);
        return requestService.addRequest(userId, eventId);
    }

    // Отмена своего запроса на участие в событии
    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        log.info("PrivateRequestController / cancelRequest: отмена своего запроса на участие в событии " +
                userId + requestId);
        return requestService.cancelRequest(userId, requestId);
    }
}
