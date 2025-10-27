package ru.practicum.comment.service;

import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentRequest;

import java.util.List;

public interface CommentService {
    // Получение опубликованных комментариев к событию
    List<CommentDto> getEventComments(Long eventId, int from, int size);

    // Создание нового комментария пользователем
    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    // Получение комментариев текущего пользователя
    List<CommentDto> getUserComments(Long userId, int from, int size);

    // Получение конкретного комментария пользователя
    CommentDto getUserComment(Long userId, Long commentId);

    // Обновление комментария пользователем
    CommentDto updateUserComment(Long userId, Long commentId, UpdateCommentRequest updateRequest);

    // Удаление комментария пользователем
    void deleteUserComment(Long userId, Long commentId);

    // Получение комментариев с фильтрацией для администратора
    List<CommentDto> getAdminComments(List<Long> eventIds, List<String> statuses, int from, int size);

    // Модерация комментария администратором
    CommentDto updateAdminComment(Long commentId, String status);

    // Удаление комментария администратором
    void deleteAdminComment(Long commentId);
}