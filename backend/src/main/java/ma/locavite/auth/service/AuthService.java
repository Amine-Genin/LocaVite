package ma.locavite.auth.service;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.dto.RegisterRequest;
import ma.locavite.auth.dto.UserResponse;
import ma.locavite.auth.exception.EmailAlreadyExistsException;
import ma.locavite.auth.mapper.UserMapper;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Tentative d'inscription pour l'email : {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("L'email {} est déjà utilisé", request.getEmail());
            throw new EmailAlreadyExistsException("L'adresse email est déjà utilisée : " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role clientRole = roleRepository.findByName("CLIENT")
                .or(() -> roleRepository.findByName("ROLE_CLIENT"))
                .orElseGet(() -> roleRepository.save(Role.builder().name("CLIENT").build()));

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(clientRole);

        User savedUser = userRepository.save(user);
        log.info("Utilisateur créé avec succès [id={}]", savedUser.getId());

        emailService.sendConfirmationEmail(savedUser);

        return userMapper.toResponse(savedUser);
    }
}
