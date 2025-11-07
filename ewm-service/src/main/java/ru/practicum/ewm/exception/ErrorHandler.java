package ru.practicum.ewm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestControllerAdvice
public class ErrorHandler {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError badRequest(final ValidationException e) {
        return new ApiError(e.getMessage(), "Incorrectly made request.",
                HttpStatus.BAD_REQUEST.getReasonPhrase().toUpperCase(), LocalDateTime.now().format(DTF));
    }

    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ApiError notFound(final NotFoundException e) {
        return new ApiError(e.getMessage(), "The required object was not found.",
                HttpStatus.NOT_FOUND.getReasonPhrase().toUpperCase(), LocalDateTime.now().format(DTF));
    }


    @ExceptionHandler
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public ApiError alreadyExists(AlreadyExistsException e) {
        return new ApiError(e.getMessage(), "For the requested operation the conditions are not met.",
                HttpStatus.CONFLICT.getReasonPhrase().toUpperCase(), LocalDateTime.now().format(DTF));
    }
}