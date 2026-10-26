package ma.locavite.user.controller;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.dto.UserResponse;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.user.dto.UpdateProfileRequest;
import ma.locavite.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    @Test
    void getMe_usesAuthenticatedUser() {
        User currentUser = user(1L, "client@example.com");
        UserResponse response = response(1L, "client@example.com");
        when(userService.getProfile(1L)).thenReturn(response);

        var result = userController.getMe(currentUser, null);

        assertThat(result.getBody()).isSameAs(response);
        verify(userService).getProfile(1L);
    }

    @Test
    void updateMe_usesAuthenticatedUser() {
        User currentUser = user(2L, "client@example.com");
        UpdateProfileRequest request = new UpdateProfileRequest();
        UserResponse response = response(2L, "client@example.com");
        when(userService.updateProfile(2L, request)).thenReturn(response);

        var result = userController.updateMe(currentUser, null, request);

        assertThat(result.getBody()).isSameAs(response);
        verify(userService).updateProfile(2L, request);
    }

    @Test
    void getMe_resolvesUserFromAuthenticationPrincipal() {
        User currentUser = user(3L, "principal@example.com");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(currentUser);
        when(userService.getProfile(3L)).thenReturn(response(3L, "principal@example.com"));

        var result = userController.getMe(null, authentication);

        assertThat(result.getBody().getId()).isEqualTo(3L);
        verifyNoInteractions(userRepository);
    }

    @Test
    void getMe_resolvesUserByAuthenticationName() {
        User currentUser = user(4L, "client@example.com");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("client@example.com");
        when(authentication.getName()).thenReturn("client@example.com");
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(currentUser));
        when(userService.getProfile(4L)).thenReturn(response(4L, "client@example.com"));

        var result = userController.getMe(null, authentication);

        assertThat(result.getBody().getId()).isEqualTo(4L);
    }

    @Test
    void getMe_rejectsAuthenticationForAnUnknownUser() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("unknown@example.com");
        when(authentication.getName()).thenReturn("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.getMe(null, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Utilisateur non trouvé");
    }

    @Test
    void getMe_rejectsMissingAuthentication() {
        assertThatThrownBy(() -> userController.getMe(null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authentification requise");
    }

    private static User user(Long id, String email) {
        return User.builder().id(id).email(email).build();
    }

    private static UserResponse response(Long id, String email) {
        return UserResponse.builder().id(id).email(email).roles(Set.of()).build();
    }
}
