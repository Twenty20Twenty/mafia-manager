package com.mafia.manager.service.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Абстракция над объектным хранилищем.
 *
 * <p>Реализации: {@link MinioStorageService} (dev/prod self-hosted),
 * в будущем — S3StorageService, R2StorageService без изменения клиентского кода.</p>
 */
public interface StorageService {

    /**
     * Загружает аватар пользователя в хранилище.
     *
     * @param entityId  ID сущности (userId или clubId) — используется в имени файла
     * @param file      загружаемый файл
     * @return          публичный URL для отображения в браузере
     */
    String uploadAvatar(Long entityId, MultipartFile file);

    /**
     * Удаляет файл по его публичному URL.
     * Безопасен при вызове с null или URL из другого хранилища — просто игнорирует.
     *
     * @param fileUrl   публичный URL файла (как хранится в avatar_url)
     */
    void deleteByUrl(String fileUrl);
}
