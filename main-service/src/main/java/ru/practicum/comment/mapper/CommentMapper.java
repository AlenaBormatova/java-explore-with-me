package ru.practicum.comment.mapper;


import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.CommentStatus;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.model.Comment;
import ru.practicum.mapper.UserMapper;

public class CommentMapper {

    public static Comment toEntity(NewCommentDto dto) {
        return Comment.builder()
                .text(dto.getText())
                .status(CommentStatus.PENDING)
                .build();
    }

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(UserMapper.toShortDto(comment.getAuthor()))
                .eventId(comment.getEvent().getId())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .status(comment.getStatus())
                .build();
    }
}