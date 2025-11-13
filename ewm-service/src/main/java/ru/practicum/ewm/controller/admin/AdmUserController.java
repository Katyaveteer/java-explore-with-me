package ru.practicum.ewm.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.user.NewUserRequest;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.service.user.UserService;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdmUserController {
    private final UserService userService;

    // получение инфо о пользователях
    @GetMapping
    public List<UserDto> getUserList(@RequestParam(required = false, name = "ids") List<Long> idList,
                                     @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                     @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("AdmUserController / getUserList: получение инфо о пользователях " + idList + from + size);
        return userService.getUserList(idList, PageRequest.of(from, size));
    }

    // добавление нового пользователя
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto addUser(@Valid @RequestBody NewUserRequest newUserRequest) {
        log.info("AdmUserController / addUser: добавление нового пользователя " + newUserRequest);
        return userService.addUser(newUserRequest);
    }

    // удаление пользователя
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        log.info("AdmUserController / deleteUser: удаление пользователя " + userId);
        userService.deleteUser(userId);
    }
}