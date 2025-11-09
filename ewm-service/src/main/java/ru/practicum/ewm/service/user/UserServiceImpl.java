package ru.practicum.ewm.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.user.NewUserRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.exception.AlreadyExistsException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    // admin
    // получение инфо о пользователях
    @Override
    public List<UserDto> getUserList(List<Long> idList, Pageable pageable) {
        if (idList == null) {
            // выгрузить всех пользователей постранично
            return userMapper.toUserDtoList(userRepository.findAll(pageable).toList());
        } else {
            List<User> userList = new ArrayList<>();
            if (userRepository.existsByIdIn(idList)) {
                userList = userRepository.findAllByIdIn(idList, pageable).toList();
            }
            return userMapper.toUserDtoList(userList);
        }
    }

    // добавление нового пользователя
    @Override
    public UserDto addUser(NewUserRequest userDto) {
        if (userRepository.existsByName(userDto.getName())) {
            throw new AlreadyExistsException("User already exists with name: " + userDto.getName());
        }
        User userToSave = userMapper.toUser(userDto);
        userRepository.save(userToSave);
        return userMapper.toUserDto(userToSave);
    }

    // удаление пользователя
    @Override
    public void deleteUser(Long userId) {
        userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User does not exist with id" + userId));
        userRepository.deleteById(userId);
    }
}