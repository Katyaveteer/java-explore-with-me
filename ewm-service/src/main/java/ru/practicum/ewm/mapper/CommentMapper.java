package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.model.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(source = "event.id", target = "event")
    @Mapping(source = "commentator.id", target = "commentator")
    CommentDto toCommentDto(Comment comment);

    Comment toComment(NewCommentDto newCommentDto);
}
