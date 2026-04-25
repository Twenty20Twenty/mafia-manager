package com.mafia.manager.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Утилита валидации загружаемых файлов.
 * Вынесена отдельно, чтобы легко переиспользовать в других upload-endpoints.
 */
public final class FileValidator {

    private static final long   MAX_SIZE_BYTES    = 5 * 1024 * 1024L; // 5 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    private FileValidator() {}

    /**
     * Проверяет файл перед загрузкой в хранилище.
     *
     * @throws IllegalArgumentException при невалидном файле
     */
    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Размер файла превышает 5 МБ. Текущий: " + (file.getSize() / 1024 / 1024) + " МБ"
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Недопустимый тип файла: " + contentType + ". Разрешены: JPEG, PNG, WebP, GIF"
            );
        }

        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            String ext = originalName.toLowerCase();
            boolean validExt = ALLOWED_EXTENSIONS.stream().anyMatch(ext::endsWith);
            if (!validExt) {
                throw new IllegalArgumentException(
                        "Недопустимое расширение файла. Разрешены: " + ALLOWED_EXTENSIONS
                );
            }
        }
    }

    /**
     * Извлекает расширение из оригинального имени файла.
     * Возвращает .jpg если расширение не определено.
     */
    public static String extractExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf(".")).toLowerCase();
        }
        // fallback по content-type
        String ct = file.getContentType();
        if (ct != null) {
            return switch (ct.toLowerCase()) {
                case "image/png"  -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif"  -> ".gif";
                default           -> ".jpg";
            };
        }
        return ".jpg";
    }
}
