package com.mafia.manager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Включает поддержку @Async в приложении.
 * Необходимо для неблокирующей отправки писем в EmailService.
 * Без этой аннотации @Async работать не будет.
 */
@Configuration
@EnableAsync
public class MailConfig {
}
