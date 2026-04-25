package com.mafia.manager.service.storage;

import com.mafia.manager.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * Реализация StorageService для MinIO (и любого S3-совместимого хранилища).
 *
 * <p>Структура ключей в bucket:
 * <pre>
 *   avatars/users/{userId}/{uuid}.jpg
 *   avatars/clubs/{clubId}/{uuid}.png
 * </pre>
 * Такая структура позволяет легко удалять все файлы конкретного объекта
 * и не перемешивать файлы разных типов.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final S3Client          s3Client;
    private final StorageProperties props;

    // ── Загрузка ─────────────────────────────────────────────────────────────

    @Override
    public String uploadAvatar(Long entityId, MultipartFile file) {
        FileValidator.validateImage(file);

        String key = buildAvatarKey(entityId, file);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(props.getBucket().getAvatars())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения файла при загрузке: " + e.getMessage(), e);
        }

        String publicUrl = buildPublicUrl(props.getBucket().getAvatars(), key);
        log.info("Avatar uploaded: {}", publicUrl);
        return publicUrl;
    }

    // ── Удаление ─────────────────────────────────────────────────────────────

    @Override
    public void deleteByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        // Пытаемся извлечь key из URL вида: http://host:9000/bucket/key
        String key = extractKeyFromUrl(fileUrl);
        if (key == null) {
            log.warn("Cannot extract S3 key from URL, skipping delete: {}", fileUrl);
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(props.getBucket().getAvatars())
                    .key(key)
                    .build());
            log.info("Deleted file from storage: {}", key);
        } catch (Exception e) {
            // Не бросаем исключение — файл может уже не существовать
            log.warn("Failed to delete file from storage: {}, error: {}", key, e.getMessage());
        }
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    /**
     * Формирует уникальный ключ для аватара.
     * Пример: avatars/123/a1b2c3d4-e5f6.jpg
     */
    private String buildAvatarKey(Long entityId, MultipartFile file) {
        String ext = FileValidator.extractExtension(file);
        String uuid = UUID.randomUUID().toString();
        return "avatars/" + entityId + "/" + uuid + ext;
    }

    /**
     * Строит публичный URL для отдачи в браузер.
     * Пример: http://localhost:9000/avatars/avatars/123/uuid.jpg
     */
    private String buildPublicUrl(String bucket, String key) {
        String base = props.getPublicUrl();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/" + bucket + "/" + key;
    }

    /**
     * Извлекает S3-key из публичного URL.
     * URL: http://localhost:9000/avatars/avatars/123/uuid.jpg
     * Key: avatars/123/uuid.jpg (всё после /{bucket}/)
     */
    private String extractKeyFromUrl(String url) {
        String bucket = props.getBucket().getAvatars();
        String marker = "/" + bucket + "/";
        int idx = url.indexOf(marker);
        if (idx < 0) return null;
        return url.substring(idx + marker.length());
    }
}
