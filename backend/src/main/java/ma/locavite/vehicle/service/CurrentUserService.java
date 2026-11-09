package ma.locavite.vehicle.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Point unique à adapter à ton module auth (type de l'id, méthode de recherche de l'utilisateur,
 * nom des rôles).
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Id de l'utilisateur connecté, vide si anonyme. */
    public Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        // ADAPTER : findByEmail / findByUsername selon ton UserRepository
        return userRepository.findByEmail(auth.getName()).map(User::getId);
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return hasAuthority(auth, "ROLE_ADMIN", "ADMIN");
    }

    public boolean isAgence() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return hasAuthority(auth, "ROLE_AGENCE", "AGENCE");
    }

    private boolean hasAuthority(Authentication auth, String... expectedAuthorities) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> java.util.Arrays.stream(expectedAuthorities)
                        .anyMatch(expected -> expected.equals(authority)));
    }
}