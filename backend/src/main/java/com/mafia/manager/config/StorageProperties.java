package com.mafia.manager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурация объектного хранилища (MinIO / S3-совместимого).
 * Все значения берутся из application.properties с префиксом storage.minio.
 */
@Data
//@Component
@ConfigurationProperties(prefix = "storage.minio")
public class StorageProperties {

    /** URL S3-эндпоинта. Dev: http://minio:9000. Prod: https://s3.yourdomain.com */
    private String endpoint;

    /** Access key (MINIO_ROOT_USER в dev, IAM key в prod). */
    private String accessKey;

    /** Secret key (MINIO_ROOT_PASSWORD в dev, IAM secret в prod). */
    private String secretKey;

    /** Названия bucket-ов. */
    private Buckets bucket = new Buckets();

    /**
     * Публичный URL для формирования ссылок, доступных браузеру.
     * Может отличаться от endpoint (например, через nginx reverse proxy).
     */
    private String publicUrl;

    @Data
    public static class Buckets {
        private String avatars = "avatars";
    }
}
