package ru.practicum.service;

import ru.practicum.dto.category.CategoryDto;

import java.util.List;

public interface CategoryService {
    // Создание новой категории
    CategoryDto createCategory(CategoryDto categoryDto);

    // Удаление категории по идентификатору
    void deleteCategory(Long categoryId);

    // Обновление категории по идентификатору
    CategoryDto updateCategory(Long categoryId, CategoryDto categoryDto);

    // Получение списка категорий
    List<CategoryDto> getCategories(int from, int size);

    // Получение категории по идентификатору
    CategoryDto getCategory(Long categoryId);
}