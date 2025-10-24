package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.event.Event;
import ru.practicum.model.event.EventState;
import ru.practicum.model.request.ParticipationRequest;
import ru.practicum.model.request.RequestStatus;
import ru.practicum.model.user.User;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    /*
     Методы для работы с заявками на участие в событиях
     */

    // Создание запроса на участие в событии
    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        // Валидация входных параметров
        if (userId == null || userId < 1) {
            throw new ValidationException("ID пользователя должен быть положительным");
        }
        if (eventId == null || eventId < 1) {
            throw new ValidationException("ID события должен быть положительным");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        // Проверка на повторный запрос
        requestRepository.findByRequesterIdAndEventId(userId, eventId)
                .ifPresent(request -> {
                    throw new ConflictException("Запрос уже существует");
                });

        // Инициатор события не может добавить запрос на участие в своём событии
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор не может подавать заявку на участие в своем собственном событии");
        }

        // Нельзя участвовать в неопубликованном событии
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        // Проверка лимита участников
        if (event.getParticipantLimit() > 0) {
            Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedRequests >= event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит участников");
            }
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requester(user)
                .event(event)
                .status(event.getRequestModeration() && event.getParticipantLimit() > 0 ?
                        RequestStatus.PENDING : RequestStatus.CONFIRMED)
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);

        // Если автоматическое подтверждение - увеличиваем счетчик подтвержденных заявок
        if (savedRequest.getStatus() == RequestStatus.CONFIRMED) {
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }

        return RequestMapper.toDto(savedRequest);
    }

    // Получение информации о заявках текущего пользователя на участие в чужих событиях
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        return requestRepository.findByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    // Отмена своего запроса на участие в событии
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Запрос не найден для текущего пользователя");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        // Если запрос был подтвержден, уменьшаем счетчик
        if (request.getStatus() == RequestStatus.CONFIRMED) {
            Event event = request.getEvent();
            event.setConfirmedRequests(event.getConfirmedRequests() - 1);
            eventRepository.save(event);
        }

        return RequestMapper.toDto(updatedRequest);
    }

    // Получение информации о запросах на участие в событии текущего пользователя
    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Пользователь не является инициатором этого события");
        }

        return requestRepository.findByEventInitiatorIdAndEventId(userId, eventId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    // Изменение статуса заявок на участие в событии текущего пользователя
    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Пользователь не является инициатором этого события");
        }

        List<ParticipationRequest> requests = requestRepository.findByIdIn(updateRequest.getRequestIds());

        // Проверка, что все запросы находятся в состоянии ожидания
        if (requests.stream().anyMatch(req -> req.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Запрос должен иметь статус PENDING");
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        if ("CONFIRMED".equals(updateRequest.getStatus())) {
            // Подтверждение заявок
            Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            int availableSlots = event.getParticipantLimit() - confirmedCount.intValue();

            if (availableSlots <= 0) {
                throw new ConflictException("Достигнут лимит участников");
            }

            for (ParticipationRequest request : requests) {
                if (availableSlots > 0) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    requestRepository.save(request);
                    result.getConfirmedRequests().add(RequestMapper.toDto(request));
                    availableSlots--;

                    // Обновляем счетчик подтвержденных заявок
                    event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    requestRepository.save(request);
                    result.getRejectedRequests().add(RequestMapper.toDto(request));
                }
            }

            eventRepository.save(event);

        } else if ("REJECTED".equals(updateRequest.getStatus())) {
            // Отклонение заявок
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.REJECTED);
                requestRepository.save(request);
                result.getRejectedRequests().add(RequestMapper.toDto(request));
            }
        }

        return result;
    }
}