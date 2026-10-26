package ma.locavite.vehicle.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserId_returnsEmptyWithoutAuthenticatedUser() {
        assertThat(currentUserService.getCurrentUserId()).isEmpty();

        var unauthenticated = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user@example.com", "password");
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);
        assertThat(currentUserService.getCurrentUserId()).isEmpty();

        var anonymous = new AnonymousAuthenticationToken("key", "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        assertThat(currentUserService.getCurrentUserId()).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void getCurrentUserId_returnsIdForAuthenticatedUser() {
        var authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated("user@example.com", "password", List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(User.builder().id(42L).build()));

        assertThat(currentUserService.getCurrentUserId()).contains(42L);
    }

    @Test
    void getCurrentUserId_returnsEmptyWhenAuthenticatedUserIsMissing() {
        var authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated("missing@example.com", "password", List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThat(currentUserService.getCurrentUserId()).isEmpty();
    }

    @Test
    void isAdmin_returnsFalseWithoutAuthenticationOrAdminAuthority() {
        assertThat(currentUserService.isAdmin()).isFalse();

        var authentication = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated("user@example.com", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(currentUserService.isAdmin()).isFalse();
    }

    @Test
    void isAdmin_acceptsBothSupportedAdminAuthorities() {
        var rolePrefixed = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated("admin@example.com", "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(rolePrefixed);
        assertThat(currentUserService.isAdmin()).isTrue();

        var unprefixed = org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated("admin@example.com", "password",
                        List.of(new SimpleGrantedAuthority("ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(unprefixed);
        assertThat(currentUserService.isAdmin()).isTrue();
    }
}
