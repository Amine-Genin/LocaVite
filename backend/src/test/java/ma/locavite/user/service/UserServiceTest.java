package ma.locavite.user.service;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.dto.UserResponse;
import ma.locavite.auth.mapper.UserMapper;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.common.exception.InvalidRoleException;
import ma.locavite.common.exception.SelfRoleModificationException;
import ma.locavite.common.exception.UserNotFoundException;
import ma.locavite.user.dto.AssignRolesRequest;
import ma.locavite.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsers_mapsUsersFromTheRequestedPage() {
        User user = user("client@example.com", Set.of(role("CLIENT")));
        PageRequest pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));

        var result = userService.getAllUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("client@example.com");
    }

    @Test
    void getProfile_returnsTheMappedUser() {
        User user = user("client@example.com", Set.of(role("CLIENT")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getProfile(1L);

        assertThat(result.getEmail()).isEqualTo("client@example.com");
    }

    @Test
    void getProfile_throwsWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateProfile_savesAndReturnsUpdatedProfile() {
        User user = user("client@example.com", Set.of(role("CLIENT")));
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNom("Nouveau nom");
        request.setPrenom("Nouveau prénom");
        request.setTelephone("+212612345678");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse result = userService.updateProfile(1L, request);

        assertThat(result.getNom()).isEqualTo("Nouveau nom");
        assertThat(result.getPrenom()).isEqualTo("Nouveau prénom");
        assertThat(result.getTelephone()).isEqualTo("+212612345678");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_throwsWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(1L, new UpdateProfileRequest()))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignRole_rejectsMissingOrEmptyRoleLists() {
        assertThatThrownBy(() -> userService.assignRole(1L, (AssignRolesRequest) null))
                .isInstanceOf(InvalidRoleException.class);
        assertThatThrownBy(() -> userService.assignRole(1L, new AssignRolesRequest(null)))
                .isInstanceOf(InvalidRoleException.class);
        assertThatThrownBy(() -> userService.assignRole(1L, Set.of()))
                .isInstanceOf(InvalidRoleException.class);

        verifyNoInteractions(userRepository, roleRepository);
    }

    @Test
    void assignRole_rejectsUnknownBlankAndMissingRoles() {
        User user = user("client@example.com", Set.of(role("CLIENT")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.assignRole(1L, Set.of(" ")))
                .isInstanceOf(InvalidRoleException.class);
        assertThatThrownBy(() -> userService.assignRole(1L, Set.of("NOT_A_ROLE")))
                .isInstanceOf(InvalidRoleException.class);

        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.assignRole(1L, Set.of("CLIENT")))
                .isInstanceOf(InvalidRoleException.class);
    }

    @Test
    void assignRole_updatesRolesForAnExistingUser() {
        Role clientRole = role("CLIENT");
        User user = user("client@example.com", Set.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(clientRole));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse result = userService.assignRole(1L,
                AssignRolesRequest.builder().roles(Set.of("client")).build());

        assertThat(user.getRoles()).containsExactly(clientRole);
        assertThat(result.getRoles()).containsExactly("CLIENT");
        verify(userRepository).save(user);
    }

    @Test
    void assignRole_rejectsRemovingCurrentAdministratorsOwnAdminRole() {
        User admin = user("admin@example.com", Set.of(role("ADMIN")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(role("CLIENT")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ADMIN@example.com", "password"));

        assertThatThrownBy(() -> userService.assignRole(1L, Set.of("CLIENT")))
                .isInstanceOf(SelfRoleModificationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignRole_keepsAdminRoleWhenAdministratorUpdatesOwnRoles() {
        Role adminRole = role("ADMIN");
        User admin = user("admin@example.com", Set.of(adminRole));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(admin)).thenReturn(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ADMIN@example.com", "password"));

        userService.assignRole(1L, Set.of("ADMIN"));

        verify(userRepository).save(admin);
    }

    @Test
    void assignRole_throwsWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.assignRole(1L, Set.of("CLIENT")))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(roleRepository);
    }

    private static User user(String email, Set<Role> roles) {
        return User.builder()
                .id(1L)
                .nom("Nom")
                .prenom("Prenom")
                .email(email)
                .telephone("+212612345678")
                .password("encoded")
                .roles(new HashSet<>(roles))
                .build();
    }

    private static Role role(String name) {
        return Role.builder().name(name).build();
    }
}
