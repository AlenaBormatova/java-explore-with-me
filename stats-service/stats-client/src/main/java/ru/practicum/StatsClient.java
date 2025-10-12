package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class StatsClient {
    private final String serverUrl;
    private final RestTemplate restTemplate;

    public StatsClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.restTemplate = new RestTemplate();
    }

    public void saveHit(EndpointHitDto endpointHitDto) {
        String url = serverUrl + "/hit";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(endpointHitDto, headers);

        try {
            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
            log.info("Успешно сохранен запрос в сервисе статистики. Статус ответа: {}", response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            log.error("Ошибка при сохранении запроса в сервисе статистики. Статус: {}, Ответ: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new StatsClientException("Ошибка при сохранении запроса в сервисе статистики: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при сохранении запроса в сервисе статистики: {}", e.getMessage());
            throw new StatsClientException("Неожиданная ошибка при сохранении запроса в сервисе статистики", e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String encodedStart = URLEncoder.encode(start.format(formatter), StandardCharsets.UTF_8);
        String encodedEnd = URLEncoder.encode(end.format(formatter), StandardCharsets.UTF_8);

        StringBuilder urlBuilder = new StringBuilder(serverUrl + "/stats?start={start}&end={end}");

        Map<String, String> params = new HashMap<>();
        params.put("start", encodedStart);
        params.put("end", encodedEnd);

        if (uris != null && !uris.isEmpty()) {
            urlBuilder.append("&uris={uris}");
            params.put("uris", String.join(",", uris));
        }

        if (unique != null) {
            urlBuilder.append("&unique={unique}");
            params.put("unique", unique.toString());
        }

        try {
            ResponseEntity<ViewStatsDto[]> response = restTemplate.getForEntity(
                    urlBuilder.toString(),
                    ViewStatsDto[].class,
                    params
            );

            log.info("Успешно получена статистика из сервиса статистики. Количество записей: {}",
                    response.getBody() != null ? response.getBody().length : 0);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }

            return List.of();

        } catch (HttpStatusCodeException e) {
            log.error("Ошибка при получении статистики из сервиса статистики. Статус: {}, Ответ: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new StatsClientException("Ошибка при получении статистики из сервиса статистики: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при получении статистики из сервиса статистики: {}", e.getMessage());
            throw new StatsClientException("Неожиданная ошибка при получении статистики из сервиса статистики", e);
        }
    }
}