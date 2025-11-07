package ru.practicum.statsdto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViewStatsRequest {
    LocalDateTime start;
    LocalDateTime end;
    List<String> uris;
    boolean unique;
}
