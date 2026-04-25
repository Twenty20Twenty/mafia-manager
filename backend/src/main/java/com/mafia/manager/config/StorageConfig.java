package com.mafia.manager.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Конфигурация S3-клиента для MinIO.
 *
 * <p>В dev подключается к локальному MinIO.
 * В prod — достаточно поменять endpoint в .env на реальный S3/R2,
 * код менять не нужно.</p>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    private final StorageProperties props;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
                        )
                )
                // MinIO не зависит от региона, но SDK требует указать хоть какой-то
                .region(Region.US_EAST_1)
                // Отключаем path-style → MinIO требует path-style: bucket/key
                .forcePathStyle(true)
                .build();
    }
}
