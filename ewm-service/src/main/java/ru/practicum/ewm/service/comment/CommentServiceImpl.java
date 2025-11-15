package ru.practicum.ewm.service.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // добавление комментария к событию
    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        Comment commentToSave = commentMapper.toComment(newCommentDto);
        commentToSave.setCreated(LocalDateTime.now());
        commentToSave.setCommentator(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не существует: " + userId)));
        commentToSave.setEvent(eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не существует: " + eventId)));

        commentRepository.save(commentToSave);

        return commentMapper.toCommentDto(commentToSave);
    }

    // удаление комментария пользователем
    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не существует: " + commentId));

        if (!userId.equals(comment.getCommentator().getId())) {
            throw new ValidationException("Пользователь с идентификатором не является комментатором: " + userId);
        }

        commentRepository.deleteById(commentId);
    }

    // изменение комментария
    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, @Valid NewCommentDto newCommentDto) {
        Comment commentToUpdate = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не существует: " + commentId));

        if (!userId.equals(commentToUpdate.getCommentator().getId())) {
            throw new ValidationException("Пользователь с идентификатором не является комментатором: " + userId);
        }

        // Используем trim() перед сохранением
        commentToUpdate.setCommentText(newCommentDto.getCommentText().trim());
        commentRepository.save(commentToUpdate);

        return commentMapper.toCommentDto(commentToUpdate);
    }


    // получение всех комментариев к событию
    @Override
    public List<CommentDto> getAllCommentsToEvent(Long eventId, Pageable pageable) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие не существует: " + eventId);
        }

        List<Comment> commentList = commentRepository.findAllByEvent_IdOrderByCreatedDesc(eventId, pageable);
        List<CommentDto> commentDtoList = new ArrayList<>();

        for (Comment comment : commentList) {
            commentDtoList.add(commentMapper.toCommentDto(comment));
        }

        return commentDtoList;
    }

    // удаление комментария админом
    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий не существует: " + commentId);
        }

        commentRepository.deleteById(commentId);
    }
}


