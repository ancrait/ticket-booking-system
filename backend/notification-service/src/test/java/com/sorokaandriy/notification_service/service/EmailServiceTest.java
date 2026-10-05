package com.sorokaandriy.notification_service.service;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    @Test
    void sendHtmlEmail_sendsMimeMessageWithRecipientSubjectAndHtmlBody() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
        EmailService emailService = new EmailService(mailSender);

        emailService.sendHtmlEmail("john@example.com", "Test Subject", "<h1>Hello</h1>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();

        assertThat(message.getRecipients(Message.RecipientType.TO))
                .extracting(Object::toString)
                .containsExactly("john@example.com");
        assertThat(message.getSubject()).isEqualTo("Test Subject");
        assertThat(extractHtml(message)).contains("<h1>Hello</h1>");
    }

    private String extractHtml(MimeMessage message) throws Exception {
        Object content = message.getContent();
        while (content instanceof MimeMultipart multipart) {
            content = multipart.getBodyPart(0).getContent();
        }
        return String.valueOf(content);
    }
}
