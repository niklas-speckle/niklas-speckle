package at.qe.skeleton.services;

import at.qe.skeleton.model.Userx;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for sending mails via Spring Boot Starter Mail.
 */

@Component
@Scope("application")
public class EmailService {

    String username = "spring.mail.username";
    @Autowired
    private Environment env;


    /**
     * Creates and configures an instance of JavaMailSenderImpl.
     * Sets mail server properties (host, port etc.) based on environment configurations.
     * @return an instance of JavaMailSender (implemented by the JavaMailSenderImpl)
     */
    private JavaMailSender getJavaMailSender() {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(env.getProperty("spring.mail.host"));
        mailSender.setPort(587);

        mailSender.setUsername(env.getProperty(username));
        mailSender.setPassword(env.getProperty("spring.mail.password"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", env.getProperty("spring.mail.properties.mail.transport.protocol"));
        props.put("mail.smtp.auth", env.getProperty("spring.mail.properties.mail.smtp.auth"));
        props.put("mail.smtp.starttls.enable", env.getProperty("spring.mail.properties.mail.smtp.starttls.enable"));
        props.put("mail.debug", env.getProperty("spring.mail.debug"));
        props.put("mail.smtp.localhost", env.getProperty("spring.mail.smtp.localhost"));

        return mailSender;
    }

    /**
     *  Creates a MimeMessage and sets the email details (like the from Address) to send email via java mail server.
     *  In case of a MessagingException (e.g., due to a mail server issue), it logs an error entry using LogService.
     * @param sendTo the email receiving user
     * @param subject
     * @param message
     */
    public void sendEmail(Userx sendTo, String subject, String message) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {

            try {
                MimeMessage msg = getJavaMailSender().createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, "utf-8");
                helper.setFrom(Objects.requireNonNull(env.getProperty(username)));
                helper.setTo(sendTo.getEmail());
                helper.setSubject(subject);
                helper.setText(message, true);
                getJavaMailSender().send(msg);
            } catch (MessagingException ignored) {
            }
        });
        executor.shutdown();
    }
}
