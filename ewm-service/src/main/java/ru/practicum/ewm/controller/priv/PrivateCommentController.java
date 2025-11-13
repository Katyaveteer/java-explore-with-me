package ru.practicum.ewm.controller.priv;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.service.comment.CommentService;



@RestController
@RequestMapping(path = "/users/{userId}/comments")
@RequiredArgsConstructor
@Slf4j
public class PrivateCommentController {
    private final CommentService commentService;

    // добавление комментария
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable Long userId,
                                 @RequestParam Long eventId,
                                 @Valid @RequestBody NewCommentDto newCommentDto) {
        log.info("PrivateCommentController / addComment: " +
                "добавление пользователем {} комментария {}", userId, eventId);
        return commentService.addComment(userId, eventId, newCommentDto);
    }

    // удаление комментария
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId,
                              @PathVariable Long commentId) {
        log.info("PrivateCommentController / deleteComment: " +
                "удаление пользователем {} комментария {}", userId, commentId);
        commentService.deleteComment(userId, commentId);
    }

    // изменение комментария
    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @Valid @RequestBody NewCommentDto newCommentDto) {
        log.info("PrivateCommentController / updateComment: изменение комментария {}", commentId);
        return commentService.updateComment(userId, commentId, newCommentDto);
    }
}