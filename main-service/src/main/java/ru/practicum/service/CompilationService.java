package ru.practicum.service;

import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    // Создание новой подборки событий
    CompilationDto createCompilation(NewCompilationDto newCompilationDto);

    // Удаление подборки событий по идентификатору
    void deleteCompilation(Long compId);

    // Обновление подборки событий по идентификатору
    CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest);

    // Получение списка подборок событий
    List<CompilationDto> getCompilations(Boolean pinned, int from, int size);

    // Получение подборки событий по идентификатору
    CompilationDto getCompilation(Long compId);
}