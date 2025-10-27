package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.CommentStatus;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentRequest;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.model.event.Event;
import ru.practicum.model.event.EventState;
import ru.practicum.model.user.User;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    /*
     Публичные методы
     */

    // Получение опубликованных комментариев к событию
    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано");
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        List<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.APPROVED, pageable);

        return comments.stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    /*
     Приватные методы
     */

    // Создание нового комментария пользователем
    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        // Проверяем, не оставлял ли пользователь уже комментарий к этому событию
        if (commentRepository.existsByAuthorIdAndEventId(userId, eventId)) {
            throw new ConflictException("Пользователь уже оставил комментарий к этому событию");
        }

        Comment comment = CommentMapper.toEntity(newCommentDto);
        comment.setAuthor(user);
        comment.setEvent(event);

        Comment savedComment = commentRepository.save(comment);
        return CommentMapper.toDto(savedComment);
    }

    // Получение комментариев текущего пользователя
    @Override
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        List<Comment> comments = commentRepository.findByAuthorId(userId, pageable);

        return comments.stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    // Получение конкретного комментария пользователя
    @Override
    public CommentDto getUserComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));
        return CommentMapper.toDto(comment);
    }

    // Обновление комментария пользователем
    @Override
    @Transactional
    public CommentDto updateUserComment(Long userId, Long commentId, UpdateCommentRequest updateRequest) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Можно редактировать только комментарии со статусом PENDING");
        }

        comment.setText(updateRequest.getText());
        Comment updatedComment = commentRepository.save(comment);
        return CommentMapper.toDto(updatedComment);
    }

    // Удаление комментария пользователем
    @Override
    @Transactional
    public void deleteUserComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));
        commentRepository.delete(comment);
    }

    /*
     Административные методы
     */

    // Получение комментариев с фильтрацией для администратора
    @Override
    public List<CommentDto> getAdminComments(List<Long> eventIds, List<String> statuses, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());

        List<CommentStatus> commentStatuses = null;
        if (statuses != null) {
            commentStatuses = statuses.stream()
                    .map(this::parseCommentStatus)
                    .collect(Collectors.toList());
        }

        List<Comment> comments = commentRepository.findAdminComments(eventIds, commentStatuses, pageable);
        return comments.stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    // Модерация комментария администратором
    @Override
    @Transactional
    public CommentDto updateAdminComment(Long commentId, String status) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        CommentStatus newStatus = parseCommentStatus(status);

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Можно изменять статус только для комментариев со статусом PENDING");
        }

        comment.setStatus(newStatus);
        Comment updatedComment = commentRepository.save(comment);
        return CommentMapper.toDto(updatedComment);
    }

    // Удаление комментария администратором
    @Override
    @Transactional
    public void deleteAdminComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий не найден");
        }
        commentRepository.deleteById(commentId);
    }

    // Преобразует строковое представление статуса в enum CommentStatus
    private CommentStatus parseCommentStatus(String status) {
        try {
            return CommentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Неверный статус комментария: " + status +
                    ". Допустимые значения: PENDING, APPROVED, REJECTED");
        }
    }
}