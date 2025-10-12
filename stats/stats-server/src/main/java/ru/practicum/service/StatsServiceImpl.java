package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHitDto;
import ru.practicum.ViewStatsDto;
import ru.practicum.mapper.EndpointHitMapper;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto endpointHitDto) {
        EndpointHit endpointHit = EndpointHitMapper.toEntity(endpointHitDto);
        statsRepository.save(endpointHit);
        log.info("Сохранен запрос: app={}, uri={}, ip={}",
                endpointHit.getApp(), endpointHit.getUri(), endpointHit.getIp());
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Получение статистики с {} по {}, uris: {}, unique: {}", start, end, uris, unique);

        if (Boolean.TRUE.equals(unique)) {
            return statsRepository.findUniqueStats(start, end, uris);
        } else {
            return statsRepository.findStats(start, end, uris);
        }
    }
}