package ru.practicum.service;

import ru.practicum.dto.event.*;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    // Создание нового события пользователем
    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    // Получение событий, созданных пользователем
    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    // Получение события пользователя по идентификатору
    EventFullDto getUserEvent(Long userId, Long eventId);

    // Обновление события пользователем
    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    // Поиск событий по фильтрам для администраторов
    List<EventFullDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                      LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    // Обновление события администратором
    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest updateRequest);

    // Получение опубликованных событий для публичного доступа с фильтрацией
    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        Boolean onlyAvailable, String sort, int from, int size,
                                        String clientIp, String requestUri);

    // Получение опубликованного события по идентификатору для публичного доступа
    EventFullDto getPublicEvent(Long eventId, String clientIp, String requestUri);
}