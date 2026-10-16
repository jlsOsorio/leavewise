package org.altechego.leavewise.common.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public String[] publicPaths() {
        return new String[] {
                "/api/auth/registration", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                "/api/auth/companies", "/api/auth/companies/*/locations",
                "/actuator/health"};
    }

    @Bean
    public String[] privatePaths() {
        return new String[] {"/api/**"};
    }

    // Security errors happen in filters, outside MVC; delegating to the resolver gives them the
    // GlobalExceptionHandler ProblemDetail (401 UNAUTHENTICATED, 403 FORBIDDEN)
    @Bean
    public AuthenticationEntryPoint problemAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {

        return (request, response, ex) -> resolver.resolveException(request, response, null, ex);
    }

    @Bean
    public AccessDeniedHandler problemAccessDeniedHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {

        return (request, response, ex) -> resolver.resolveException(request, response, null, ex);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("publicPaths") String[] publicPaths,
            @Qualifier("privatePaths") String[] privatePaths,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
            AuthenticationEntryPoint problemAuthenticationEntryPoint,
            AccessDeniedHandler problemAccessDeniedHandler) {

        return http
                .csrf(AbstractHttpConfigurer::disable) // bearer only, no cookies reach the backend
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(publicPaths).permitAll()
                        .requestMatchers(privatePaths).authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(problemAuthenticationEntryPoint)
                        .accessDeniedHandler(problemAccessDeniedHandler))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(problemAuthenticationEntryPoint)
                        .accessDeniedHandler(problemAccessDeniedHandler))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
