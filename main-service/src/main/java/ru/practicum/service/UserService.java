package ru.practicum.service;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserService {
    // Создание нового пользователя
    UserDto createUser(NewUserRequest newUserRequest);

    // Получение списка пользователей
    List<UserDto> getUsers(List<Long> ids, int from, int size);

    // Удаление пользователя по идентификатору
    void deleteUser(Long userId);
}