package ru.practicum.ewm.mapper;


import org.mapstruct.Mapper;
import ru.practicum.ewm.dto.event.LocationDto;
import ru.practicum.ewm.model.Location;
import org.springframework.stereotype.Component;


@Mapper(componentModel = "spring")
public interface LocationMapper {
    Location toLocation(LocationDto locationDto);

    LocationDto toLocationDto(Location location);
}
