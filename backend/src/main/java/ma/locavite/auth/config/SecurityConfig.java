package ma.locavite.auth.config;

import jakarta.servlet.http.HttpServletResponse;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.payment.cmi.filter.CmiWebhookFilter;
import ma.locavite.payment.cmi.filter.IdempotencyFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;
    private final boolean requireHttps;

    public SecurityConfig(UserRepository userRepository,
                          @Value("${security.require-https:true}") boolean requireHttps) {
        this.userRepository = userRepository;
        this.requireHttps = requireHttps;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email : " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CmiWebhookFilter cmiWebhookFilter,
            IdempotencyFilter idempotencyFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .requiresChannel(channel -> {
                if (requireHttps) {
                    channel.requestMatchers("/api/payments/**").requiresSecure();
                }
            })
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/payments/cmi/callback", "/api/payments/cmi/webhook").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reservations/quote").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reservations/*/contract/digital").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/brands", "/api/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/seasons", "/api/seasons/**").permitAll()
                .requestMatchers("/api/vehicles", "/api/vehicles/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentification requise\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Accès refusé\"}");
                })
            )
            .addFilterBefore(cmiWebhookFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(idempotencyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("*"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
