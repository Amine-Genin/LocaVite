package ma.locavite.auth.service;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.dto.*;
import ma.locavite.auth.exception.EmailAlreadyExistsException;
import ma.locavite.auth.exception.InvalidTokenException;
import ma.locavite.auth.mapper.UserMapper;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper,
                       EmailService emailService,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Tentative de connexion pour l'email : {}", request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            log.warn("Échec d'authentification pour l'email : {}", request.getEmail());
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        log.info("Connexion réussie pour l'utilisateur [id={}, email={}]", user.getId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .user(userMapper.toResponse(user))
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Tentative de rafraîchissement du token");

        String refreshToken = request.getRefreshToken();
        String email = refreshTokenService.validateAndGetEmail(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token invalide ou expiré"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("Utilisateur introuvable pour ce refresh token"));

        // Révocation de l'ancien refresh token et génération d'un nouveau
        refreshTokenService.revokeRefreshToken(refreshToken);
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getEmail());
        String newAccessToken = jwtService.generateToken(user);

        log.info("Token rafraîchi avec succès pour [{}]", email);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .user(userMapper.toResponse(user))
                .build();
    }

    public void logout(RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenService.revokeRefreshToken(request.getRefreshToken());
            log.info("Déconnexion : refresh token révoqué");
        }
    }
}
