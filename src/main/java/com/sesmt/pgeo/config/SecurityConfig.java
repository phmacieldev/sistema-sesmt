package com.sesmt.pgeo.config;

import com.sesmt.pgeo.model.Usuario;
import com.sesmt.pgeo.repository.UsuarioRepository;
import com.sesmt.pgeo.security.CspNonceFilter;
import com.sesmt.pgeo.security.LoginRateLimitFilter;
import com.sesmt.pgeo.security.NonceCspHeaderWriter;
import com.sesmt.pgeo.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.header.HeaderWriterFilter;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @org.springframework.beans.factory.annotation.Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           UsuarioRepository usuarioRepo,
                                           LoginAttemptService loginAttemptService) throws Exception {
        http.addFilterBefore(new CspNonceFilter(), HeaderWriterFilter.class);
        if (rateLimitEnabled) {
            http.addFilterBefore(new LoginRateLimitFilter(loginAttemptService),
                                 UsernamePasswordAuthenticationFilter.class);
        }
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/static/**", "/css/**", "/js/**", "/webjars/**", "/error").permitAll()
                .requestMatchers("/login", "/login/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/ws/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(loginSuccessHandler(usuarioRepo, loginAttemptService))
                .failureHandler(loginFailureHandler(loginAttemptService))
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/mover_agendamento",
                    "/atualizar_status_aso",
                    "/atualizar_aso",
                    "/ws/**"
                )
            )
            // ── Headers de segurança HTTP ─────────────────────────────────
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(ct -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
                .referrerPolicy(ref ->
                    ref.policy(org.springframework.security.web.header.writers
                        .ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .addHeaderWriter(new NonceCspHeaderWriter())
            )
            // ── Sessão ───────────────────────────────────────────────────
            .sessionManagement(session -> session
                .maximumSessions(2)
                .expiredUrl("/login?expired=true")
            );

        return http.build();
    }

    /**
     * Registra login bem-sucedido, zera contador de rate limiting e atualiza último acesso.
     */
    private AuthenticationSuccessHandler loginSuccessHandler(UsuarioRepository usuarioRepo,
                                                              LoginAttemptService loginAttemptService) {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            String ip = getClientIp(request);
            loginAttemptService.registrarSucesso(ip);
            usuarioRepo.findByUsername(username).ifPresent(u -> {
                u.setUltimoLogin(LocalDateTime.now());
                usuarioRepo.save(u);
            });
            log.info("LOGIN OK | usuario={} | ip={}", username, ip);
            response.sendRedirect("/home");
        };
    }

    /**
     * Registra tentativa falha e incrementa contador de rate limiting por IP.
     */
    private AuthenticationFailureHandler loginFailureHandler(LoginAttemptService loginAttemptService) {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            String ip = getClientIp(request);
            loginAttemptService.registrarFalha(ip);
            log.warn("LOGIN FALHOU | usuario={} | ip={} | motivo={}",
                username, ip, exception.getMessage());
            response.sendRedirect("/login?error=true");
        };
    }

    /**
     * Extrai o IP real do cliente considerando proxies/load balancers.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepo) {
        return username -> {
            Usuario u = usuarioRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "Usuário não encontrado: " + username));

            if (!u.isAtivo()) {
                throw new UsernameNotFoundException("Usuário desativado: " + username);
            }

            return User.builder()
                .username(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole().name())
                .build();
        };
    }

    /**
     * BCrypt com strength=12 (padrão é 10).
     * Força 4x maior, imperceptível ao usuário (~300ms vs ~100ms por login).
     * Mais resistente a ataques de força bruta offline.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
