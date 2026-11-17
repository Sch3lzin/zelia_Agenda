package com.zelia.agenda.service;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        lenient().when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        emailService = new EmailService(mailSender, "https://agenda.exemplo.com/",
                "no-reply@exemplo.com", "Agenda Zélia");
    }

    /**
     * Walks the message tree and returns the first part of the given MIME type.
     * The top-level MimeMessage has no Content-Type header until saveChanges(), so
     * we check for a Multipart container first instead of relying on isMimeType().
     */
    private static String contentOf(Part part, String mimeType) throws Exception {
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = contentOf(multipart.getBodyPart(i), mimeType);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        if (part.isMimeType(mimeType)) {
            return (String) content;
        }
        return null;
    }

    private MimeMessage captureSentMessage() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();
        // Headers (Content-Type of every part) are only written by saveChanges(); without it,
        // isMimeType() reports text/plain for all parts, including the HTML one.
        message.saveChanges();
        return message;
    }

    @Test
    void sendsEmailToRecipientWithExpectedSubject() throws Exception {
        emailService.sendPasswordResetEmail("maria@exemplo.com", "Maria", "TOKEN123");

        MimeMessage message = captureSentMessage();

        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("maria@exemplo.com");
        assertThat(message.getSubject()).contains("link");
    }

    @Test
    void resetLinkHasTokenInFragmentWithoutDoubleSlash() throws Exception {
        emailService.sendPasswordResetEmail("maria@exemplo.com", "Maria", "TOKEN123");

        String text = contentOf(captureSentMessage(), "text/plain");

        assertThat(text).contains("https://agenda.exemplo.com/recuperar-conta#codigo=TOKEN123");
        assertThat(text).doesNotContain("com//recuperar");
    }

    @Test
    void escapesRecipientNameInHtml() throws Exception {
        emailService.sendPasswordResetEmail("x@exemplo.com", "<b>Ana</b>", "T");

        String html = contentOf(captureSentMessage(), "text/html");

        assertThat(html).contains("&lt;b&gt;Ana&lt;/b&gt;");
        assertThat(html).doesNotContain("<b>Ana</b>");
    }

    @Test
    void sendFailureDoesNotPropagateException() {
        doThrow(new MailSendException("smtp fora do ar")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> emailService.sendPasswordResetEmail("x@exemplo.com", "Ana", "T"))
                .doesNotThrowAnyException();
    }

    @Test
    void masksEmailForLogs() {
        assertThat(EmailService.maskEmail("maria@exemplo.com")).isEqualTo("m***@exemplo.com");
        assertThat(EmailService.maskEmail("a@exemplo.com")).isEqualTo("***");
        assertThat(EmailService.maskEmail("semarroba")).isEqualTo("***");
    }
}
