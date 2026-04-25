package com.mafia.manager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Swagger / OpenAPI 3.
 *
 * <p>Регистрирует глобальную схему безопасности «Bearer JWT»,
 * которую затем можно применять на уровне отдельных операций
 * через {@code @SecurityRequirement(name = "bearerAuth")}
 * или глобально (см. {@link #openAPI()}).</p>
 *
 * <p>UI доступен по адресу: {@code /swagger-ui/index.html}</p>
 * <p>JSON-схема:            {@code /v3/api-docs}</p>
 */
@Configuration
public class SwaggerConfig {

    /** Имя схемы — используется в аннотациях {@code @SecurityRequirement}. */
    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(buildInfo())
                // Применяем JWT-авторизацию глобально ко всем операциям
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(buildComponents());
    }

    // ── Метаданные ────────────────────────────────────────────────────────────

    private Info buildInfo() {
        return new Info()
                .title("Mafia Manager API")
                .version("1.0.0")
                .description("""
                        REST API платформы управления мафия-турнирами.

                        **Аутентификация**: передайте JWT-токен в заголовке:
                        ```
                        Authorization: Bearer <token>
                        ```
                        Токен получается через `POST /api/auth/login`.
                        """)
                .contact(new Contact()
                        .name("Mafia Manager Team"));
    }

    // ── Компоненты (схемы безопасности) ──────────────────────────────────────

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, buildJwtScheme());
    }

    private SecurityScheme buildJwtScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Введите JWT-токен, полученный при логине");
    }
}
