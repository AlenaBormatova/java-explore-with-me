package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.EndpointHitDto;
import ru.practicum.StatsClient;
import ru.practicum.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final StatsClient statsClient;

    @Value("${app.name:ewm-main-service}")
    private String appName;

    /*
     Методы для работы со статистикой
     */

    // Сохранение информации о запросе к эндпоинту
    public void saveHit(String uri, String ip) {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app(appName)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.saveHit(hit);
    }

    // Получение статистики по посещениям за указанный период
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        return statsClient.getStats(start, end, uris, unique);
    }

    // Получение количества просмотров для события
    public Long getViewsForEvent(Long eventId) {
        LocalDateTime start = LocalDateTime.now().minusYears(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/events/" + eventId);

        List<ViewStatsDto> stats = getStats(start, end, uris, true);
        return stats.isEmpty() ? 0L : stats.getFirst().getHits();
    }
}