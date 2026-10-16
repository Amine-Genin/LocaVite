package ma.locavite.auth.service;

import ma.locavite.auth.domain.User;

public interface EmailService {
    void sendConfirmationEmail(User user);
}
