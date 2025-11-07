package mapper;


import dto.event.LocationDto;
import model.Location;
import org.springframework.stereotype.Component;


@Component
public class LocationMapper {

    public LocationDto toLocationDto(Location location) {
        if (location == null) {
            return null;
        }

        LocationDto locationDto = new LocationDto();
        locationDto.setLat(location.getLat());
        locationDto.setLon(location.getLon());

        return locationDto;
    }

    public Location toLocation(LocationDto locationDto) {
        if (locationDto == null) {
            return null;
        }

        Location location = new Location();
        location.setLat(locationDto.getLat());
        location.setLon(locationDto.getLon());

        return location;
    }
}
