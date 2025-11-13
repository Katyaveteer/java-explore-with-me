package ru.practicum.ewm.dto.location;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocationDto {
    Long id;

    Float lat; // Широта

    Float lon; // Долгота
}
