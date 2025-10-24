package ru.practicum.service;

import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    // Создание запроса на участие в событии
    ParticipationRequestDto createRequest(Long userId, Long eventId);

    // Получение информации о заявках текущего пользователя на участие в чужих событиях
    List<ParticipationRequestDto> getUserRequests(Long userId);

    // Отмена своего запроса на участие в событии
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    // Получение информации о запросах на участие в событии текущего пользователя
    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId);

    // Изменение статуса заявок на участие в событии текущего пользователя
    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest updateRequest);
}