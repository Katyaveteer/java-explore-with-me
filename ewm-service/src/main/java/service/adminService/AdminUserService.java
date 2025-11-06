package service.adminService;

import dto.user.NewUserRequest;
import dto.user.UserDto;
import exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import model.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        List<User> users = (ids == null || ids.isEmpty())
                ? userRepository.findAll(page).getContent()
                : userRepository.findAllByIdIn(ids, page);
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public UserDto registerUser(NewUserRequest dto) {
        User user = User.builder().name(dto.getName()).email(dto.getEmail()).build();
        user = userRepository.save(user);
        return toDto(user);
    }

    public void delete(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
    }

    private UserDto toDto(User u) {
        return UserDto.builder().id(u.getId()).name(u.getName()).email(u.getEmail()).build();
    }
}
