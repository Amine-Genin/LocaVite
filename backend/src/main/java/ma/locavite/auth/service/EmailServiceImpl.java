package ma.locavite.auth.service;

import ma.locavite.auth.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendConfirmationEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@locavite.ma");
            message.setTo(user.getEmail());
            message.setSubject("Bienvenue sur LocaVite - Confirmation d'inscription");
            message.setText("Bonjour " + user.getPrenom() + " " + user.getNom() + ",\n\n"
                    + "Votre compte client a été créé avec succès sur LocaVite.\n"
                    + "Vous pouvez dès à présent vous connecter et profiter de nos services de location de véhicules.\n\n"
                    + "Cordialement,\n"
                    + "L'équipe LocaVite");
            mailSender.send(message);
            log.info("Email de confirmation envoyé à {}", user.getEmail());
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email de confirmation à {} : {}", user.getEmail(), e.getMessage());
        }
    }
}

