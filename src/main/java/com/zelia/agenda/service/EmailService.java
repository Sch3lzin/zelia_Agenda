package com.zelia.agenda.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final String SENDER_ADDRESS = "agendazelia@email.com";
    private static final String SENDER_NAME = "Agenda Zélia";
    private static final String BASE_URL = "http://localhost:8080";
    private static final String PASSWORD_RESET_PATH = "/recuperar-conta";

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendPasswordResetEmail(String recipientEmail, String recipientName, String token) {
        String subject = "Aqui está seu link para alterar a senha";
        String resetLink = BASE_URL + PASSWORD_RESET_PATH + "?codigo=" + token;

        String htmlBody = buildPasswordResetHtml(recipientName, resetLink);
        String plainBody = buildPasswordResetText(recipientName, resetLink);

        try {
            sendMessage(recipientEmail, subject, plainBody, htmlBody);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            log.error("Failed to send password reset email to {}", recipientEmail, e);
        }
    }

    private void sendMessage(String to, String subject, String plainBody, String htmlBody)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        helper.setFrom(SENDER_ADDRESS, SENDER_NAME);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(plainBody, htmlBody);

        mailSender.send(message);
    }

    private String buildPasswordResetHtml(String recipientName, String resetLink) {
        String safeName = HtmlUtils.htmlEscape(recipientName);

        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                     style="background-color:#f4f6f8;padding:32px 16px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                           style="max-width:520px;background-color:#ffffff;border-radius:12px;
                                  overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                      <tr>
                        <td style="background-color:#2a7f6f;padding:24px;text-align:center;">
                          <span style="color:#ffffff;font-size:22px;font-weight:bold;">Agenda Zélia</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:32px 28px;color:#333333;font-size:16px;line-height:1.6;">
                          <p style="margin:0 0 16px;">Olá, <strong>[[NAME]]</strong>!</p>
                          <p style="margin:0 0 24px;">
                            Recebemos uma solicitação para redefinir a senha da sua conta.
                            Clique no botão abaixo para criar uma nova senha.
                          </p>
                          <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
                            <tr>
                              <td align="center">
                                <a href="[[LINK]]" target="_self"
                                   style="display:inline-block;background-color:#2a7f6f;color:#ffffff;
                                          text-decoration:none;font-weight:bold;padding:14px 32px;
                                          border-radius:8px;">
                                  Alterar senha
                                </a>
                              </td>
                            </tr>
                          </table>
                          <p style="margin:24px 0 0;font-size:14px;color:#666666;">
                            Este link é válido por 15 minutos. Se você não solicitou a alteração,
                            pode ignorar este e-mail com segurança.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="background-color:#fafafa;padding:16px 28px;font-size:13px;
                                   color:#888888;text-align:center;">
                          Obrigado,<br>Clínica Voll Med.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.replace("[[NAME]]", safeName)
                .replace("[[LINK]]", resetLink);
    }

    private String buildPasswordResetText(String recipientName, String resetLink) {
        return """
            Olá, %s!

            Recebemos uma solicitação para redefinir a senha da sua conta.
            Acesse o link abaixo para criar uma nova senha (válido por 15 minutos):

            %s

            Se você não solicitou a alteração, ignore este e-mail.

            Obrigado,
            Clínica Voll Med.
            """.formatted(recipientName, resetLink);
    }
}