package ru.practicum.ewm.exception;


import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor

public class ApiError {
    String message;
    String reason;
    String status;
    String timestamp;
}
