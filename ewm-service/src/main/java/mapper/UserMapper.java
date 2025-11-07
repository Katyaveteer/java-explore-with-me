package mapper;


import dto.user.NewUserRequest;
import dto.user.UserDto;
import dto.user.UserShortDto;
import model.User;
import org.springframework.stereotype.Component;


@Component
public class UserMapper {

    public UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());

        return userDto;
    }

    public User toUser(NewUserRequest newUserRequest) {
        if (newUserRequest == null) {
            return null;
        }

        User user = new User();
        user.setName(newUserRequest.getName());
        user.setEmail(newUserRequest.getEmail());

        return user;
    }

    public UserShortDto toUserShortDto(User user) {
        if (user == null) {
            return null;
        }

        UserShortDto userShortDto = new UserShortDto();
        userShortDto.setId(user.getId());
        userShortDto.setName(user.getName());

        return userShortDto;
    }
}
