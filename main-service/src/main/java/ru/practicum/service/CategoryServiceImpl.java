package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.category.Category;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

     /*
     Методы для работы с категориями
     */

    // Создание новой категории
    @Override
    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        if (categoryRepository.findAll().stream()
                .anyMatch(category -> category.getName().equals(categoryDto.getName()))) {
            throw new ConflictException("Категория с таким названием уже существует");
        }

        Category category = CategoryMapper.toEntity(categoryDto);
        Category savedCategory = categoryRepository.save(category);
        return CategoryMapper.toDto(savedCategory);
    }

    // Удаление категории по идентификатору
    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));

        if (!eventRepository.findByCategoryId(catId).isEmpty()) {
            throw new ConflictException("Категория не пуста");
        }

        categoryRepository.delete(category);
    }

    // Обновление категории по идентификатору
    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));

        // Если имя не изменилось, просто вернем текущую категорию
        if (categoryDto.getName() != null && categoryDto.getName().equals(category.getName())) {
            return CategoryMapper.toDto(category);
        }

        // Если имя изменилось, проверяем уникальность
        if (categoryDto.getName() != null &&
                !categoryDto.getName().equals(category.getName()) &&
                categoryRepository.existsByNameAndIdNot(categoryDto.getName(), catId)) {
            throw new ConflictException("Категория с таким названием уже существует");
        }

        // Обновляем только если есть изменения
        if (categoryDto.getName() != null) {
            category.setName(categoryDto.getName());
            Category updatedCategory = categoryRepository.save(category);
            return CategoryMapper.toDto(updatedCategory);
        }

        // Если никаких изменений нет, вернем текущую категорию
        return CategoryMapper.toDto(category);
    }

    // Получение списка категорий
    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        PageRequest pageRequest = PageRequest.of(from, size);
        Page<Category> categoryPage = categoryRepository.findAll(pageRequest);

        return categoryPage.getContent().stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    // Получение категории по идентификатору
    @Override
    public CategoryDto getCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
        return CategoryMapper.toDto(category);
    }
}