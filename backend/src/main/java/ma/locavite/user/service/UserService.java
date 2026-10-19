package ma.locavite.user.service;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.RoleType;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse assignRole(Long userId, AssignRolesRequest request) {
        if (request == null || request.getRoles() == null) {
            throw new InvalidRoleException("La liste des rôles ne peut pas être vide");
        }
        return assignRole(userId, request.getRoles());
    }
    //jajoute ces deux 
    @Transactional(readOnly = true)
     public UserResponse getProfile(Long userId) {
     User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé avec l'id : " + userId));
    return userMapper.toResponse(user);
    }

   @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé avec l'id : " + userId));
    user.setNom(request.getNom());
    user.setPrenom(request.getPrenom());
    user.setTelephone(request.getTelephone());
    return userMapper.toResponse(userRepository.save(user));
}

    @Transactional
    public UserResponse assignRole(Long userId, Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new InvalidRoleException("La liste des rôles ne peut pas être vide");
        }

        // 1. Recherche de l'utilisateur cible (404 si introuvable)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé avec l'id : " + userId));

        // 2. Validation et récupération des entités Role
        Set<Role> newRoles = new HashSet<>();
        for (String roleName : roleNames) {
            if (roleName == null || roleName.isBlank()) {
                throw new InvalidRoleException("Le nom du rôle ne peut pas être vide");
            }

            try {
                RoleType.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidRoleException("Rôle inexistant : " + roleName);
            }

            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new InvalidRoleException("Rôle inexistant : " + roleName));
            newRoles.add(role);
        }

        // 3. Vérification de l'utilisateur courant via SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            String currentUsername = authentication.getName();
            if (user.getEmail().equalsIgnoreCase(currentUsername)) {
                boolean containsAdmin = newRoles.stream()
                        .anyMatch(r -> RoleType.ADMIN.name().equalsIgnoreCase(r.getName()));
                if (!containsAdmin) {
                    log.warn("Tentative de retrait du rôle ADMIN par l'administrateur lui-même [{}]", currentUsername);
                    throw new SelfRoleModificationException("Un administrateur ne peut pas se retirer lui-même le rôle ADMIN");
                }
            }
        }

        user.setRoles(newRoles);
        User savedUser = userRepository.save(user);
        log.info("Rôles mis à jour pour l'utilisateur [id={}, email={}] : {}", savedUser.getId(), savedUser.getEmail(), roleNames);

        return userMapper.toResponse(savedUser);
    }
}
