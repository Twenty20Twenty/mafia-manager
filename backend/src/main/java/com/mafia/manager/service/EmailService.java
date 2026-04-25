package com.mafia.manager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Отвечает исключительно за отправку писем.
 * Никакой бизнес-логики — только формирование MimeMessage и отправка.
 * Вся логика кодов/токенов живёт в EmailVerificationService.
 *
 * Методы помечены @Async — отправка не блокирует HTTP-ответ пользователю.
 * Для @Async нужен @EnableAsync в конфиге (добавлен в MailConfig).
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // ── РЕГИСТРАЦИЯ ───────────────────────────────────────────────────────────

    /**
     * Отправляет письмо с 6-значным кодом подтверждения после регистрации.
     *
     * @param toEmail  адрес получателя
     * @param nickname никнейм пользователя (для обращения в письме)
     * @param code     6-значный цифровой код
     */
    @Async
    public void sendRegistrationCode(String toEmail, String nickname, String code) {
        String subject = "Mafia Manager — подтверждение email";
        String body = buildRegistrationBody(nickname, code);
        send(toEmail, subject, body);
    }

    // ── СМЕНА EMAIL ───────────────────────────────────────────────────────────

    /**
     * Отправляет письмо с кодом подтверждения на НОВЫЙ email.
     *
     * @param toEmail  новый адрес, который пользователь хочет установить
     * @param nickname никнейм пользователя
     * @param code     6-значный цифровой код
     */
    @Async
    public void sendEmailChangeCode(String toEmail, String nickname, String code) {
        String subject = "Mafia Manager — подтверждение нового email";
        String body = buildEmailChangeBody(nickname, code);
        send(toEmail, subject, body);
    }

    // ── СБРОС ПАРОЛЯ ─────────────────────────────────────────────────────────

    /**
     * Отправляет письмо со ссылкой для сброса пароля.
     * Ссылка ведёт на фронтенд: {frontendUrl}/reset-password?token={token}
     *
     * @param toEmail  адрес пользователя
     * @param nickname никнейм пользователя
     * @param token    UUID-токен (вставляется в ссылку)
     */
    @Async
    public void sendPasswordResetLink(String toEmail, String nickname, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject   = "Mafia Manager — сброс пароля";
        String body      = buildPasswordResetBody(nickname, resetLink);
        send(toEmail, subject, body);
    }

    // ── ВНУТРЕННЯЯ ОТПРАВКА ───────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            // Не бросаем дальше — письмо асинхронное, не должно ронять запрос.
            // В проде сюда нужно добавить логгирование (Slf4j / Sentry).
            throw new RuntimeException("Ошибка отправки письма на " + to, e);
        }
    }

    // ── HTML-ШАБЛОНЫ ─────────────────────────────────────────────────────────

    private String buildRegistrationBody(String nickname, String code) {
        return """
            <div style="font-family:sans-serif;max-width:480px;margin:0 auto">
              <h2 style="color:#E03131">Mafia Manager</h2>
              <p>Привет, <b>%s</b>!</p>
              <p>Введи этот код для подтверждения email:</p>
              <div style="font-size:32px;font-weight:bold;letter-spacing:8px;
                          padding:16px 24px;background:#f4f4f4;
                          border-radius:8px;display:inline-block;margin:8px 0">
                %s
              </div>
              <p style="color:#888;font-size:13px">Код действителен 15 минут.<br>
                Если ты не регистрировался — просто проигнорируй это письмо.</p>
            </div>
            """.formatted(nickname, code);
    }

    private String buildEmailChangeBody(String nickname, String code) {
        return """
            <div style="font-family:sans-serif;max-width:480px;margin:0 auto">
              <h2 style="color:#E03131">Mafia Manager</h2>
              <p>Привет, <b>%s</b>!</p>
              <p>Введи этот код для подтверждения нового email-адреса:</p>
              <div style="font-size:32px;font-weight:bold;letter-spacing:8px;
                          padding:16px 24px;background:#f4f4f4;
                          border-radius:8px;display:inline-block;margin:8px 0">
                %s
              </div>
              <p style="color:#888;font-size:13px">Код действителен 15 минут.<br>
                Если ты не менял email — немедленно смени пароль.</p>
            </div>
            """.formatted(nickname, code);
    }

    private String buildPasswordResetBody(String nickname, String resetLink) {
        return """
            <div style="font-family:sans-serif;max-width:480px;margin:0 auto">
              <h2 style="color:#E03131">Mafia Manager</h2>
              <p>Привет, <b>%s</b>!</p>
              <p>Мы получили запрос на сброс пароля. Нажми кнопку ниже:</p>
              <a href="%s"
                 style="display:inline-block;padding:12px 24px;
                        background:#E03131;color:#fff;border-radius:8px;
                        text-decoration:none;font-weight:bold;margin:8px 0">
                Сбросить пароль
              </a>
              <p style="color:#888;font-size:13px">Ссылка действительна 1 час.<br>
                Если ты не запрашивал сброс — просто проигнорируй это письмо.</p>
            </div>
            """.formatted(nickname, resetLink);
    }
}
